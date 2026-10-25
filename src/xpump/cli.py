import argparse
import asyncio
from dataclasses import replace
from datetime import timedelta
import json
import os
from pathlib import Path
import sys
import time

import httpx

from .config import Config
from .ideas import OllamaIdeas, TemplateIdeas
from .models import Post, utcnow
from .pipeline import Pipeline
from .publish import Publisher
from .store import Blocked, Store
from .xfeed import configure_rules, stream


async def worker(pipeline, queue):
    last_prune = 0
    while True:
        if pipeline.publisher:
            try:
                await pipeline.publisher.reconcile()
            except Exception as exc:
                pipeline.store.health("reconcile_error", type(exc).__name__)
        if time.monotonic() - last_prune > 3600:
            pipeline.store.prune()
            last_prune = time.monotonic()
        pipeline.store.health("worker_heartbeat", utcnow().isoformat())
        try:
            post = await asyncio.wait_for(queue.get(), timeout=5)
        except TimeoutError:
            continue
        try:
            print(json.dumps(await pipeline.process(post)), flush=True)
        finally:
            queue.task_done()


async def execute(args):
    cfg = Config.load(args.config)
    if args.command == "demo":
        # Dedicated state and forced network-free provider, even with a live config file.
        cfg = replace(cfg, mode="preview", author_ids=("1",), idea_provider="templates", x_enabled=False,
                      cooldown_seconds=0, max_launches_24h=1000, max_candidates_24h=1000,
                      max_launch_sol="0", max_24h_sol="0", max_total_sol="0")
    directory = str(Path(cfg.data_dir) / ("demo" if args.command == "demo" else cfg.mode))
    store = Store(directory)
    if args.command == "status":
        print(json.dumps(store.snapshot(), indent=2))
        return
    if args.command == "stop":
        (store.root / "STOP").touch()
        print("Kill switch set. Already broadcast transactions can still finalize.")
        return
    async with httpx.AsyncClient(timeout=15, follow_redirects=False) as client:
        if args.command == "configure-x":
            await configure_rules(cfg, client)
            print("Dedicated X stream rule configured.")
            return
        with store.single_runner():
            if args.command == "demo":
                now = utcnow()
                post_id = str(time.time_ns())
                pipeline = Pipeline(cfg, store, TemplateIdeas(), started_at=now - timedelta(seconds=1))
                print(json.dumps(await pipeline.process(Post(post_id, post_id, "1", "Coffee on the moon!", now)), indent=2))
                return
            if cfg.x_enabled and cfg.configure_x_on_start:
                await configure_rules(cfg, client)
            ideas = (TemplateIdeas() if cfg.idea_provider == "templates" else
                     OllamaIdeas(client, cfg.ollama_model, cfg.ai_timeout_seconds))
            publisher = Publisher(cfg, store, client) if cfg.mode == "live" else None
            pipeline = Pipeline(cfg, store, ideas, publisher)
            queue = asyncio.Queue(maxsize=32)
            store.health("mode", cfg.mode)
            store.health("started_at", utcnow().isoformat())
            if not cfg.x_enabled:
                store.health("feed", {"status": "disabled", "time": utcnow().isoformat()})
                print("X feed disabled: waiting for configuration; no new posts or launches. "
                      "Set XPUMP_AUTHOR_IDS, X_BEARER_TOKEN and XPUMP_X_ENABLED=true, then restart.", flush=True)
            try:
                async with asyncio.TaskGroup() as group:
                    if cfg.x_enabled:
                        group.create_task(stream(cfg, store, queue))
                    group.create_task(worker(pipeline, queue))
            finally:
                store.health("feed", {"status": "stopped", "time": utcnow().isoformat()})


def main():
    parser = argparse.ArgumentParser(description="Automatic, rule-bound X-to-Pump.fun launcher")
    parser.add_argument("--config", default=os.getenv("XPUMP_CONFIG", "config.toml"))
    parser.add_argument("command", choices=("demo", "run", "status", "stop", "configure-x", "web"))
    args = parser.parse_args()
    try:
        if args.command == "web":
            import uvicorn
            from .web import create_app
            cfg = Config.load(args.config)
            uvicorn.run(create_app(cfg), host="0.0.0.0", port=int(os.getenv("PORT", "8000")),
                        access_log=False, limit_concurrency=32)
            return
        asyncio.run(execute(args))
    except KeyboardInterrupt:
        pass
    except Exception as exc:
        # No tracebacks with credential-bearing provider URLs.
        while isinstance(exc, BaseExceptionGroup):
            exc = exc.exceptions[0]
        reason = str(exc) if isinstance(exc, (Blocked, ValueError)) else type(exc).__name__
        print(f"Stopped: {reason}", file=sys.stderr)
        sys.exit(1)

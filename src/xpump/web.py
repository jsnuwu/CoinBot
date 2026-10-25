"""Free preview website. This module has no publisher, wallet or paid API integration."""
import asyncio
import base64
from collections import deque
from contextlib import asynccontextmanager, suppress
from dataclasses import asdict, replace
import hashlib
from importlib.resources import files
import json
from pathlib import Path
import re
import time

from starlette.applications import Starlette
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import HTMLResponse, JSONResponse, Response
from starlette.routing import Route

from .art import render
from .ideas import SENSITIVE_CONTENT, TemplateIdeas
from .models import Post, utcnow
from .pipeline import Pipeline
from .public_x import PublicMonitor
from .store import Store


def create_app(cfg):
    if cfg.mode != "preview" or cfg.x_enabled or cfg.idea_provider != "templates":
        raise ValueError("The website supports free template previews only; no paid X API or live mode")
    authors = tuple(a.strip().split(":")[1] for a in cfg.public_x_accounts.split(",") if a.strip())
    preview_cfg = replace(cfg, author_ids=authors, max_launch_sol="0", max_24h_sol="0", max_total_sol="0")
    requests = deque()

    @asynccontextmanager
    async def lifespan(app):
        store = Store(str(Path(cfg.data_dir) / "preview"))
        try:
            with store.single_runner():
                store.prune()
                pipeline = Pipeline(preview_cfg, store, TemplateIdeas())
                monitor = PublicMonitor(preview_cfg, pipeline)
                app.state.store, app.state.monitor = store, monitor
                task = asyncio.create_task(monitor.run())
                try:
                    yield
                finally:
                    task.cancel()
                    with suppress(asyncio.CancelledError):
                        await task
        finally:
            store.db.close()

    async def home(request):
        return HTMLResponse(files("xpump").joinpath("static/index.html").read_text())

    async def asset(request):
        name = request.path_params["name"]
        if name not in ("app.js", "app.css"):
            return Response(status_code=404)
        return Response(files("xpump").joinpath("static/" + name).read_text(),
                        media_type="application/javascript" if name.endswith("js") else "text/css")

    async def health(request):
        # Liveness only. Source availability is reported separately by /api/status.
        return JSONResponse({"status": "ok", "mode": "preview", "launch_enabled": False})

    async def status(request):
        monitor = request.app.state.monitor
        snapshot = request.app.state.store.snapshot()
        return JSONResponse({"mode": "preview", "launch_enabled": False, "source": "public_x_embed_experimental",
                             "ephemeral_storage": cfg.free_hosting, "monitor": monitor.state,
                             "previews": snapshot["recent_posts"], "poll_seconds": cfg.public_x_poll_seconds})

    async def preview(request: Request):
        now = time.monotonic()
        while requests and now - requests[0] >= 60:
            requests.popleft()
        if len(requests) >= 20:
            return JSONResponse({"error": "Bitte kurz warten. Maximal 20 Vorschläge pro Minute."}, status_code=429)
        requests.append(now)
        raw = bytearray()
        async for chunk in request.stream():
            raw.extend(chunk)
            if len(raw) > 16000:
                return JSONResponse({"error": "Der Text ist zu lang."}, status_code=413)
        try:
            data = json.loads(raw)
            text, url = data.get("text", ""), data.get("url", "")
            if not isinstance(text, str) or not 1 <= len(text.strip()) <= 4000 or not isinstance(url, str):
                raise ValueError()
            if url and not re.fullmatch(r"https://(?:x\.com|twitter\.com)/(?:[A-Za-z0-9_]{1,15}|i/web)/status/[0-9]{1,24}", url):
                raise ValueError()
        except (ValueError, AttributeError, TypeError):
            return JSONResponse({"error": "Bitte Post-Text und optional einen gültigen X-Post-Link eingeben."}, status_code=400)
        if SENSITIVE_CONTENT.search(text):
            return JSONResponse({"error": "Diesen Inhalt verarbeitet der Meme-Filter nicht."}, status_code=422)
        ident = str(int(hashlib.sha256((url + text).encode()).hexdigest()[:15], 16))
        post = Post(ident, ident, "0", text, utcnow())
        idea = await TemplateIdeas().generate(post)
        if idea is None:
            return JSONResponse({"error": "Kein passendes Vorlagenmotiv gefunden. Unterstützt werden z. B. "
                                          "Mond, Kaffee, Katzen, Hunde, Tacos, Gold, Roboter und Pizza."}, status_code=422)
        metadata = idea.metadata(post, "meme.png")
        if url:
            metadata["source_post"] = url
        else:
            metadata.pop("source_post")
        return JSONResponse({"status": "preview", "origin": "user_supplied_text", "idea": asdict(idea),
                             "metadata": metadata, "image": base64.b64encode(render(idea)).decode()})

    async def saved_preview(request):
        ident = request.path_params["ident"]
        if not re.fullmatch(r"[0-9]{1,24}", ident):
            return Response(status_code=404)
        path = request.app.state.store.root / "previews" / ident
        try:
            metadata = json.loads((path / "metadata.json").read_text())
            png = (path / "meme.png").read_bytes()
        except (OSError, ValueError):
            return Response(status_code=404)
        return JSONResponse({"status": "preview", "origin": "experimental_public_x_view", "metadata": metadata,
                             "image": base64.b64encode(png).decode()})

    app = Starlette(lifespan=lifespan, routes=[
        Route("/", home), Route("/assets/{name}", asset), Route("/health", health), Route("/api/status", status),
        Route("/api/preview", preview, methods=["POST"]), Route("/api/previews/{ident}", saved_preview),
    ])

    async def headers(request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["Referrer-Policy"] = "no-referrer"
        response.headers["Cache-Control"] = "no-store"
        response.headers["Content-Security-Policy"] = (
            "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: blob:; "
            "connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"
        )
        return response

    app.add_middleware(BaseHTTPMiddleware, dispatch=headers)
    return app

from dataclasses import replace

import httpx
import pytest

from xpump.ideas import TemplateIdeas
from xpump.pipeline import Pipeline
from xpump.public_x import PublicMonitor
from test_free_web import embed, tweet


@pytest.mark.parametrize("failure,code,http_status", [
    (403, "public_access_limited", 403), (429, "public_access_limited", 429),
    (503, "public_view_unavailable", 503), ("html", "public_view_unavailable", None),
    ("timeout", "request_timeout", None), ("network", "network_error", None),
])
async def test_source_failure_is_specific_without_exposing_provider_details(cfg, store, failure, code, http_status):
    monitor = PublicMonitor(replace(cfg, public_x_accounts="testaccount:123"), Pipeline(cfg, store, TemplateIdeas()))

    async def handler(request):
        if failure == "timeout":
            raise httpx.ReadTimeout("secret provider URL/token must not be logged")
        if failure == "network":
            raise httpx.ConnectError("secret provider URL/token must not be logged")
        return httpx.Response(failure, text="private provider details") if isinstance(failure, int) else httpx.Response(200, text="Please log in")
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        await monitor.check(client, now=1000)
    account = monitor.state["accounts"][0]
    assert account["error_code"] == code
    assert account.get("http_status") == http_status
    assert "secret" not in str(monitor.state) and "private provider details" not in str(monitor.state)
    assert monitor.next_due["testaccount"] == 1900
    assert monitor.state["started_at"]


async def test_recovery_clears_old_error_code(cfg, store):
    monitor = PublicMonitor(replace(cfg, public_x_accounts="testaccount:123"), Pipeline(cfg, store, TemplateIdeas()))
    failing = True

    async def handler(request):
        return httpx.Response(403) if failing else httpx.Response(200, text=embed(tweet()))
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        await monitor.check(client, now=1000)
        failing = False
        await monitor.check(client, now=1900)
    account = monitor.state["accounts"][0]
    assert account["status"] == "readable" and "error_code" not in account

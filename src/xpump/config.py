from dataclasses import dataclass, fields
from decimal import Decimal, InvalidOperation
from pathlib import Path
import math
import os
import re
import tomllib


def lamports(value: str) -> int:
    try:
        amount = Decimal(str(value)) * 1_000_000_000
        if not amount.is_finite() or amount < 0 or amount != amount.to_integral_value():
            raise ValueError("SOL amount must be nonnegative with at most nine decimals")
        return int(amount)
    except InvalidOperation as exc:
        raise ValueError("Invalid SOL amount") from exc


@dataclass(frozen=True)
class Config:
    mode: str = "preview"
    data_dir: str = "data"
    author_ids: tuple[str, ...] = ()
    max_post_age_seconds: int = 60
    min_score: float = 0.85
    cooldown_seconds: int = 900
    max_launches_24h: int = 2
    max_candidates_24h: int = 30
    max_launch_sol: str = "0"
    max_24h_sol: str = "0"
    max_total_sol: str = "0"
    wallet_reserve_sol: str = "0.01"
    priority_fee_sol: str = "0.00001"
    idea_provider: str = "templates"
    ollama_model: str = "qwen3:4b"
    ai_timeout_seconds: int = 8
    x_enabled: bool = False
    configure_x_on_start: bool = False
    free_hosting: bool = False
    public_x_accounts: str = ""
    public_x_poll_seconds: int = 120

    def __post_init__(self):
        if self.mode not in ("preview", "live") or self.idea_provider not in ("templates", "ollama"):
            raise ValueError("Unknown operating mode or idea provider")
        if type(self.x_enabled) is not bool or type(self.configure_x_on_start) is not bool:
            raise ValueError("X flags must be booleans")
        if type(self.free_hosting) is not bool:
            raise ValueError("free_hosting must be a boolean")
        if self.free_hosting and (self.mode != "preview" or self.x_enabled or self.idea_provider != "templates"):
            raise ValueError("Free hosting permits preview/templates only, without the paid X API")
        if type(self.public_x_poll_seconds) is not int or self.public_x_poll_seconds < 60:
            raise ValueError("Public X polling must be at least 60 seconds")
        if self.public_x_accounts and self.mode != "preview":
            raise ValueError("Experimental public X source is preview-only")
        if self.public_x_accounts:
            accounts = self.public_x_accounts.split(",")
            if len(accounts) > 5 or any(not re.fullmatch(r"[A-Za-z0-9_]{1,15}:[0-9]{1,24}", a.strip())
                                        for a in accounts):
                raise ValueError("Use up to five handle:numeric_id entries for public X accounts")
        if not math.isfinite(self.min_score) or not 0 <= self.min_score <= 1:
            raise ValueError("min_score must be between zero and one")
        for name in ("max_post_age_seconds", "max_launches_24h", "max_candidates_24h", "ai_timeout_seconds"):
            if type(getattr(self, name)) is not int or getattr(self, name) <= 0:
                raise ValueError(f"{name} must be a positive integer")
        if type(self.cooldown_seconds) is not int or self.cooldown_seconds < 0:
            raise ValueError("Invalid cooldown")
        if any(not isinstance(a, str) or not re.fullmatch(r"[0-9]{1,24}", a) for a in self.author_ids):
            raise ValueError("Use numeric X author IDs")
        for name in ("max_launch_sol", "max_24h_sol", "max_total_sol", "wallet_reserve_sol", "priority_fee_sol"):
            lamports(getattr(self, name))
        if self.mode == "live":
            caps = [lamports(self.max_launch_sol), lamports(self.max_24h_sol), lamports(self.max_total_sol)]
            if not self.author_ids or not 0 < caps[0] <= caps[1] <= caps[2]:
                raise ValueError("Live mode requires authors and positive, ordered launch/24h/total SOL caps")
            if lamports(self.priority_fee_sol) >= caps[0]:
                raise ValueError("Priority fee must be below the launch cap")

    @classmethod
    def load(cls, path: str):
        values = tomllib.loads(Path(path).read_text())
        # Hosting settings override TOML without writing secrets into a generated file.
        for field in fields(cls):
            key = "XPUMP_" + field.name.upper()
            if key not in os.environ:
                continue
            raw = os.environ[key].strip()
            try:
                if field.name == "author_ids":
                    value = tuple(part.strip() for part in raw.split(",")) if raw else ()
                elif type(field.default) is bool:
                    if raw.lower() not in ("true", "false"):
                        raise ValueError()
                    value = raw.lower() == "true"
                elif type(field.default) is int:
                    value = int(raw)
                elif type(field.default) is float:
                    value = float(raw)
                else:
                    value = raw
            except ValueError as exc:
                raise ValueError(f"Invalid setting: {key}") from exc
            values[field.name] = value
        if "author_ids" in values:
            values["author_ids"] = tuple(values["author_ids"])
        return cls(**values)

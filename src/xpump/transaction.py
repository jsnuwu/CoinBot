"""Reject unknown transaction shapes before signing third-party transactions."""
import json
from importlib.resources import files
import struct

from solders.pubkey import Pubkey
from solders.transaction import VersionedTransaction

from .store import Blocked

IDL = json.loads(files("xpump").joinpath("pump_create.json").read_text())
PUMP = Pubkey.from_string(IDL["address"])
COMPUTE = Pubkey.from_string("ComputeBudget111111111111111111111111111111")


def expected_accounts(spec, wallet: Pubkey, mint: Pubkey):
    result = {"mint": mint, "user": wallet, "program": PUMP}
    result.update({a["name"]: Pubkey.from_string(a["address"]) for a in spec["accounts"] if "address" in a})

    def seed(s):
        if s["kind"] == "const":
            return bytes(s["value"])
        if s["kind"] == "account":
            return bytes(result[s["path"]])
        raise Blocked("unsupported_pda_seed")

    for item in spec["accounts"]:
        name = item["name"]
        if name in result:
            continue
        if "address" in item:
            result[name] = Pubkey.from_string(item["address"])
        elif "pda" in item:
            pda = item["pda"]
            program = Pubkey.from_bytes(seed(pda["program"])) if "program" in pda else PUMP
            result[name] = Pubkey.find_program_address([seed(s) for s in pda["seeds"]], program)[0]
        elif name == "mayhem_token_vault":
            result[name] = Pubkey.find_program_address(
                [bytes(result["mayhem_state"]), bytes(result["token_program"]), bytes(mint)],
                result["associated_token_program"],
            )[0]
        else:
            raise Blocked("unsupported_create_account")
    return [result[a["name"]] for a in spec["accounts"]]


def expected_data(spec, wallet, name, symbol, uri):
    data = bytes(spec["discriminator"])
    for s in (name, symbol, uri):
        value = s.encode("utf-8")
        data += struct.pack("<I", len(value)) + value
    return data + bytes(wallet)


def validate_transaction(raw: bytes, wallet, mint, name, symbol, uri, priority_lamports: int):
    tx = VersionedTransaction.from_bytes(raw)
    msg = tx.message
    if getattr(msg, "address_table_lookups", []):
        raise Blocked("lookup_tables_not_supported")
    keys = list(msg.account_keys)
    if (not keys or keys[0] != wallet or msg.header.num_required_signatures != 2
            or set(keys[:2]) != {wallet, mint}):
        raise Blocked("unexpected_transaction_signers")
    creates, compute_tags = 0, set()
    units, micro_price = 200000, 0
    for ix in msg.instructions:
        program = keys[ix.program_id_index]
        data = bytes(ix.data)
        if program == COMPUTE:
            if not data or data[0] not in (2, 3) or data[0] in compute_tags or ix.accounts:
                raise Blocked("unsupported_compute_instruction")
            compute_tags.add(data[0])
            if data[0] == 2 and len(data) == 5:
                units = struct.unpack("<I", data[1:])[0]
                if not 1 <= units <= 1_400_000:
                    raise Blocked("invalid_compute_limit")
            elif data[0] == 3 and len(data) == 9:
                micro_price = struct.unpack("<Q", data[1:])[0]
            else:
                raise Blocked("invalid_compute_instruction")
        elif program == PUMP:
            creates += 1
            spec = next((s for s in IDL["instructions"] if bytes(s["discriminator"]) == data[:8]), None)
            if not spec:
                raise Blocked("only_create_is_allowed")
            prefix = expected_data(spec, wallet, name, symbol, uri)
            tail = data[len(prefix):]
            # v2: mayhem=false, optional cashback=false, fee_bps=0, holder_reward=false.
            allowed_tails = (b"",) if spec["name"] == "create" else tuple(b"\0" * n for n in (1, 2, 10, 11))
            if not data.startswith(prefix) or tail not in allowed_tails:
                raise Blocked("unexpected_create_metadata_or_flags")
            if [keys[i] for i in ix.accounts] != expected_accounts(spec, wallet, mint):
                raise Blocked("unexpected_create_accounts")
        else:
            # Includes transfers, dev buys, approvals, tips and bundled trades.
            raise Blocked("unexpected_instruction_program")
    if creates != 1 or (units * micro_price + 999999) // 1000000 > priority_lamports:
        raise Blocked("create_count_or_priority_fee")
    return tx

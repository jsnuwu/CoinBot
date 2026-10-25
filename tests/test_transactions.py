import struct

import pytest
from solders.hash import Hash
from solders.instruction import AccountMeta, Instruction
from solders.keypair import Keypair
from solders.message import MessageV0
from solders.null_signer import NullSigner
from solders.pubkey import Pubkey
from solders.system_program import TransferParams, transfer
from solders.transaction import VersionedTransaction

from xpump.store import Blocked
from xpump.transaction import COMPUTE, IDL, PUMP, expected_accounts, expected_data, validate_transaction


def create_raw(wallet, mint, name="Lunar Pop", symbol="LPOP", uri="https://ipfs.io/ipfs/" + "a" * 46,
               version="create", tail=None, extra=(), change_account=False):
    spec = next(s for s in IDL["instructions"] if s["name"] == version)
    keys = expected_accounts(spec, wallet, mint)
    if change_account:
        keys[2] = Keypair().pubkey()
    accounts = [AccountMeta(key, a.get("signer", False), a.get("writable", False))
                for key, a in zip(keys, spec["accounts"], strict=True)]
    if tail is None:
        tail = b"" if version == "create" else b"\0\0"
    data = expected_data(spec, wallet, name, symbol, uri) + tail
    instructions = [Instruction(PUMP, data, accounts), *extra]
    msg = MessageV0.try_compile(wallet, instructions, [], Hash.default())
    return bytes(VersionedTransaction(msg, [NullSigner(wallet), NullSigner(mint)]))


@pytest.fixture
def keys():
    return Keypair().pubkey(), Keypair().pubkey()


def check(raw, keys):
    return validate_transaction(raw, *keys, "Lunar Pop", "LPOP", "https://ipfs.io/ipfs/" + "a" * 46, 10000)


@pytest.mark.parametrize("version", ["create", "create_v2"])
def test_known_create_shapes_allowed(keys, version):
    check(create_raw(*keys, version=version), keys)


def test_pda_and_token_program_are_pinned(keys):
    spec = IDL["instructions"][0]
    accounts = expected_accounts(spec, *keys)
    assert str(accounts[9]) == "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    expected = Pubkey.find_program_address([b"bonding-curve", bytes(keys[1])], PUMP)[0]
    assert accounts[2] == expected


@pytest.mark.parametrize("change", [dict(name="Other Name"), dict(uri="https://attacker.invalid"),
                                    dict(change_account=True), dict(tail=b"\x01"),
                                    dict(version="create_v2", tail=b"\x01\x00")])
def test_modified_creation_rejected(keys, change):
    with pytest.raises(Blocked):
        check(create_raw(*keys, **change), keys)


def test_unexpected_transfer_rejected(keys):
    ix = transfer(TransferParams(from_pubkey=keys[0], to_pubkey=Keypair().pubkey(), lamports=1))
    with pytest.raises(Blocked, match="unexpected_instruction"):
        check(create_raw(*keys, extra=[ix]), keys)


def test_buy_instruction_rejected(keys):
    buy = Instruction(PUMP, b"buybytes", [])
    with pytest.raises(Blocked, match="only_create"):
        check(create_raw(*keys, extra=[buy]), keys)


def test_excess_priority_fee_rejected(keys):
    price = Instruction(COMPUTE, b"\x03" + struct.pack("<Q", 1_000_000), [])
    with pytest.raises(Blocked, match="priority_fee"):
        check(create_raw(*keys, extra=[price]), keys)


def test_foreign_fee_payer_rejected(keys):
    with pytest.raises(Blocked, match="signers"):
        check(create_raw(Keypair().pubkey(), keys[1]), keys)

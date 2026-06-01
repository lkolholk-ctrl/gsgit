import omvll
from functools import lru_cache

class GsGitConfig(omvll.ObfuscationConfig):
    def __init__(self):
        super().__init__()

    # ── String Encryption (XOR локальный — уникальный ключ на каждую строку) ──
    def obfuscate_string(self, _, __, string: bytes):
        return omvll.StringEncOptLocal()

    # ── Control Flow Flattening ───────────────────────────────────────────────
    def flatten_cfg(self, mod: omvll.Module, func: omvll.Function):
        return True

    # ── Break Control Flow ────────────────────────────────────────────────────
    def break_control_flow(self, mod: omvll.Module, func: omvll.Function):
        return omvll.ObfuscationConfig.default_config(self, mod, func, [], [], [], 10)

    # ── Arithmetic Substitution ───────────────────────────────────────────────
    def obfuscate_arithmetic(self, mod: omvll.Module, func: omvll.Function) -> omvll.ArithmeticOpt:
        return True

    # ── Indirect Calls ────────────────────────────────────────────────────────
    def indirect_call(self, mod: omvll.Module, func: omvll.Function):
        return omvll.ObfuscationConfig.default_config(self, mod, func, [], [], [], 10)

    # ── Function Outline ──────────────────────────────────────────────────────
    def function_outline(self, _, __):
        return omvll.FunctionOutlineWithProbability(30)

    # ── Basic Block Duplicate ─────────────────────────────────────────────────
    def basic_block_duplicate(self, _, __):
        return omvll.BasicBlockDuplicateWithProbability(30)


@lru_cache(maxsize=1)
def omvll_get_config() -> omvll.ObfuscationConfig:
    return GsGitConfig()

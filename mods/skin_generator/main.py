"""Procedural texture generator mod for PoorCraft.

This module procedurally builds up to 256 block textures using Pillow, numpy,
and a dash of nostalgic Minecraft energy. Textures are pushed to the Java side
via `add_procedural_texture()` so the renderer can build an atlas before
`ChunkRenderer.init()` finishes booting."""

from __future__ import annotations

import json
import random
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
from PIL import Image

from poorcraft import (
    add_procedural_texture,
    get_mod_config,
    log,
)


DEFAULT_CONFIG: Dict[str, object] = {
    "enabled": True,
    "max_textures": 256,
    "texture_size": 16,
    "randomize_on_startup": True,
    "variations_per_block": 3,
    "noise_scale": 4.0,
    "color_variation": 0.15,
    "biome_palettes": {
        "Desert": ["#EDC9AF", "#E4A672", "#C19A6B", "#D2B48C", "#DEB887"],
        "Snow": ["#FFFAFA", "#F0F8FF", "#E6E6FA", "#B0C4DE", "#DCDCDC"],
        "Jungle": ["#228B22", "#32CD32", "#90EE90", "#006400", "#2E8B57"],
        "Plains": ["#9ACD32", "#6B8E23", "#556B2F", "#8FBC8F", "#7CFC00"],
    },
    "block_patterns": {
        "stone": {"speckles": True, "cracks": True},
        "dirt": {"clumps": True, "organic": True},
        "grass": {"blades": True, "gradient": True},
        "wood": {"grain": True, "knots": False},
        "leaves": {"veins": True, "transparency_variation": True},
        "sand": {"ripples": True},
        "ice": {"frost": True},
    },
}

CONFIG_PATH = Path(__file__).with_name("config.json")

BASE_TEXTURES: List[Tuple[str, str]] = [
    ("dirt", "Plains"),
    ("stone", "Plains"),
    ("bedrock", "Plains"),
    ("grass_top", "Plains"),
    ("grass_side", "Plains"),
    ("sand", "Desert"),
    ("sandstone", "Desert"),
    ("cactus_top", "Desert"),
    ("cactus_side", "Desert"),
    ("snow_block", "Snow"),
    ("ice", "Snow"),
    ("snow_layer", "Snow"),
    ("jungle_grass_top", "Jungle"),
    ("jungle_grass_side", "Jungle"),
    ("jungle_dirt", "Jungle"),
    ("wood_top", "Plains"),
    ("wood_side", "Plains"),
    ("leaves", "Jungle"),
]


config: Dict[str, object] = {}
generated_textures: List[str] = []


def init() -> None:
    """Initialize the mod, load configuration, and kick off texture generation."""

    global config

    config = _load_config()
    if not config.get("enabled", True):
        log("[SkinGen] Mod disabled via config.json, skipping generation.")
        return

    log(
        f"[SkinGen] Starting texture pass with max={config.get('max_textures')} "
        f"and variations={config.get('variations_per_block')}"
    )

    if config.get("randomize_on_startup", True):
        generate_textures()
    else:
        log("[SkinGen] randomize_on_startup is false, retaining previous textures if any.")


def generate_textures() -> List[str]:
    """Generate and register procedural textures, returning their names."""

    global generated_textures
    generated_textures = []

    texture_size = int(config.get("texture_size", 16))
    if texture_size != 16:
        log("[SkinGen] Only 16x16 textures are supported right now, forcing 16.")
        texture_size = 16

    max_textures = min(256, int(config.get("max_textures", 256)))
    variations_per_block = max(1, int(config.get("variations_per_block", 3)))
    color_variation = float(config.get("color_variation", 0.15))
    noise_scale = float(config.get("noise_scale", 4.0))

    rng = random.Random()
    rng_seed = rng.randint(0, 2**31)
    log(f"[SkinGen] Using rng seed {rng_seed} for this generation run.")
    rng.seed(rng_seed)

    palettes = config.get("biome_palettes", {})

    textures_created = 0
    for base_name, biome_name in BASE_TEXTURES:
        palette = palettes.get(biome_name) or DEFAULT_CONFIG["biome_palettes"].get(biome_name, ["#FFFFFF"])
        for variant in range(variations_per_block):
            if textures_created >= max_textures:
                break

            texture_name = _variant_name(base_name, variant)
            rgba_bytes = generate_single_texture(
                texture_name=texture_name,
                base_name=base_name,
                palette=palette,
                texture_size=texture_size,
                noise_scale=noise_scale,
                color_variation=color_variation,
                rng=rng,
            )

            add_procedural_texture(texture_name, rgba_bytes)
            generated_textures.append(texture_name)
            textures_created += 1

    log(
        f"[SkinGen] Generated {len(generated_textures)} textures; "
        "I don't know what is going on here but it's working like 2011 Minecraft magic."
    )
    return generated_textures


def generate_single_texture(
    *,
    texture_name: str,
    base_name: str,
    palette: List[str],
    texture_size: int,
    noise_scale: float,
    color_variation: float,
    rng: random.Random,
) -> bytes:
    """Create a single texture variation and return raw RGBA bytes."""

    base_color = hex_to_rgb(rng.choice(palette))
    noise = simple_perlin_noise(texture_size, texture_size, noise_scale, rng.randint(0, 2**31))

    pixels = np.zeros((texture_size, texture_size, 4), dtype=np.float32)
    pixels[..., 0] = base_color[0]
    pixels[..., 1] = base_color[1]
    pixels[..., 2] = base_color[2]
    pixels[..., 3] = 255

    variation = (noise - 0.5) * 2.0 * color_variation
    pixels[..., 0:3] = np.clip(pixels[..., 0:3] * (1.0 + variation[..., None]), 0, 255)

    pixels = apply_texture_pattern(pixels, base_name, noise, rng)

    image = Image.fromarray(pixels.astype(np.uint8), mode="RGBA")
    return image.tobytes()


def simple_perlin_noise(width: int, height: int, scale: float, seed: int) -> np.ndarray:
    """Simple value-noise implementation that mimics Perlin-ish smoothness."""

    rng = np.random.default_rng(seed)
    steps = max(2, int(max(width, height) / max(scale, 1.0)))

    base = rng.random((steps, steps), dtype=np.float32)
    tile = np.kron(base, np.ones((int(np.ceil(height / steps)), int(np.ceil(width / steps))), dtype=np.float32))
    noise = tile[:height, :width]
    noise_min = float(noise.min())
    noise_max = float(noise.max())
    if noise_max - noise_min < 1e-6:
        return np.zeros_like(noise)
    return (noise - noise_min) / (noise_max - noise_min)


def apply_texture_pattern(pixels: np.ndarray, base_name: str, noise: np.ndarray, rng: random.Random) -> np.ndarray:
    """Apply block-specific flourishes (grass blades, stone cracks, leaf alpha tweaks)."""

    block_patterns = config.get("block_patterns", {})
    alpha = pixels[..., 3]

    if "stone" in base_name:
        opts = block_patterns.get("stone", {})
        if opts.get("speckles", True):
            mask = noise > 0.75
            pixels[..., 0][mask] *= 0.6
            pixels[..., 1][mask] *= 0.6
            pixels[..., 2][mask] *= 0.6
        if opts.get("cracks", True):
            crack_noise = simple_perlin_noise(*noise.shape, 2.0, rng.randint(0, 2**31))
            pixels[..., 0][crack_noise < 0.2] = 30
            pixels[..., 1][crack_noise < 0.2] = 30
            pixels[..., 2][crack_noise < 0.2] = 30

    if "dirt" in base_name:
        opts = block_patterns.get("dirt", {})
        if opts.get("clumps", True):
            clump = simple_perlin_noise(*noise.shape, 3.5, rng.randint(0, 2**31))
            pixels[..., 0:3] = np.clip(pixels[..., 0:3] * (0.8 + clump[..., None] * 0.4), 0, 255)

    if "grass" in base_name:
        opts = block_patterns.get("grass", {})
        if opts.get("gradient", True):
            gradient = np.linspace(1.15, 0.85, pixels.shape[0])[:, None]
            pixels[..., 1] = np.clip(pixels[..., 1] * gradient, 0, 255)
        if opts.get("blades", True):
            blade_mask = noise > 0.6
            pixels[..., 0][blade_mask] *= 0.8
            pixels[..., 2][blade_mask] *= 0.8

    if "wood" in base_name:
        opts = block_patterns.get("wood", {})
        if opts.get("grain", True):
            lines = (np.sin(np.linspace(0, np.pi * 4, pixels.shape[1])) + 1.0) * 0.5
            pixels[..., 0] = np.clip(pixels[..., 0] * (0.85 + lines), 0, 255)
            pixels[..., 2] = np.clip(pixels[..., 2] * (0.7 + lines * 0.3), 0, 255)

    if "leaves" in base_name:
        opts = block_patterns.get("leaves", {})
        if opts.get("veins", True):
            veins = simple_perlin_noise(*noise.shape, 3.0, rng.randint(0, 2**31))
            pixels[..., 1] = np.clip(pixels[..., 1] * (0.8 + veins), 0, 255)
        if opts.get("transparency_variation", True):
            alpha[:] = np.clip(alpha * (0.6 + noise * 0.4), 80, 255)

    if "sand" in base_name and block_patterns.get("sand", {}).get("ripples", True):
        ripples = np.sin(np.linspace(0, np.pi * 6, pixels.shape[1]))
        pixels[..., 0] = np.clip(pixels[..., 0] * (0.9 + ripples), 0, 255)
        pixels[..., 1] = np.clip(pixels[..., 1] * (0.95 + ripples * 0.2), 0, 255)

    if "ice" in base_name and block_patterns.get("ice", {}).get("frost", True):
        frost = simple_perlin_noise(*noise.shape, 5.0, rng.randint(0, 2**31))
        pixels[..., 2] = np.clip(pixels[..., 2] * (0.7 + frost), 0, 255)
        alpha[:] = np.clip(alpha * (0.6 + frost * 0.4), 150, 255)

    pixels[..., 3] = alpha
    return pixels


def hex_to_rgb(value: str) -> Tuple[int, int, int]:
    """Convert a hex color like '#C0FFEE' into an RGB tuple."""

    value = value.strip().lstrip("#")
    if len(value) == 3:
        value = "".join(ch * 2 for ch in value)
    return tuple(int(value[i : i + 2], 16) for i in range(0, 6, 2))


def _variant_name(base_name: str, variant_index: int) -> str:
    """Use bare name for the first variant, numbered suffix afterwards."""

    return base_name if variant_index == 0 else f"{base_name}_{variant_index}"


def _load_config() -> Dict[str, object]:
    """Load config defaults, merge with mod.json config and config.json overrides."""

    loaded = json.loads(json.dumps(DEFAULT_CONFIG))

    mod_json_config = get_mod_config("skin_generator") or {}
    _deep_merge(loaded, mod_json_config)

    if CONFIG_PATH.exists():
        try:
            file_config = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
            _deep_merge(loaded, file_config)
        except Exception as exc:
            log(f"[SkinGen] Failed to read config.json: {exc}")

    return loaded


def _deep_merge(target: Dict[str, object], override: Dict[str, object]) -> None:
    """Recursively merge override values into target in-place."""

    for key, value in override.items():
        if isinstance(value, dict) and isinstance(target.get(key), dict):
            _deep_merge(target[key], value)  # type: ignore[arg-type]
        else:
            target[key] = value

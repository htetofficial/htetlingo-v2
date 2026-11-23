#!/usr/bin/env python3
"""
Generate cute, aesthetic, liquid-glass style app icons, mascots, and gamification assets
for Android, macOS, and Web.
Pure Python (zero external dependencies).
"""
from __future__ import annotations
import math
import os
import struct
import zlib

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ANDROID_RES_DIR = os.path.join(BASE_DIR, "../../apps/android/app/src/main/res")
WEB_ASSETS_DIR = os.path.join(BASE_DIR, "../../apps/web/public/assets")

def write_png(filepath: str, width: int, height: int, pixel_fn):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter type 0
        for x in range(width):
            r, g, b, a = pixel_fn(x, y, width, height)
            raw.extend([
                max(0, min(255, int(r))),
                max(0, min(255, int(g))),
                max(0, min(255, int(b))),
                max(0, min(255, int(a)))
            ])

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack("!I", len(data)) + tag + data + struct.pack("!I", zlib.crc32(tag + data) & 0xffffffff)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack("!IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")

    with open(filepath, "wb") as f:
        f.write(png)

def clamp01(v: float) -> float:
    return max(0.0, min(1.0, v))

def smoothstep(edge0: float, edge1: float, x: float) -> float:
    t = clamp01((x - edge0) / (edge1 - edge0))
    return t * t * (3.0 - 2.0 * t)

# --- App Icon Renderer (Liquid Glass Blossom / Cute Star) ---
def render_app_icon(x: int, y: int, w: int, h: int, round_icon: bool = False):
    # Normalized coords -1.0 to 1.0
    nx = (x / (w - 1.0)) * 2.0 - 1.0
    ny = (y / (h - 1.0)) * 2.0 - 1.0
    dist = math.sqrt(nx * nx + ny * ny)

    # Base shape: rounded squircle or circle
    if round_icon:
        shape_alpha = smoothstep(1.0, 0.96, dist)
    else:
        # Rounded squircle: |x|^4 + |y|^4 <= 1
        sq = (nx**4 + ny**4)**0.25
        shape_alpha = smoothstep(1.0, 0.94, sq)

    if shape_alpha <= 0.001:
        return (0, 0, 0, 0)

    # Liquid Glass gradient background (Soft Sakura Pink -> Pastel Lilac -> Iridescent Sky)
    # Angle in radians
    angle = math.atan2(ny, nx)
    t_diag = (nx + ny) * 0.5 + 0.5

    # Base iridescent pastel colors
    # Pink: (244, 114, 182), Lilac: (192, 132, 252), Peach: (253, 186, 116), Sky: (186, 230, 253)
    r_bg = 244 * (1.0 - t_diag) + 192 * t_diag
    g_bg = 114 * (1.0 - t_diag) + 132 * t_diag
    b_bg = 182 * (1.0 - t_diag) + 252 * t_diag

    # Inner Glass Glow & Specular Highlight at top-left
    spec_dist = math.sqrt((nx + 0.35)**2 + (ny + 0.35)**2)
    specular = smoothstep(0.85, 0.0, spec_dist) * 0.45

    # Glass Rim / Border Glow
    rim = smoothstep(0.7, 0.96, dist) * 0.4

    # Center Mascot Icon: A cute glowing Star with cute eyes and blush!
    # Distance to star shape
    star_dist = dist / 0.58
    star_alpha = smoothstep(1.0, 0.88, star_dist)

    r = r_bg + specular * 180 + rim * 120
    g = g_bg + specular * 180 + rim * 120
    b = b_bg + specular * 200 + rim * 140

    if star_alpha > 0:
        # Cream-White glowing star center
        star_r = 255
        star_g = 250
        star_b = 230

        # Cute big round eyes! (Left eye at -0.16, -0.05, Right eye at +0.16, -0.05)
        eye_l_dist = math.sqrt((nx + 0.16)**2 + (ny + 0.05)**2) / 0.065
        eye_r_dist = math.sqrt((nx - 0.16)**2 + (ny + 0.05)**2) / 0.065

        # Eye shine highlights
        shine_l_dist = math.sqrt((nx + 0.18)**2 + (ny + 0.07)**2) / 0.025
        shine_r_dist = math.sqrt((nx - 0.14)**2 + (ny + 0.07)**2) / 0.025

        # Cheerful rosy blush!
        blush_l = smoothstep(0.12, 0.0, math.sqrt((nx + 0.26)**2 + (ny - 0.08)**2))
        blush_r = smoothstep(0.12, 0.0, math.sqrt((nx - 0.26)**2 + (ny - 0.08)**2))

        # Smile curve: (nx)^2 + (ny - 0.14)^2 ≈ circle arc
        smile_d = abs(ny - 0.14 - 1.2 * (nx * nx))
        is_smile = 1.0 if (smile_d < 0.025 and abs(nx) < 0.12 and ny > 0.12) else 0.0

        if shine_l_dist < 1.0 or shine_r_dist < 1.0:
            star_r, star_g, star_b = (255, 255, 255)
        elif eye_l_dist < 1.0 or eye_r_dist < 1.0:
            # Deep purple cute pupil
            star_r, star_g, star_b = (60, 20, 80)
        elif is_smile > 0:
            star_r, star_g, star_b = (180, 50, 90)
        else:
            # Add blush
            star_r += (blush_l + blush_r) * 60
            star_g -= (blush_l + blush_r) * 70
            star_b -= (blush_l + blush_r) * 30

        r = r * (1.0 - star_alpha) + star_r * star_alpha
        g = g * (1.0 - star_alpha) + star_g * star_alpha
        b = b * (1.0 - star_alpha) + star_b * star_alpha

    return (r, g, b, shape_alpha * 255)

# --- Cute Liquid Glass Heart Renderer ---
def render_glass_heart(x: int, y: int, w: int, h: int):
    nx = (x / (w - 1.0)) * 2.0 - 1.0
    ny = -( (y / (h - 1.0)) * 2.0 - 1.0 ) + 0.2  # flip Y

    # Heart curve: (x^2 + y^2 - 1)^3 - x^2 * y^3 <= 0
    # Scaled to fit nicely
    hx = nx * 1.3
    hy = ny * 1.3
    val = (hx * hx + hy * hy - 0.8)**3 - (hx * hx) * (hy * hy * hy)
    shape_alpha = smoothstep(0.08, -0.02, val)

    if shape_alpha <= 0.001:
        return (0, 0, 0, 0)

    # 3D Pink Glass Shading
    t = (nx + ny) * 0.5 + 0.5
    r = 251 * (1.0 - t) + 244 * t
    g = 113 * (1.0 - t) + 63 * t
    b = 133 * (1.0 - t) + 94 * t

    # Glass highlight
    highlight = smoothstep(0.5, 0.0, math.sqrt((nx + 0.25)**2 + (ny - 0.35)**2)) * 0.6
    r += highlight * 180
    g += highlight * 180
    b += highlight * 180

    return (r, g, b, shape_alpha * 255)

# --- Glowing Crystal Gem Renderer ---
def render_crystal_gem(x: int, y: int, w: int, h: int):
    nx = (x / (w - 1.0)) * 2.0 - 1.0
    ny = (y / (h - 1.0)) * 2.0 - 1.0

    # Diamond / Octagon shape
    d = abs(nx) + abs(ny * 1.1)
    shape_alpha = smoothstep(0.95, 0.88, d)

    if shape_alpha <= 0.001:
        return (0, 0, 0, 0)

    # Cyan/Amethyst crystal facets
    facet = math.sin(nx * 8.0) * math.cos(ny * 8.0)
    r = 56 + facet * 40
    g = 189 + facet * 30
    b = 248 + facet * 20

    # Specular glint
    glint = smoothstep(0.35, 0.0, math.sqrt((nx + 0.2)**2 + (ny + 0.2)**2)) * 0.5
    r += glint * 200
    g += glint * 200
    b += glint * 200

    return (r, g, b, shape_alpha * 255)

# --- Warm Streak Flame Renderer ---
def render_streak_flame(x: int, y: int, w: int, h: int):
    nx = (x / (w - 1.0)) * 2.0 - 1.0
    ny = -( (y / (h - 1.0)) * 2.0 - 1.0 ) + 0.1

    # Teardrop / Flame equation
    fx = nx / (1.0 - ny * 0.45) if ny < 1.0 else nx
    d = math.sqrt(fx * fx + ny * ny)
    shape_alpha = smoothstep(0.9, 0.82, d)

    if shape_alpha <= 0.001:
        return (0, 0, 0, 0)

    # Fiery Gold to Warm Orange
    t = ny * 0.5 + 0.5
    r = 245 * t + 251 * (1.0 - t)
    g = 158 * t + 191 * (1.0 - t)
    b = 11 * t + 36 * (1.0 - t)

    # Core inner glow
    core = smoothstep(0.4, 0.0, d) * 0.5
    r += core * 100
    g += core * 80

    return (r, g, b, shape_alpha * 255)

def main():
    print("=== Generating Aesthetic Liquid Glass Icons & Visual Assets ===")

    # 1. Android Mipmap Launcher Icons
    densities = [
        ("mipmap-mdpi", 48),
        ("mipmap-hdpi", 72),
        ("mipmap-xhdpi", 96),
        ("mipmap-xxhdpi", 144),
        ("mipmap-xxxhdpi", 192),
    ]

    for folder, size in densities:
        # Standard launcher icon
        path = os.path.join(ANDROID_RES_DIR, folder, "ic_launcher.png")
        print(f"Generating {path} ({size}x{size})...")
        write_png(path, size, size, lambda x, y, w, h: render_app_icon(x, y, w, h, round_icon=False))

        # Round launcher icon
        path_round = os.path.join(ANDROID_RES_DIR, folder, "ic_launcher_round.png")
        write_png(path_round, size, size, lambda x, y, w, h: render_app_icon(x, y, w, h, round_icon=True))

    # 2. Android Drawable Cute Visual Assets
    drawable_dir = os.path.join(ANDROID_RES_DIR, "drawable")
    write_png(os.path.join(drawable_dir, "ic_mascot_bloom.png"), 160, 160, lambda x, y, w, h: render_app_icon(x, y, w, h, round_icon=True))
    write_png(os.path.join(drawable_dir, "ic_glass_heart.png"), 128, 128, lambda x, y, w, h: render_glass_heart(x, y, w, h))
    write_png(os.path.join(drawable_dir, "ic_gem_crystal.png"), 128, 128, lambda x, y, w, h: render_crystal_gem(x, y, w, h))
    write_png(os.path.join(drawable_dir, "ic_streak_flame.png"), 128, 128, lambda x, y, w, h: render_streak_flame(x, y, w, h))

    # 3. Web & Mac High-Res Assets
    os.makedirs(WEB_ASSETS_DIR, exist_ok=True)
    write_png(os.path.join(WEB_ASSETS_DIR, "mascot-bloom.png"), 256, 256, lambda x, y, w, h: render_app_icon(x, y, w, h, round_icon=True))
    write_png(os.path.join(WEB_ASSETS_DIR, "glass-heart.png"), 160, 160, lambda x, y, w, h: render_glass_heart(x, y, w, h))
    write_png(os.path.join(WEB_ASSETS_DIR, "gem-crystal.png"), 160, 160, lambda x, y, w, h: render_crystal_gem(x, y, w, h))
    write_png(os.path.join(WEB_ASSETS_DIR, "streak-flame.png"), 160, 160, lambda x, y, w, h: render_streak_flame(x, y, w, h))

    # 512x512 High-Res App Icon for Play Store & Mac Icon
    dist_dir = os.path.join(BASE_DIR, "../../dist-apps")
    os.makedirs(dist_dir, exist_ok=True)
    write_png(os.path.join(dist_dir, "HtetLingo-512.png"), 512, 512, lambda x, y, w, h: render_app_icon(x, y, w, h, round_icon=False))

    print("=== Successfully Generated All Assets ===")

if __name__ == "__main__":
    main()

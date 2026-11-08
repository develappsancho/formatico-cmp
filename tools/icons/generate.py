"""
Formatico's app icons, drawn from the "Icons · app icons" board of the design canvas: the braces of identity A in
Geist Mono on the brand's teal. Every file is drawn from the same geometry, the font in the app's own resources:

    python3 tools/icons/generate.py

It writes the icons into each app's folders and needs fontTools and Pillow; macOS's iconutil makes the .icns.
"""

import json
import os
import shutil
import subprocess
import tempfile

from fontTools.pens.basePen import BasePen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FONT = os.path.join(ROOT, "core/designsystem/src/commonMain/composeResources/font/geist_mono.ttf")

TEAL = "#0F766E"
WHITE = "#FFFFFF"
DARK_TILE = "#042F2E"
DARK_BRACES = "#2DD4BF"
TINTED_TILE = "#1C1C1E"
TINTED_BRACES = "#E5E5EA"

# The board's mark: the braces 42% of the tile, their letters 7% closer. Below 48 px the space goes, in SemiBold.
EM = 0.42
SPACING = -0.07
SMALL_TEXT, SMALL_WEIGHT, SMALL_SPACING = "{}", 600, -0.06
SUPERSAMPLE = 4


def font_at(weight):
    font = TTFont(FONT)
    return instantiateVariableFont(font, {"wght": weight})


FONTS = {500: font_at(500), 600: font_at(600)}


class ContourPen(BasePen):
    """Collects a glyph's outline as closed polygons, its curves cut into short straight pieces."""

    def __init__(self, glyph_set, steps=24):
        super().__init__(glyph_set)
        self.contours = []
        self.current = []
        self.steps = steps

    def _moveTo(self, point):
        self.current = [point]

    def _lineTo(self, point):
        self.current.append(point)

    def _qCurveToOne(self, control, point):
        start = self.current[-1]
        for step in range(1, self.steps + 1):
            t = step / self.steps
            x = (1 - t) ** 2 * start[0] + 2 * (1 - t) * t * control[0] + t ** 2 * point[0]
            y = (1 - t) ** 2 * start[1] + 2 * (1 - t) * t * control[1] + t ** 2 * point[1]
            self.current.append((x, y))

    def _curveToOne(self, first, second, point):
        start = self.current[-1]
        for step in range(1, self.steps + 1):
            t = step / self.steps
            x = ((1 - t) ** 3 * start[0] + 3 * (1 - t) ** 2 * t * first[0] + 3 * (1 - t) * t ** 2 * second[0]
                 + t ** 3 * point[0])
            y = ((1 - t) ** 3 * start[1] + 3 * (1 - t) ** 2 * t * first[1] + 3 * (1 - t) * t ** 2 * second[1]
                 + t ** 3 * point[1])
            self.current.append((x, y))

    def _closePath(self):
        if self.current:
            self.contours.append(self.current)
        self.current = []

    _endPath = _closePath


def layout(text, weight, spacing):
    """The text's glyphs and where each stands, in font units, and the box their ink fills."""
    font = FONTS[weight]
    cmap = font.getBestCmap()
    glyph_set = font.getGlyphSet()
    upem = font["head"].unitsPerEm
    placed, x = [], 0.0
    xs, ys = [], []
    for char in text:
        name = cmap[ord(char)]
        advance = font["hmtx"][name][0]
        pen = ContourPen(glyph_set)
        glyph_set[name].draw(TransformPen(pen, (1, 0, 0, 1, x, 0)))
        for contour in pen.contours:
            xs += [p[0] for p in contour]
            ys += [p[1] for p in contour]
        placed.append((name, x))
        x += advance + spacing * upem
    return glyph_set, placed, (min(xs), min(ys), max(xs), max(ys)), upem


def braces(size, em=EM, small=None):
    """The braces of a [size] tile: their glyphs, the scale from font units, and the shift that centres their ink."""
    small = size < 48 if small is None else small
    text, weight, spacing = (SMALL_TEXT, SMALL_WEIGHT, SMALL_SPACING) if small else ("{ }", 500, SPACING)
    glyph_set, placed, (x0, y0, x1, y1), upem = layout(text, weight, spacing)
    scale = em * size / upem
    # Font units grow upwards; a picture's rows go down.
    dx = size / 2 - (x0 + x1) / 2 * scale
    dy = size / 2 + (y0 + y1) / 2 * scale
    return glyph_set, placed, scale, dx, dy


def braces_polygons(size, em=EM, small=None):
    glyph_set, placed, scale, dx, dy = braces(size, em, small)
    polygons = []
    for name, x in placed:
        pen = ContourPen(glyph_set)
        glyph_set[name].draw(TransformPen(pen, (scale, 0, 0, -scale, dx + x * scale, dy)))
        polygons += pen.contours
    return polygons


def braces_path(size, em=EM, small=None):
    """The braces as an SVG path's data, which Android's vector drawables take as they are."""
    glyph_set, placed, scale, dx, dy = braces(size, em, small)
    commands = []
    for name, x in placed:
        pen = SVGPathPen(glyph_set, ntos=lambda n: f"{n:.2f}".rstrip("0").rstrip("."))
        glyph_set[name].draw(TransformPen(pen, (scale, 0, 0, -scale, dx + x * scale, dy)))
        commands.append(pen.getCommands())
    return "".join(commands)


def draw(size, tile=TEAL, ink=WHITE, radius=0.0, em=EM, small=None, inset=0.0, shadow=False, background=None):
    """
    One icon of [size] px: the tile, its corners [radius] of its side, set in by [inset] of the picture, and the braces
    on it. Drawn four times as large and made smaller, so that its edges are smooth.
    """
    big = size * SUPERSAMPLE
    image = Image.new("RGBA", (big, big), background or (0, 0, 0, 0))
    body = big * (1 - 2 * inset)
    left = (big - body) / 2
    box = (left, left, left + body, left + body)
    if shadow:
        cast = Image.new("RGBA", (big, big), (0, 0, 0, 0))
        offset = body * 0.015
        ImageDraw.Draw(cast).rounded_rectangle(
            (box[0], box[1] + offset * 2, box[2], box[3] + offset * 2), radius=body * radius, fill=(15, 23, 42, 90)
        )
        image = Image.alpha_composite(image, cast.filter(ImageFilter.GaussianBlur(body * 0.025)))
    if tile:
        layer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
        ImageDraw.Draw(layer).rounded_rectangle(box, radius=body * radius, fill=tile)
        image = Image.alpha_composite(image, layer)
    ink_layer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    pen = ImageDraw.Draw(ink_layer)
    for polygon in braces_polygons(round(body), em, small if small is not None else size * (1 - 2 * inset) < 48):
        pen.polygon([(left + x, left + y) for x, y in polygon], fill=ink)
    image = Image.alpha_composite(image, ink_layer)
    return image.resize((size, size), Image.LANCZOS)


def save(image, *parts):
    path = os.path.join(ROOT, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path, optimize=True)
    return path


def write(text, *parts):
    path = os.path.join(ROOT, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as file:
        file.write(text)
    return path


def android():
    res = "app/androidApp/src/main/res"
    # The adaptive icon's layers are 108 dp; a launcher shows the middle 72, and keeps the middle 66 whatever its mask.
    path = braces_path(72, small=False)
    shifted = f'<group android:translateX="18" android:translateY="18">\n        <path\n            android:fillColor="{{}}"\n            android:pathData="{path}" />\n    </group>'
    vector = """<?xml version="1.0" encoding="utf-8"?>
<!-- Drawn by tools/icons/generate.py from the canvas's icon board. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    {}
</vector>
"""
    write(vector.format(shifted.format(WHITE)), res, "drawable/ic_launcher_foreground.xml")
    write(vector.format(shifted.format(WHITE)), res, "drawable/ic_launcher_monochrome.xml")
    write(vector.format(f'<path\n        android:fillColor="{TEAL}"\n        android:pathData="M0,0h108v108h-108z" />'),
          res, "drawable/ic_launcher_background.xml")
    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""
    write(adaptive, res, "mipmap-anydpi-v26/ic_launcher.xml")
    write(adaptive, res, "mipmap-anydpi-v26/ic_launcher_round.xml")
    # The wizard's pictures: with Android 11 (minSdk 30) and later the adaptive icon is always the one shown.
    for folder in ("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi", "drawable-v24"):
        shutil.rmtree(os.path.join(ROOT, res, folder), ignore_errors=True)


def ios():
    folder = "app/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
    for old in os.listdir(os.path.join(ROOT, folder)):
        if old.endswith(".png"):
            os.remove(os.path.join(ROOT, folder, old))
    # iOS rounds the corners itself: each picture fills its square.
    save(draw(1024), folder, "app-icon-1024.png")
    save(draw(1024, tile=DARK_TILE, ink=DARK_BRACES), folder, "app-icon-1024-dark.png")
    save(draw(1024, tile=TINTED_TILE, ink=TINTED_BRACES), folder, "app-icon-1024-tinted.png")
    contents = {
        "images": [
            {"filename": "app-icon-1024.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"},
            {"appearances": [{"appearance": "luminosity", "value": "dark"}], "filename": "app-icon-1024-dark.png",
             "idiom": "universal", "platform": "ios", "size": "1024x1024"},
            {"appearances": [{"appearance": "luminosity", "value": "tinted"}], "filename": "app-icon-1024-tinted.png",
             "idiom": "universal", "platform": "ios", "size": "1024x1024"},
        ],
        "info": {"author": "xcode", "version": 1},
    }
    write(json.dumps(contents, indent=2) + "\n", folder, "Contents.json")


def mac(size):
    # Apple's grid: a body of 824 in 1024, its corners 185 of it, and its shadow.
    return draw(size, radius=185 / 824, inset=100 / 1024, shadow=size >= 64)


def desktop():
    folder = "app/desktopApp/icons"
    with tempfile.TemporaryDirectory() as work:
        iconset = os.path.join(work, "Formatico.iconset")
        os.makedirs(iconset)
        for size in (16, 32, 128, 256, 512):
            mac(size).save(os.path.join(iconset, f"icon_{size}x{size}.png"))
            mac(size * 2).save(os.path.join(iconset, f"icon_{size}x{size}@2x.png"))
        os.makedirs(os.path.join(ROOT, folder), exist_ok=True)
        subprocess.run(["iconutil", "-c", "icns", iconset, "-o", os.path.join(ROOT, folder, "Formatico.icns")],
                       check=True)
    # Windows and Linux: a tile with rounded corners, each size drawn for itself.
    tiles = {size: draw(size, radius=0.23) for size in (16, 32, 48, 256)}
    tiles[256].save(os.path.join(ROOT, folder, "Formatico.ico"), sizes=[(s, s) for s in tiles],
                    append_images=[tiles[s] for s in (16, 32, 48)])
    save(draw(512, radius=0.23), folder, "Formatico.png")
    save(draw(256, radius=0.23), "app/desktopApp/src/main/resources/formatico-icon.png")


def web():
    folder = "app/webApp/src/webMain/resources"
    small = braces_path(32, small=True)
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="8" fill="{TEAL}"/>
  <path fill="{WHITE}" d="{small}"/>
</svg>
"""
    write(svg, folder, "favicon.svg")
    icons = {size: draw(size, radius=0.25) for size in (16, 32, 48)}
    icons[48].save(os.path.join(ROOT, folder, "favicon.ico"), sizes=[(s, s) for s in icons],
                   append_images=[icons[16], icons[32]])
    save(draw(180), folder, "apple-touch-icon.png")
    save(draw(192, radius=0.225), folder, "icon-192.png")
    save(draw(512, radius=0.225), folder, "icon-512.png")
    # A maskable icon fills its square, and keeps its braces inside the middle 80% any mask leaves.
    save(draw(512, em=0.35), folder, "icon-maskable-512.png")
    manifest = {
        "name": "Formatico",
        "short_name": "Formatico",
        "description": "Format, explore and convert any structured text.",
        "start_url": ".",
        "display": "standalone",
        "background_color": "#F8FAFC",
        "theme_color": TEAL,
        "icons": [
            {"src": "icon-192.png", "sizes": "192x192", "type": "image/png", "purpose": "any"},
            {"src": "icon-512.png", "sizes": "512x512", "type": "image/png", "purpose": "any"},
            {"src": "icon-maskable-512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable"},
        ],
    }
    write(json.dumps(manifest, indent=2) + "\n", folder, "manifest.webmanifest")


if __name__ == "__main__":
    android()
    ios()
    desktop()
    web()

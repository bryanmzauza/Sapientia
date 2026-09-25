#!/usr/bin/env python3
"""Generates the bundled Sapientia resource-pack assets.

Every texture is drawn procedurally from the palettes, masks and glyphs in this
package, so the art stays consistent and can be regenerated after tweaks:

    python scripts/textures/generate_textures.py            # write assets
    python scripts/textures/generate_textures.py --preview out.png

Output (committed to the repository and shipped inside the plugin jar):

    sapientia-core/src/main/resources/pack/java/     Java Edition assets
    sapientia-core/src/main/resources/pack/bedrock/  Bedrock Edition assets

The list of ids comes from the English catalogue (en.yml): every item and
block that has a display name must have a spec here, otherwise the script
fails. Requires Pillow.
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

import yaml
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))

import art  # noqa: E402
import specs  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
LANG = ROOT / "sapientia-core/src/main/resources/lang/en.yml"
OUT = ROOT / "sapientia-core/src/main/resources/pack"
NAMESPACE = "sapientia"
BEDROCK_ICON_SIZE = 32


def catalogue_ids() -> list[str]:
    """Returns every item/block id that has a display name in en.yml."""
    data = yaml.safe_load(LANG.read_text(encoding="utf-8"))
    ids: list[str] = []
    for section in ("item", "block", "metal", "component"):
        ids.extend(data.get(section, {}).keys())
    android = data.get("android", {})
    ids.extend(android.get("block", {}).keys())
    ids.extend(android.get("upgrade", {}).keys())
    return ids


def write_png(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, optimize=True)


def write_json(obj: dict, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def build(preview: Path | None) -> None:
    ids = catalogue_ids()
    missing = [i for i in ids if specs.lookup(i) is None]
    if missing:
        sys.exit("No texture spec for: " + ", ".join(missing))

    java = OUT / "java"
    bedrock = OUT / "bedrock"
    for d in (java, bedrock):
        if d.exists():
            shutil.rmtree(d)

    assets = java / "assets" / NAMESPACE
    item_texture_data: dict[str, dict] = {}
    icons: list[tuple[str, Image.Image]] = []

    for item_id in ids:
        spec = specs.lookup(item_id)
        rendered = spec.render()

        if rendered.kind == "item":
            write_png(rendered.sprite, assets / "textures/item" / f"{item_id}.png")
            write_json({"parent": rendered.parent, "textures": {"layer0": f"{NAMESPACE}:item/{item_id}"}},
                       assets / "models/item" / f"{item_id}.json")
            model_ref = f"{NAMESPACE}:item/{item_id}"
            icon = rendered.sprite.resize((BEDROCK_ICON_SIZE, BEDROCK_ICON_SIZE), Image.NEAREST)
        else:
            textures = {}
            for face, img in rendered.faces.items():
                name = item_id if face == "all" else f"{item_id}_{face}"
                write_png(img, assets / "textures/block" / f"{name}.png")
                textures[face] = f"{NAMESPACE}:block/{name}"
            parent = "minecraft:block/cube_all" if "all" in rendered.faces else "minecraft:block/orientable"
            write_json({"parent": parent, "textures": textures}, assets / "models/block" / f"{item_id}.json")
            model_ref = f"{NAMESPACE}:block/{item_id}"
            icon = art.isometric_icon(rendered.faces, BEDROCK_ICON_SIZE)

        write_json({"model": {"type": "minecraft:model", "model": model_ref}},
                   assets / "items" / f"{item_id}.json")

        write_png(icon, bedrock / "textures/items" / f"{item_id}.png")
        item_texture_data[f"{NAMESPACE}.{item_id}"] = {"textures": f"textures/items/{item_id}"}
        icons.append((item_id, icon))

    write_json({"resource_pack_name": "sapientia", "texture_name": "atlas.items",
                "texture_data": item_texture_data}, bedrock / "textures/item_texture.json")

    logo = art.pack_logo()
    write_png(logo.resize((128, 128), Image.NEAREST), java / "pack.png")
    write_png(logo.resize((256, 256), Image.NEAREST), bedrock / "pack_icon.png")

    print(f"Wrote {len(ids)} items to {OUT.relative_to(ROOT)}")
    if preview:
        art.contact_sheet(icons, preview)
        print(f"Preview sheet: {preview}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--preview", type=Path, help="also write a labelled contact sheet PNG")
    args = parser.parse_args()
    build(args.preview)


if __name__ == "__main__":
    main()

"""Shared helpers for the Heaven & Hell resource generators."""
import copy
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
NS = "heavenhell"
VANILLA_DATA = "/home/claude/misode/mcmeta/data/minecraft"
VANILLA_ASSETS = "/home/claude/misode/mcmeta-assets/assets/minecraft"

WRITTEN = []


def hh(path):
    return f"{NS}:{path}"


def mc(path):
    return path if ":" in path else f"minecraft:{path}"


def data_path(ns, kind, name, ext="json"):
    return os.path.join(RES, "data", ns, kind, f"{name}.{ext}")


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    WRITTEN.append(path)


def vanilla(rel):
    with open(os.path.join(VANILLA_DATA, rel), encoding="utf-8") as f:
        return json.load(f)


def deep(obj):
    return copy.deepcopy(obj)


def replace_strings(obj, mapping):
    """Recursively replaces exact string values (and list items) using mapping."""
    if isinstance(obj, dict):
        return {k: replace_strings(v, mapping) for k, v in obj.items()}
    if isinstance(obj, list):
        out = []
        for v in obj:
            r = replace_strings(v, mapping)
            if isinstance(r, str) and isinstance(v, str) and r in out and v != r:
                continue  # avoid duplicates created by the mapping
            out.append(r)
        return out
    if isinstance(obj, str):
        return mapping.get(obj, obj)
    return obj

"""Shared helpers for The Poolrooms resource generators."""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
NS = "poolrooms"
VANILLA_DATA = "/home/claude/misode/mcmeta/data/minecraft"
VANILLA_ASSETS = "/home/claude/misode/mcmeta-assets/assets/minecraft"

WRITTEN = []


def pr(path):
    return f"{NS}:{path}"


def data_path(ns, kind, name, ext="json"):
    return os.path.join(RES, "data", ns, kind, f"{name}.{ext}")


def asset(kind, name, ext="json"):
    return os.path.join(RES, "assets", NS, kind, f"{name}.{ext}")


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    WRITTEN.append(path)

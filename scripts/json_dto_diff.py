#!/usr/bin/env python3
"""Diff a Hypixel API JSON fixture against the gson DTO tree.

Walks a JSON document and the Java DTO class graph in parallel, reporting JSON
keys that no DTO field maps to. Understands the gson-extras annotations used in
this module: @SerializedName, @SerializedPath, @Extract, @Capture, @Collapse,
@Key and @Split.

Usage:
    python scripts/json_dto_diff.py                       # audit every member
    python scripts/json_dto_diff.py --section dungeons    # one member subtree
    python scripts/json_dto_diff.py --root SkyBlockIsland --node profile
    python scripts/json_dto_diff.py --show-mapped         # also list covered keys

Exit status is 1 when unmapped keys are found, so it works as a CI gate.
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import re
import sys
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
MODULE = os.path.dirname(HERE)
DEFAULT_SRC = os.path.join(MODULE, "src/main/java/api/simplified/hypixel")
DEFAULT_JSON = os.path.join(MODULE, "src/test/resources/craftedfury.json")

# Types we never descend into: JDK, NBT, and skyblock model classes.
OPAQUE = {
    "String", "UUID", "int", "long", "double", "float", "boolean", "short", "byte",
    "Integer", "Long", "Double", "Float", "Boolean", "Short", "Byte", "Number",
    "Object", "JsonElement", "JsonObject", "JsonArray", "CompoundTag", "ListTag",
    "SkyBlockDate", "SkyBlockDate.RealTime", "SkyBlockDate.SkyBlockTime",
}
MAP_TYPES = {"Map", "ConcurrentMap", "ConcurrentLinkedMap", "HashMap", "LinkedHashMap"}
SEQ_TYPES = {"List", "Set", "ConcurrentList", "ConcurrentSet", "ConcurrentLinkedList",
             "ArrayList", "HashSet", "Collection"}
WRAPPER_TYPES = {"Optional", "AtomicReference"}

FIELD_RE = re.compile(
    r"^(?P<mods>(?:(?:private|protected|public|static|final|transient)\s+)+)"
    r"(?P<rest>[^=;()]+?)\s+(?P<name>\w+)\s*(?:=[^=]|;)"
)
CLASS_RE = re.compile(
    r"\b(?:class|enum|interface|record)\s+(?P<name>\w+)"
    r"(?:\s*<[^>]*>)?(?:\s+extends\s+(?P<super>[\w.]+))?"
)


class Field:
    __slots__ = ("key", "name", "type", "capture", "descend", "collapse", "is_key",
                 "split", "owner")

    def __init__(self, key, name, type_, capture, descend, collapse, is_key, split):
        self.owner = None
        self.key = key
        self.name = name
        self.type = type_
        self.capture = capture
        self.descend = descend
        self.collapse = collapse
        self.is_key = is_key
        self.split = split


class JClass:
    __slots__ = ("name", "simple", "file", "fields", "is_enum", "super_name")

    def __init__(self, name, simple, file, is_enum, super_name=None):
        self.name = name
        self.simple = simple
        self.file = file
        self.fields = []
        self.is_enum = is_enum
        self.super_name = super_name


def has_ann(ann, name):
    """True when annotation @name is present, tolerating whitespace before '(' ."""
    return re.search(r"@\s*%s\b" % name, ann) is not None


def ann_value(ann, *names, param=None):
    """Return a string annotation argument, or None.

    Java allows whitespace anywhere around the parentheses, the parameter name,
    the '=' and the string literal, so every position here is \\s*-tolerant:
    @SerializedName("x"), @SerializedName( "x" ) and @Capture( filter = "x" )
    all parse identically.
    """
    alt = "|".join(names)
    if param:
        pattern = r'@\s*(?:%s)\s*\(\s*(?:[^)]*?,\s*)?%s\s*=\s*"([^"]*)"' % (alt, param)
    else:
        pattern = r'@\s*(?:%s)\s*\(\s*"([^"]*)"\s*\)' % alt
    m = re.search(pattern, ann)
    return m.group(1) if m else None


def ann_flag(ann, name, param):
    """Return True when a boolean annotation parameter is explicitly set to true."""
    return re.search(r'@\s*%s\s*\([^)]*\b%s\s*=\s*true\b' % (name, param), ann) is not None


def split_generics(text):
    """Split the top-level comma-separated arguments of a generic type."""
    out, depth, cur = [], 0, ""
    for ch in text:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def parse_type(text):
    """Return (raw_name, [generic_args]) for a Java type string."""
    text = re.sub(r"@\w+(?:\([^)]*\))?", "", text).strip()
    text = text.replace("final ", "").strip()
    m = re.match(r"^([\w.]+)\s*(?:<(.+)>)?\s*$", text)
    if not m:
        return text, []
    raw = m.group(1)
    args = split_generics(m.group(2)) if m.group(2) else []
    return raw, args


def parse_sources(src_root):
    """Parse every .java file under src_root into JClass objects keyed by name."""
    classes = {}
    for path in sorted(glob.glob(os.path.join(src_root, "**/*.java"), recursive=True)):
        text = open(path, encoding="utf-8").read()
        # strip block comments so commented-out fields are not parsed
        text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
        stack, depth, pend = [], 0, []
        for raw_line in text.split("\n"):
            line = raw_line.strip()
            if line.startswith("//"):
                continue
            cm = CLASS_RE.search(line)
            if cm and not line.startswith("*"):
                simple = cm.group("name")
                qualified = ".".join([e[0].simple for e in stack] + [simple])
                cls = JClass(qualified, simple, path, " enum " in " " + line,
                             cm.group("super"))
                classes.setdefault(qualified, cls)
                classes.setdefault(simple, cls)
                # remember the depth the class was opened at so method bodies
                # closing inside it do not pop it off the stack
                stack.append([cls, depth, False])
                depth += line.count("{") - line.count("}")
                pend = []
                continue

            if line.startswith("@"):
                pend.append(line)
            else:
                fm = FIELD_RE.match(line)
                if fm and stack and "static" not in fm.group("mods"):
                    ann = " ".join(pend)
                    raw, args = parse_type(fm.group("rest"))
                    sn = ann_value(ann, "SerializedName", "SerializedPath")
                    # @Extract takes a second element now, so the lone-literal form no longer
                    # matches every site. Without the named-parameter attempt first, an
                    # @Extract(value = ..., filter = ...) field falls back to its Java name and
                    # reports as a phantom binding while its real key reports as unmapped.
                    ex = ann_value(ann, "Extract", param="value") or ann_value(ann, "Extract")
                    # a bare @Capture means "filter = ''", which matches every sibling key
                    cap = None
                    if has_ann(ann, "Capture"):
                        cf = ann_value(ann, "Capture", param="filter")
                        cap = cf if cf is not None else ""
                    key = fm.group("name")
                    if sn is not None:
                        key = sn
                    elif ex is not None:
                        key = ex
                    field = Field(
                        key=key,
                        name=fm.group("name"),
                        type_=(raw, args),
                        capture=cap,
                        descend=ann_flag(ann, "Capture", "descend"),
                        collapse=has_ann(ann, "Collapse"),
                        is_key=has_ann(ann, "Key"),
                        split=ann_value(ann, "Split"),
                    )
                    field.owner = stack[-1][0]
                    # @Capture alongside an explicit name captures inside that nested
                    # object rather than among the declaring class's own siblings
                    if cap is not None and (sn is not None or ex is not None):
                        field.capture = None
                        field.descend = True
                    stack[-1][0].fields.append(field)
                if line and not line.startswith("*"):
                    pend = []

            depth += line.count("{") - line.count("}")
            while stack:
                if depth > stack[-1][1]:
                    stack[-1][2] = True
                    break
                if stack[-1][2]:
                    stack.pop()
                else:
                    break
    return classes


def content_type(classes, type_pair):
    """Unwrap Optional/List/Map down to the type whose fields describe a JSON object."""
    raw, args = type_pair
    simple = raw.split(".")[-1]
    if simple in WRAPPER_TYPES and args:
        return content_type(classes, parse_type(args[0]))
    return raw, args


def resolve(classes, raw, ctx=None):
    """Resolve a type name, preferring classes nested in or declared beside ctx.

    Simple names collide across files (two different nested `Profile` classes, for
    example), so an unscoped lookup picks whichever file sorted first.
    """
    simple = raw.split(".")[-1]
    if raw in OPAQUE or simple in OPAQUE:
        return None
    if ctx is not None:
        # innermost-first: Outer.Inner.Target, then Outer.Target, then same file
        parts = ctx.name.split(".")
        for i in range(len(parts), 0, -1):
            hit = classes.get(".".join(parts[:i]) + "." + raw)
            if hit is not None:
                return hit
        for cls in classes.values():
            if cls.file == ctx.file and cls.simple == simple:
                return cls
    return classes.get(raw) or classes.get(simple)


class Auditor:
    def __init__(self, classes, show_mapped=False, max_depth=12):
        self.classes = classes
        self.show_mapped = show_mapped
        self.max_depth = max_depth
        self.missing = []
        self.mapped = []
        self.unresolved = set()

    def inherited_fields(self, cls):
        """Fields declared on cls plus every superclass in the chain."""
        fields, seen, cur = [], set(), cls
        while cur is not None and cur.name not in seen:
            seen.add(cur.name)
            fields.extend(cur.fields)
            cur = resolve(self.classes, cur.super_name, cur) if cur.super_name else None
        return fields

    def bindings(self, cls):
        """key -> [(remaining_path_tuple, Field)] plus a list of (regex, Field) captures."""
        direct = defaultdict(list)
        captures = []
        for f in self.inherited_fields(cls):
            if f.is_key:
                continue
            if f.capture is not None:
                captures.append((re.compile(f.capture), f))
                continue
            parts = f.key.split(".") if f.key else [""]
            direct[parts[0]].append((tuple(parts[1:]), f))
        return direct, captures

    def walk_class(self, node, cls, path, depth):
        if cls is None or cls.is_enum:
            return
        direct, captures = self.bindings(cls)
        self.walk_bindings(node, direct, captures, path, depth, cls.name)

    def walk_bindings(self, node, direct, captures, path, depth, label):
        if depth > self.max_depth or not isinstance(node, dict):
            return
        for key, value in node.items():
            child = "%s.%s" % (path, key)
            if key in direct:
                bound = direct[key]
                leaf = [f for rem, f in bound if not rem]
                nested = [(rem, f) for rem, f in bound if rem]
                if leaf:
                    if self.show_mapped:
                        self.mapped.append("%s -> %s.%s" % (child, label, leaf[0].name))
                    self.descend(value, leaf[0], child, depth)
                elif nested:
                    merged = defaultdict(list)
                    for rem, f in nested:
                        merged[rem[0]].append((rem[1:], f))
                    self.walk_bindings(value, merged, [], child, depth + 1, label)
            else:
                hit = next((f for rx, f in captures if rx.search(key)), None)
                if hit:
                    if self.show_mapped:
                        self.mapped.append("%s -> %s.%s (@Capture)" % (child, label, hit.name))
                    self.descend(value, hit, child, depth)
                else:
                    self.missing.append((child, describe(value)))

    def descend(self, value, field, path, depth):
        if depth > self.max_depth:
            return
        raw, args = content_type(self.classes, field.type)
        simple = raw.split(".")[-1]

        if simple in MAP_TYPES and args:
            val_raw, val_args = content_type(self.classes, parse_type(args[-1]))
            target = resolve(self.classes, val_raw, field.owner)
            if target and isinstance(value, dict):
                for k, v in value.items():
                    self.walk_class(v, target, "%s.<%s>" % (path, k), depth + 1)
            return

        if simple in SEQ_TYPES and args:
            el_raw, _ = content_type(self.classes, parse_type(args[0]))
            target = resolve(self.classes, el_raw, field.owner)
            if not target:
                return
            # @Collapse turns a JSON object into a list keyed by its field names
            items = value.values() if isinstance(value, dict) else value
            if isinstance(items, (list, type({}.values()))):
                for i, item in enumerate(items):
                    if isinstance(item, dict):
                        self.walk_class(item, target, "%s[%d]" % (path, i), depth + 1)
            return

        target = resolve(self.classes, raw, field.owner)
        if target is None:
            if isinstance(value, dict) and simple not in OPAQUE:
                self.unresolved.add("%s (%s)" % (path, raw))
            return
        if isinstance(value, dict):
            self.walk_class(value, target, path, depth + 1)
        elif isinstance(value, list):
            for i, item in enumerate(value):
                if isinstance(item, dict):
                    self.walk_class(item, target, "%s[%d]" % (path, i), depth + 1)


def describe(v):
    if isinstance(v, dict):
        return "obj{%d} %s" % (len(v), list(v.keys())[:5])
    if isinstance(v, list):
        return "arr[%d]" % len(v)
    if v is None:
        return "null"
    if isinstance(v, str):
        return "str %r" % (v[:40],)
    return "%s %s" % (type(v).__name__, v)


def merge_members(profiles):
    """Union every member object so optional keys from any profile are covered."""
    union = {}

    def merge(dst, src):
        for k, v in src.items():
            if isinstance(v, dict):
                if not isinstance(dst.get(k), dict):
                    dst[k] = {}
                merge(dst[k], v)
            elif isinstance(v, list) and v and isinstance(v[0], dict):
                if not isinstance(dst.get(k), list) or not dst[k]:
                    dst[k] = [{}]
                for item in v:
                    if isinstance(item, dict):
                        merge(dst[k][0], item)
            elif k not in dst or dst[k] is None:
                dst[k] = v

    for p in profiles:
        for member in p.get("members", {}).values():
            merge(union, member)
    return union


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--json", default=DEFAULT_JSON, help="fixture to audit")
    ap.add_argument("--src", default=DEFAULT_SRC, help="java source root to parse")
    ap.add_argument("--root", default="SkyBlockMember", help="DTO class to start from")
    ap.add_argument("--node", default="member",
                    help="'member' (union of all members), 'profile', or a dotted JSON path")
    ap.add_argument("--section", help="limit output to one top-level key of the node")
    ap.add_argument("--show-mapped", action="store_true", help="also list mapped keys")
    ap.add_argument("--show-unresolved", action="store_true",
                    help="list object-valued fields whose Java type could not be parsed")
    ap.add_argument("--max-depth", type=int, default=12)
    args = ap.parse_args()

    doc = json.load(open(args.json, encoding="utf-8"))
    profiles = doc.get("profiles", [])
    if args.node == "member":
        node = merge_members(profiles)
    elif args.node == "profile":
        node = profiles[0]
    else:
        node = doc
        for part in args.node.split("."):
            node = node[part]

    if args.section:
        node = {args.section: node[args.section]}

    classes = parse_sources(args.src)
    root = classes.get(args.root)
    if root is None:
        sys.exit("Root class %r not found under %s" % (args.root, args.src))

    audit = Auditor(classes, args.show_mapped, args.max_depth)
    audit.walk_class(node, root, args.root, 0)

    if args.show_mapped:
        print("=== MAPPED (%d) ===" % len(audit.mapped))
        for line in audit.mapped:
            print("  " + line)
        print()

    if args.show_unresolved and audit.unresolved:
        print("=== UNRESOLVED TYPES (%d) ===" % len(audit.unresolved))
        for line in sorted(audit.unresolved):
            print("  " + line)
        print()

    print("=== UNMAPPED JSON KEYS (%d) ===" % len(audit.missing))
    by_section = defaultdict(list)
    for path, kind in audit.missing:
        parts = path.split(".")
        by_section[parts[1] if len(parts) > 1 else path].append((path, kind))
    for section in sorted(by_section):
        rows = by_section[section]
        print("\n-- %s (%d)" % (section, len(rows)))
        for path, kind in rows:
            print("   %-72s %s" % (path, kind))

    return 1 if audit.missing else 0


if __name__ == "__main__":
    sys.exit(main())

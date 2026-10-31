#!/usr/bin/env python3
"""
One-off converter: reads the legacy src/main/resources/ruro.properties
(Java Properties format, numbered keys) and writes the modern python/supren.json
(nested JSON, no numbering).

Run once from the repo root:
    python python/tools/convert_config.py

Not needed at runtime - supren.json is checked in and edited by hand from
here on. Keep this script only as a record of how the migration was derived,
in case ruro.properties ever needs to be re-mined for a forgotten camera.
"""
import json
import pathlib
import re

REPO_ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = REPO_ROOT / "src/main/resources/ruro.properties"
DEST = REPO_ROOT / "python/supren.json"


def load_properties(path: pathlib.Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        # Only trim for the blank/comment check - Java's Properties loader
        # trims leading whitespace after '=' but KEEPS trailing whitespace
        # in the value, and at least one entry here (cameraModel051 =
        # "USB Camera " with a trailing space, a different physical camera
        # than cameraModel037's "USB Camera") depends on that.
        if not raw_line.strip() or raw_line.strip().startswith("#"):
            continue
        key, _, value = raw_line.partition("=")
        # Java Properties unescaping: only "\\" -> "\" is actually used in
        # this file (no \n \t \uXXXX etc present), so that's all we undo.
        props[key.strip()] = value.lstrip().replace("\\\\", "\\")
    return props


def numbered(props: dict[str, str], prefix: str) -> dict[int, str]:
    out = {}
    pat = re.compile(rf"^{prefix}(\d{{3}})$")
    for key, value in props.items():
        m = pat.match(key)
        if m:
            out[int(m.group(1))] = value
    return out


def nth_top_level_group_span(pattern: str, n: int) -> tuple[int, int] | None:
    """Return the (start, end) index of the opening '(' .. matching ')' for the
    n-th top-level capturing group in pattern (1-indexed). All groups in this
    config are non-nested, so a simple depth counter is enough."""
    depth = 0
    group_no = 0
    start = None
    for i, ch in enumerate(pattern):
        if ch == "(" and (i == 0 or pattern[i - 1] != "\\"):
            if depth == 0:
                group_no += 1
                start = i
            depth += 1
        elif ch == ")" and (i == 0 or pattern[i - 1] != "\\"):
            depth -= 1
            if depth == 0 and group_no == n:
                return (start, i)
    return None


def to_named_number_group(pattern: str, img_num_group: int) -> str:
    """Rewrite the img_num_group-th capturing group as (?P<number>...),
    so the number group no longer needs a side-table index. img_num_group
    of 0 means "no image number in the filename" - left untouched."""
    if img_num_group == 0:
        return pattern
    span = nth_top_level_group_span(pattern, img_num_group)
    if span is None:
        raise ValueError(f"Could not locate group {img_num_group} in pattern {pattern!r}")
    start, end = span
    return pattern[:start] + "(?P<number>" + pattern[start + 1:]


def main() -> None:
    props = load_properties(SRC)

    extensions = [e.strip() for e in props["fileExtensionList"].split(",")]

    infile_patterns = numbered(props, "infilePattern")
    infile_img_num_groups = numbered(props, "infilePatternImgNumGroup")
    source_patterns = []
    for idx in sorted(infile_patterns):
        pattern = infile_patterns[idx]
        img_num_group = int(infile_img_num_groups[idx])
        source_patterns.append(to_named_number_group(pattern, img_num_group))

    out_pattern = props["outfilePattern"]

    camera_models = numbered(props, "cameraModel")
    camera_models_4ch = numbered(props, "cameraModel4ch")
    cameras = {}
    for idx, model in sorted(camera_models.items()):
        code = camera_models_4ch.get(idx)
        if code is None:
            raise ValueError(f"cameraModel{idx:03d} ({model!r}) has no matching cameraModel4ch{idx:03d}")
        if model in cameras and cameras[model] != code:
            print(
                f"WARNING: cameraModel{idx:03d} ({model!r}) collides with an earlier "
                f"entry mapped to {cameras[model]!r}, now overwritten with {code!r}. "
                "Check for an invisible whitespace difference in ruro.properties "
                "(e.g. a trailing space) that used to keep these distinct."
            )
        cameras[model] = code

    config = {
        "extensions": extensions,
        "output": {
            "template": "{date:%Y%m%d}_{camera}_{number:04d}",
            "matches": out_pattern,
        },
        "unknown_camera": props["cameraModel4chUnknown"],
        "cameras": cameras,
        "source_patterns": source_patterns,
        "bestof": {
            "min_rating": 2,
        },
        "cleanup_conflicts": {
            "conflict_suffix": "_conflict",
            "protect_regex": "HDR|Pano|Bearbeitet",
        },
    }

    DEST.write_text(json.dumps(config, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {DEST} with {len(source_patterns)} source patterns and {len(cameras)} cameras.")

    # outfileCase was read by the legacy code but never actually applied -
    # confirmed dead, intentionally dropped.
    assert "outfileCase" in props


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""26.1.2 release preflight and built-jar L0 checks (Python 3.11+, stdlib only).

    python3 scripts/verify_release.py --tag 26.1.2_R3-hotfix2
    python3 scripts/verify_release.py --tag 26.1.2_R3-hotfix2 --artifact

These checks do not constitute a gameplay test. See docs/publish/ci/README.md.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import sys
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parent.parent
AT = "META-INF/accesstransformer.cfg"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def check_source(root: Path, tag: str) -> dict[str, str]:
    properties = {}
    for line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.lstrip().startswith(("#", "!")):
            continue
        key, value = line.split("=", 1)
        key, value = key.strip(), value.strip()
        require(key not in properties, f"Duplicate Gradle property: {key}")
        properties[key] = value
    require(properties["minecraft_version"] == "26.1.2", "This workflow only releases Minecraft 26.1.2")
    require(properties["minecraft_version_range"] == "[26.1.2]", "Minecraft range must remain [26.1.2]")
    require(re.fullmatch(r"26\.1\.2\.\d+", properties["neo_version"]) is not None, "Wrong NeoForge release line")
    require(properties["mod_id"] == "tacz", "mod_id must remain tacz")
    version = properties["mod_version"]
    match = re.fullmatch(r"1\.1\.8\+neoforge\.26\.1\.2\.(R\d+(?:[.-][A-Za-z0-9]+)*)", version)
    require(match is not None, "Use 1.1.8+neoforge.26.1.2.R... build metadata, not a pre-release")
    expected_tag = "26.1.2_" + match.group(1)
    require(tag == expected_tag, f"Tag/version mismatch: expected {expected_tag}, got {tag}")
    notes = (root / "docs/publish/RELEASE_NOTES.md").read_text(encoding="utf-8")
    markers = re.findall(r"^<!-- release-version: (.+) -->$", notes, re.MULTILINE)
    require(markers == [version], "Release notes are a draft or refer to another version; review their release-version marker")
    return properties


def check_artifact(root: Path, properties: dict[str, str]) -> tuple[Path, str]:
    jar_path = root / "build/libs" / f"tacz-{properties['mod_version']}.jar"
    require(jar_path.is_file(), f"Missing exact release artifact: {jar_path.name}")
    resources = root / "src/main/resources"
    configs = {path.name for path in resources.glob("*mixins*.json")}
    require(bool(configs), "No source mixin configs found")
    with zipfile.ZipFile(jar_path) as jar:
        names = jar.namelist()
        require(len(names) == len(set(names)), "Duplicate ZIP entries in release jar")
        require(jar.testzip() is None, "Corrupt release jar")
        metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode("utf-8"))
        mods = [mod for mod in metadata["mods"] if mod["modId"] == "tacz"]
        require(len(mods) == 1 and mods[0]["version"] == properties["mod_version"], "Wrong packaged mod version")
        dependencies = metadata["dependencies"]["tacz"]
        for mod_id, version_range in (
            ("minecraft", properties["minecraft_version_range"]),
            ("neoforge", f"[{properties['neo_version']},)"),
        ):
            matches = [dep for dep in dependencies if dep["modId"] == mod_id]
            require(len(matches) == 1 and matches[0]["versionRange"] == version_range
                    and matches[0]["type"] == "required", f"Wrong packaged {mod_id} requirement")
        declared = [item["config"] for item in metadata["mixins"]]
        packed = {name for name in names if "/" not in name and re.fullmatch(r".*mixins.*\.json", name)}
        require(len(declared) == len(set(declared)) and set(declared) == configs == packed,
                "Mixin configs must agree between source, mods.toml and jar")
        for config in configs:
            require(jar.read(config) == (resources / config).read_bytes(), f"Packaged mixin differs: {config}")
        require([entry["file"] for entry in metadata["accessTransformers"]] == [AT], "Missing AT registration")
        require(jar.read(AT) == (resources / AT).read_bytes(), "Packaged AT differs from source")
        # JarJar's MetadataSerializer/ContainedJarMetadataSerializer: jars[].path.
        embedded = [entry["path"] for entry in json.loads(jar.read("META-INF/jarjar/metadata.json"))["jars"]]
        require(len(embedded) == len(set(embedded)), "Duplicate JarJar metadata paths")
        require(all(path.startswith("META-INF/jarjar/") and path.endswith(".jar") and path in names
                    and ".." not in path.split("/") and "\\" not in path
                    for path in embedded), "JarJar metadata references a missing/invalid library")
        libraries = {
            "luaj": ("org/luaj/vm2/LuaError.class", "org/luaj/vm2/lib/StringLib.class"),
            "commons-math3": ("org/apache/commons/math3/util/FastMath.class",),
        }
        for library, classes in libraries.items():
            paths = [path for path in embedded if library in Path(path).name]
            require(len(paths) == 1, f"Expected one registered {library} jar")
            with zipfile.ZipFile(io.BytesIO(jar.read(paths[0]))) as nested:
                require(nested.testzip() is None, f"Corrupt embedded {library} jar")
                require(all(name in nested.namelist() for name in classes), f"Incomplete embedded {library} jar")
    return jar_path, hashlib.sha256(jar_path.read_bytes()).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--artifact", action="store_true", help="Also inspect the built release jar")
    args = parser.parse_args()
    try:
        properties = check_source(ROOT, args.tag)
        print(f"PASS: release source / notes / tag {args.tag}")
        if args.artifact:
            jar, digest = check_artifact(ROOT, properties)
            print(f"PASS: {jar.name}\nsha256: {digest}")
            if os.environ.get("GITHUB_OUTPUT"):
                with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
                    output.write(f"jar=build/libs/{jar.name}\nsha256={digest}\n")
    except (OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())

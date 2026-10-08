"""Check target source boundaries and the actual distributable JAR."""
from __future__ import annotations

import hashlib
import json
import struct
import zipfile
from pathlib import Path
from typing import TypedDict

from .catalog import Properties, Target, TempleError, validate_identity
from .runner import invocation

class JarResult(TypedDict):
    target: str
    path: str
    sha256: str
    metadata: str
    classes: int

def doctor(root: Path, targets: tuple[Target, ...]) -> list[str]:
    validate_identity(Properties.read(root / "gradle.properties"))
    checked: list[str] = []
    for target in targets:
        sources = (*target.layers(), target.directory)
        known: dict[str, Path] = {}
        for layer in sources:
            directory = layer / "src/main/java"
            for source in directory.rglob("*.java"):
                name = source.relative_to(directory).as_posix()
                if name in known:
                    raise TempleError(f"{target.directory.name}: duplicate Java source {name}: {known[name]} and {source}")
                known[name] = source
        invocation(target, ("build",))
        checked.append(target.directory.name)
    return checked

def audit_jar(target: Target) -> JarResult:
    root = target.directory.parent.parent
    identity = Properties.read(root / "gradle.properties")
    record = target.directory / "build/temple/artifact.properties"
    artifact = Path(Properties.read(record).required("artifact"))
    build_directory = (target.directory / "build").resolve()
    if not artifact.resolve().is_relative_to(build_directory):
        raise TempleError(f"{target.directory.name}: artifact must remain inside its build directory")
    loader = target.properties.required("loader")
    metadata = "fabric.mod.json" if loader == "fabric" else (
        "META-INF/mods.toml" if loader == "forge" else "META-INF/neoforge.mods.toml")
    package = identity.required("mod_group_id").replace(".", "/")
    required = (metadata, f"{package}/TempleCommon.class", f"{package}/BuildInfo.class")
    with zipfile.ZipFile(artifact) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise TempleError(f"{artifact}: duplicate ZIP entries")
        missing = set(required) - set(names)
        if missing:
            raise TempleError(f"{artifact}: missing {sorted(missing)}")
        content = archive.read(metadata).decode("utf-8")
        if "${" in content:
            raise TempleError(f"{artifact}: unexpanded metadata")
        mod_id = identity.required("mod_id")
        if loader == "fabric":
            meta = json.loads(content)
            if meta["id"] != mod_id or meta["depends"]["minecraft"] != "=" + target.properties.required("minecraft_version"):
                raise TempleError(f"{artifact}: wrong Fabric identity or Minecraft version")
            for entry in meta["entrypoints"]["main"]:
                if entry.replace(".", "/") + ".class" not in names:
                    raise TempleError(f"{artifact}: missing entrypoint class {entry}")
        else:
            import tomllib
            meta = tomllib.loads(content)
            if meta["mods"][0]["modId"] != mod_id:
                raise TempleError(f"{artifact}: wrong mod identity")
            dependencies = meta["dependencies"][mod_id]
            expected = target.properties.required("minecraft_version_range")
            if not any(d["modId"] == "minecraft" and d["versionRange"] == expected for d in dependencies):
                raise TempleError(f"{artifact}: wrong Minecraft range")
        classes = [name for name in names if name.startswith(package + "/") and name.endswith(".class")]
        maximum = int(target.properties.required("java_version")) + 44
        for name in classes:
            major = struct.unpack(">H", archive.read(name)[6:8])[0]
            if major > maximum:
                raise TempleError(f"{artifact}: {name} requires bytecode {major}, maximum is {maximum}")
        if any(name.startswith(("tools/", "integrations/", ".omo/", ".codex/")) or
               Path(name).name in ("AGENT.md", "AGENTS.md", "SKILL.md") for name in names):
            raise TempleError(f"{artifact}: contains development or agent integration files")
    with artifact.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    return {"target": target.directory.name, "path": str(artifact),
            "sha256": digest,
            "metadata": metadata, "classes": len(classes)}

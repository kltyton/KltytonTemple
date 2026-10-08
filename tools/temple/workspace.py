"""Create fresh workspaces and target blueprints without overwriting data."""
from __future__ import annotations

import os
import re
import shutil
from dataclasses import dataclass
from pathlib import Path
from types import MappingProxyType
from typing import Final, Iterator

from .catalog import Properties, Target, TempleError, select, validate_identity

EXCLUDED: Final = {".git", ".idea", ".gradle", ".temple", ".omo", ".codex", ".agents",
                   "build", "run", "runs", "out", "__pycache__", "node_modules"}
PRIVATE_NAMES: Final = {"AGENT.md", "AGENTS.md", "AGENTS.override.md", "CLAUDE.md", "GEMINI.md",
                        "NEXT_AGENT.md", "temple.local.properties"}
ROOT_FILES: Final = {"temple.py", "settings.gradle", "build.gradle", "gradle.properties",
                     "gradlew", "gradlew.bat", "LICENSE", "README.md", ".gitignore", ".gitattributes"}
ROOT_DIRS: Final = {"common", "versions", "loaders", "targets", "gradle", "tools",
                    "integrations", ".github", "docs"}

@dataclass(frozen=True, slots=True)
class NewWorkspace:
    directory: Path
    identity: Properties
    targets: tuple[str, ...]

@dataclass(frozen=True, slots=True)
class NewTarget:
    source: Target
    minecraft: str
    changes: Properties
    gradle_version: str | None

def distributable(path: Path) -> bool:
    return (
        not any(part in EXCLUDED or part == "libs" for part in path.parts)
        and path.name not in PRIVATE_NAMES
        and not path.name.endswith((".local.json", ".local.properties", ".iml"))
        and not path.name.startswith(".env")
        and (path.suffix != ".jar" or path.name == "gradle-wrapper.jar")
    )

def source_files(directory: Path, restrict_root: bool = False) -> Iterator[Path]:
    for current, directories, filenames in os.walk(directory):
        base = Path(current)
        directories[:] = [name for name in directories if name not in EXCLUDED and name != "libs"
                          and (not restrict_root or base != directory or name in ROOT_DIRS)]
        for name in directories:
            if (base / name).is_symlink():
                raise TempleError(f"Template contains a symlink directory: {base / name}")
        for name in filenames:
            source = base / name
            relative = source.relative_to(directory)
            if distributable(relative) and (not restrict_root or base != directory or name in ROOT_FILES):
                yield source

def create_workspace(root: Path, request: NewWorkspace) -> Path:
    destination = request.directory.resolve()
    if destination.exists() or destination.is_relative_to(root.resolve()):
        raise TempleError(f"Destination must be a new directory outside the template: {destination}")
    validate_identity(request.identity)
    targets = select(root, list(request.targets))
    target_names = {target.directory.name for target in targets}
    original = Properties.read(root / "gradle.properties")
    old_group = original.required("mod_group_id")
    new_group = request.identity.required("mod_group_id")
    entries: list[tuple[Path, Path]] = []
    for source in source_files(root, True):
        relative = source.relative_to(root)
        if not source.is_file() or not distributable(relative):
            continue
        if relative.parts[0] not in ROOT_DIRS and relative.as_posix() not in ROOT_FILES:
            continue
        if relative.parts[0] == "targets" and relative.parts[1] not in target_names:
            continue
        if source.is_symlink():
            raise TempleError(f"Template contains a symlink; inspect it before initialization: {source}")
        relative = Path(relative.as_posix().replace(old_group.replace(".", "/"), new_group.replace(".", "/")))
        entries.append((source, relative))
    destination.mkdir(parents=True)
    for source, relative in entries:
        output = destination / relative
        output.parent.mkdir(parents=True, exist_ok=True)
        if source.suffix == ".java":
            output.write_text(source.read_text(encoding="utf-8").replace(old_group, new_group), encoding="utf-8")
        else:
            shutil.copy2(source, output)
    values = dict(original.values)
    values.update(request.identity.values)
    Properties(MappingProxyType(values)).write(destination / "gradle.properties")
    return destination

def add_target(root: Path, request: NewTarget) -> Target:
    if not re.fullmatch(r"[0-9]+(?:\.[0-9]+)*", request.minecraft):
        raise TempleError("Minecraft version must contain numeric components")
    source = request.source
    loader = source.properties.required("loader")
    destination = root / "targets" / f"{loader}-{request.minecraft}"
    if destination.exists():
        raise TempleError(f"Target already exists: {destination}")
    values = dict(source.properties.values)
    values.update(request.changes.values)
    values.update(minecraft_version=request.minecraft,
                  minecraft_version_range=f"[{request.minecraft}]",
                  shared_sources=f"common,versions/{request.minecraft},loaders/{loader}")
    if loader == "fabric" and request.minecraft != source.properties.required("minecraft_version"):
        if "fabric_api_version" not in request.changes.values:
            raise TempleError("A new Fabric version requires --property fabric_api_version=<matching version>")
    if request.gradle_version is not None and not re.fullmatch(r"[0-9]+(?:\.[0-9]+)+", request.gradle_version):
        raise TempleError("Gradle version must contain numeric components")
    candidate = Target(destination, Properties(MappingProxyType(values)))
    candidate.validate()
    files = list(source_files(source.directory))
    if any(p.is_symlink() for p in files):
        raise TempleError("Target blueprint contains a symlink")
    destination.mkdir()
    for path in files:
        output = destination / path.relative_to(source.directory)
        output.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, output)
    Properties(MappingProxyType(values)).write(destination / "gradle.properties")
    if request.gradle_version is not None:
        wrapper = destination / "gradle/wrapper/gradle-wrapper.properties"
        text = wrapper.read_text(encoding="utf-8")
        text = re.sub(r"gradle-[0-9.]+-bin", f"gradle-{request.gradle_version}-bin", text)
        text = re.sub(r"(?m)^distributionSha256Sum=.*\n?", "", text)
        wrapper.write_text(text, encoding="utf-8")
    return candidate

def select_ide_target(root: Path, target: Target) -> None:
    """Select one composite build and its Wrapper version for the root IDE import."""
    root = root.resolve()
    if target.directory.parent.parent != root:
        raise TempleError("IDE target must belong to this project")
    identity = Properties.read(root / "gradle.properties")
    validate_identity(identity)
    package = identity.required("mod_group_id")
    constants = target.directory / "build/generated/sources/temple" / package.replace(".", "/") / "BuildInfo.java"
    constants.parent.mkdir(parents=True, exist_ok=True)
    constants.write_text(
        f"package {package};\n\npublic final class BuildInfo {{\n"
        f'    public static final String MOD_ID = "{identity.required("mod_id")}";\n'
        "    private BuildInfo() {}\n}\n", encoding="utf-8")
    wrapper = target.directory / "gradle/wrapper/gradle-wrapper.properties"
    shutil.copy2(wrapper, root / "gradle/wrapper/gradle-wrapper.properties")
    state = root / ".temple/active-target"
    state.parent.mkdir(parents=True, exist_ok=True)
    state.write_text(target.directory.name + "\n", encoding="utf-8")

def inspect_project(directory: Path) -> Properties:
    """Return declared properties and layout facts; never infer versions from directory names."""
    root = directory.resolve()
    values: dict[str, str] = {"path": str(root)}
    for name in ("settings.gradle", "settings.gradle.kts", "build.gradle", "build.gradle.kts",
                 "gradle.properties", "gradle/wrapper/gradle-wrapper.properties"):
        path = root / name
        if path.is_file():
            values[f"file:{name}"] = str(path)
    for name in ("common", "fabric", "forge", "neoforge", "targets", "versions", "build-logic"):
        if (root / name).is_dir():
            values[f"directory:{name}"] = str(root / name)
    props = root / "gradle.properties"
    if props.is_file():
        values.update(Properties.read(props).values)
    for name in ("settings.gradle", "settings.gradle.kts", "build.gradle", "build.gradle.kts"):
        path = root / name
        if path.is_file():
            text = path.read_text(encoding="utf-8-sig")
            for marker in ("architectury", "stonecutter", "com.replaymod.preprocess", "net.neoforged.moddev",
                           "net.minecraftforge.gradle", "fabric-loom", "multiloader"):
                if marker in text.lower():
                    values[f"marker:{marker}"] = name
    return Properties(MappingProxyType(values))

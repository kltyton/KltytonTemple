"""Parse the project identity and independent target catalog."""
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path
from types import MappingProxyType
from typing import Final, Mapping, TypedDict

LOADERS: Final = ("forge", "fabric", "neoforge")
IDENTIFIER: Final = re.compile(r"[a-z][a-z0-9_]{1,63}")
PACKAGE: Final = re.compile(r"[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)+")

class TempleError(Exception):
    """A project operation failed at an input or filesystem boundary."""

@dataclass(frozen=True, slots=True)
class Properties:
    values: Mapping[str, str]

    @classmethod
    def read(cls, path: Path) -> Properties:
        values: dict[str, str] = {}
        for number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
            text = line.strip()
            if not text or text.startswith(("#", "!")):
                continue
            key, separator, value = text.partition("=")
            key = key.strip()
            if not separator or not key or key in values:
                raise TempleError(f"{path}:{number}: expected a unique key=value entry")
            values[key] = value.strip()
        return cls(MappingProxyType(values))

    def required(self, name: str) -> str:
        value = self.values.get(name, "")
        if not value:
            raise TempleError(f"Missing property: {name}")
        return value

    def write(self, path: Path) -> None:
        path.write_text("".join(f"{key}={value}\n" for key, value in self.values.items()), encoding="utf-8")

class TargetInfo(TypedDict):
    target: str
    loader: str
    minecraft: str
    java: int
    gradle_java: int
    gradle: str
    path: str
    shared_sources: list[str]
    ci: bool

@dataclass(frozen=True, slots=True)
class Target:
    directory: Path
    properties: Properties

    @classmethod
    def read(cls, directory: Path) -> Target:
        target = cls(directory.resolve(), Properties.read(directory / "gradle.properties"))
        target.validate()
        for filename in ("build.gradle", "settings.gradle", "gradlew", "gradlew.bat",
                         "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"):
            if not (directory / filename).is_file():
                raise TempleError(f"{directory}: missing {filename}")
        return target

    def validate(self) -> None:
        directory = self.directory
        props = self.properties
        loader = props.required("loader")
        version = props.required("minecraft_version")
        if loader not in LOADERS or not re.fullmatch(r"[0-9]+(?:\.[0-9]+)*", version):
            raise TempleError(f"{directory}: invalid loader or Minecraft version")
        if directory.name != f"{loader}-{version}":
            raise TempleError(f"{directory}: directory must match loader-{version}")
        for key in ("java_version", "gradle_java_version"):
            value = props.required(key)
            if not value.isdigit() or int(value) < 8:
                raise TempleError(f"{directory}: {key} must be a Java major version")
        for key in ("loader_version", "minecraft_version_range", "loader_version_range"):
            value = props.required(key)
            if "\n" in value or "\r" in value:
                raise TempleError(f"{key} must stay on one line")
        self.layers()
        if props.values.get("ci_enabled", "true") not in ("true", "false"):
            raise TempleError(f"{directory}: ci_enabled must be true or false")

    def layers(self) -> tuple[Path, ...]:
        root = self.directory.parent.parent.resolve()
        names = self.properties.required("shared_sources").split(",")
        paths: list[Path] = []
        for name in names:
            path = (root / name.strip()).resolve()
            if path == root or not path.is_relative_to(root) or not name.strip():
                raise TempleError(f"{self.directory}: shared source must stay below the project root: {name}")
            if path.is_relative_to(root / "targets"):
                raise TempleError(f"{self.directory}: another target cannot be a shared source")
            if path in paths:
                raise TempleError(f"{self.directory}: duplicate shared source: {name}")
            paths.append(path)
        return tuple(paths)

    def info(self) -> TargetInfo:
        wrapper = Properties.read(self.directory / "gradle/wrapper/gradle-wrapper.properties")
        url = wrapper.required("distributionUrl")
        match = re.search(r"gradle-([0-9.]+)-", url)
        if match is None:
            raise TempleError(f"{self.directory}: unrecognized Wrapper distribution URL")
        return {
            "target": self.directory.name, "loader": self.properties.required("loader"),
            "minecraft": self.properties.required("minecraft_version"),
            "java": int(self.properties.required("java_version")),
            "gradle_java": int(self.properties.required("gradle_java_version")),
            "gradle": match.group(1), "path": str(self.directory),
            "shared_sources": [str(p) for p in self.layers()],
            "ci": self.properties.values.get("ci_enabled", "true") == "true",
        }

def catalog(root: Path) -> tuple[Target, ...]:
    directories = sorted(path for path in (root / "targets").iterdir() if path.is_dir())
    targets = tuple(Target.read(path) for path in directories)
    if not targets:
        raise TempleError(f"{root}: no targets found")
    return targets

def select(root: Path, names: list[str]) -> tuple[Target, ...]:
    available = {target.directory.name: target for target in catalog(root)}
    unknown = set(names) - available.keys()
    if unknown:
        raise TempleError(f"Unknown targets: {', '.join(sorted(unknown))}. Available: {', '.join(available)}")
    return tuple(available[name] for name in dict.fromkeys(names)) if names else tuple(available.values())

def validate_identity(props: Properties) -> None:
    if not IDENTIFIER.fullmatch(props.required("mod_id")):
        raise TempleError("mod_id must be 2-64 lowercase letters, digits or underscores, starting with a letter")
    if not PACKAGE.fullmatch(props.required("mod_group_id")):
        raise TempleError("mod_group_id must be a lowercase dotted Java package")
    for key in ("mod_name", "mod_version", "mod_license", "mod_authors", "mod_description"):
        props.required(key)

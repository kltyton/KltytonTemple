"""Plan and execute each target's own Gradle Wrapper with its runtime JDK."""
from __future__ import annotations

import os
import re
import shutil
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Mapping, TypedDict

from .catalog import Properties, Target, TempleError

class BuildResult(TypedDict):
    target: str
    command: list[str]
    cwd: str
    exit_code: int | None
    log: str | None

@dataclass(frozen=True, slots=True)
class Invocation:
    target: Target
    command: tuple[str, ...]
    environment: Mapping[str, str]

def local_properties(root: Path) -> Properties:
    path = root / "temple.local.properties"
    return Properties.read(path) if path.exists() else Properties({})

def java_major(home: Path) -> int:
    release = home / "release"
    if not release.is_file():
        raise TempleError(f"JDK release file missing: {release}")
    match = re.search(r'JAVA_VERSION="(?:1\.)?([0-9]+)', release.read_text(encoding="utf-8"))
    if match is None:
        raise TempleError(f"JDK version cannot be read: {release}")
    return int(match.group(1))

def invocation(target: Target, arguments: tuple[str, ...]) -> Invocation:
    root = target.directory.parent.parent
    settings = local_properties(root).values
    major = int(target.properties.required("gradle_java_version"))
    environment = dict(os.environ)
    home = settings.get(f"java.{major}.home") or environment.get(f"JAVA_{major}_HOME") or environment.get(f"JAVA_HOME_{major}_X64")
    if home is None:
        candidate = environment.get("JAVA_HOME")
        if candidate and java_major(Path(candidate)) == major:
            home = candidate
        else:
            executable = shutil.which("java")
            if executable and java_major(Path(executable).resolve().parent.parent) == major:
                home = str(Path(executable).resolve().parent.parent)
    if home is None or java_major(Path(home)) != major:
        raise TempleError(f"{target.directory.name}: set JAVA_{major}_HOME or java.{major}.home in temple.local.properties")
    environment["JAVA_HOME"] = home
    environment["PATH"] = str(Path(home) / "bin") + os.pathsep + environment.get("PATH", "")
    temp = settings.get("temp_dir") or environment.get("TEMPLE_TEMP_DIR") or str(root / ".temple/tmp")
    for key in ("TEMP", "TMP", "TMPDIR"):
        environment[key] = temp
    if "gradle_user_home" in settings:
        environment["GRADLE_USER_HOME"] = settings["gradle_user_home"]
    java_paths = [v for k, v in settings.items() if re.fullmatch(r"java\.[0-9]+\.home", k)]
    java_paths.extend(v for k, v in environment.items() if re.fullmatch(r"JAVA_[0-9]+_HOME|JAVA_HOME_[0-9]+_X64", k))
    command = [str(target.directory / ("gradlew.bat" if os.name == "nt" else "gradlew"))]
    if os.name != "nt":
        command.insert(0, "sh")
    command.extend(arguments)
    command.extend(("--console=plain", "--no-daemon", f"-Djava.io.tmpdir={temp}"))
    if java_paths:
        command.append("-Dorg.gradle.java.installations.paths=" + ",".join(dict.fromkeys(java_paths)))
    cache = settings.get("project_cache_dir")
    if cache:
        command.extend(("--project-cache-dir", str(Path(cache) / target.directory.name)))
    return Invocation(target, tuple(command), environment)

def execute(call: Invocation, log_directory: Path | None) -> BuildResult:
    arguments = list(call.command)
    command: str | list[str] = arguments
    if os.name == "nt":
        if any(re.search(r'[&|<>^%!\r\n]', part) for part in arguments):
            raise TempleError("Windows Wrapper arguments cannot contain cmd metacharacters")
        prefix = subprocess.list2cmdline([os.environ.get("COMSPEC", "cmd.exe"), "/d", "/s", "/c"])
        command = prefix + ' "' + subprocess.list2cmdline(arguments) + '"'
    Path(call.environment["TEMP"]).mkdir(parents=True, exist_ok=True)
    log = log_directory / f"{call.target.directory.name}.log" if log_directory else None
    if log is not None:
        log.parent.mkdir(parents=True, exist_ok=True)
        with log.open("wb") as stream:
            result = subprocess.run(command, cwd=call.target.directory, env=call.environment,
                                    stdout=stream, stderr=subprocess.STDOUT, check=False)
    else:
        result = subprocess.run(command, cwd=call.target.directory, env=call.environment, check=False)
    return {"target": call.target.directory.name, "command": list(call.command),
            "cwd": str(call.target.directory), "exit_code": result.returncode,
            "log": str(log) if log else None}

def plan(call: Invocation) -> BuildResult:
    return {"target": call.target.directory.name, "command": list(call.command),
            "cwd": str(call.target.directory), "exit_code": None, "log": None}

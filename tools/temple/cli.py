"""Human and JSON command surface for the same target operations."""
from __future__ import annotations

import argparse
import json
import re
import sys
from enum import Enum
from pathlib import Path
from types import MappingProxyType
from typing import assert_never
from zipfile import BadZipFile
from tomllib import TOMLDecodeError

from .audit import audit_jar, doctor
from .catalog import Properties, TempleError, select
from .runner import execute, invocation, plan
from .workspace import NewTarget, NewWorkspace, add_target, create_workspace, inspect_project, select_ide_target

class Command(Enum):
    LIST = "list"
    MATRIX = "matrix"
    INSPECT = "inspect"
    DOCTOR = "doctor"
    IDE = "ide"
    SELECT = "select"
    INIT = "init"
    ADD = "add-target"
    BUILD = "build"
    RUN = "run"
    PUBLISH = "publish"
    VERIFY = "verify"

def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description="KltytonTemple: isolated Minecraft targets with shared sources")
    result.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    commands = result.add_subparsers(dest="command", required=True)
    for command in Command:
        child = commands.add_parser(command.value)
        child.add_argument("--json", action="store_true", help="Return machine-readable output")
        if command not in (Command.INSPECT, Command.INIT, Command.ADD):
            child.add_argument("--target", action="append", default=[], help="Select a target; repeat to select several")
        match command:
            case Command.INIT:
                child.add_argument("destination", type=Path)
                child.add_argument("--mod-id", required=True)
                child.add_argument("--name", required=True)
                child.add_argument("--group", required=True)
                child.add_argument("--authors", required=True)
                child.add_argument("--version", default="1.0.0")
                child.add_argument("--license", default="MIT")
                child.add_argument("--description", default="A Minecraft mod.")
                child.add_argument("--target", action="append", default=[])
            case Command.ADD:
                child.add_argument("--from", dest="source", required=True)
                child.add_argument("--minecraft", required=True)
                child.add_argument("--loader-version", required=True)
                child.add_argument("--java", type=int, required=True)
                child.add_argument("--gradle-java", type=int, required=True)
                child.add_argument("--gradle-version")
                child.add_argument("--property", action="append", default=[], help="Target property override key=value")
            case Command.INSPECT:
                child.add_argument("directory", type=Path)
            case Command.BUILD | Command.RUN | Command.PUBLISH:
                child.add_argument("--plan", action="store_true", help="Show the invocation without executing Gradle")
                child.add_argument("--log-dir", type=Path)
                child.add_argument("--gradle-arg", action="append", default=[])
                if command is Command.RUN:
                    child.add_argument("--side", choices=("client", "server", "data"), default="client")
                if command is Command.PUBLISH:
                    child.add_argument("--platform", choices=("modrinth", "curseforge", "both", "maven"), required=True)
                    child.add_argument("--execute", action="store_true", help="Actually upload; otherwise only show plan")
            case Command.LIST | Command.MATRIX | Command.DOCTOR | Command.IDE | Command.SELECT | Command.VERIFY:
                pass
            case unreachable:
                assert_never(unreachable)
    return result

def main() -> int:
    arguments = parser().parse_args()
    root = arguments.root.resolve()
    try:
        command = Command(arguments.command)
        match command:
            case Command.LIST:
                data = [target.info() for target in select(root, arguments.target)]
            case Command.MATRIX:
                data = {"include": [target.info() for target in select(root, arguments.target) if target.info()["ci"]]}
            case Command.INSPECT:
                data = dict(inspect_project(arguments.directory).values)
            case Command.DOCTOR:
                data = {"checked": doctor(root, select(root, arguments.target)),
                        "scope": "properties, source collisions, Wrapper files and configured JDK; no Gradle execution"}
            case Command.IDE | Command.SELECT:
                targets = select(root, arguments.target)
                if len(targets) != 1:
                    raise TempleError("IDE import requires exactly one --target")
                if command is Command.SELECT:
                    select_ide_target(root, targets[0])
                data = {"open_directory": str(targets[0].directory),
                        "gradle_java": targets[0].info()["gradle_java"],
                        "shared_sources": [str(path) for path in targets[0].layers()],
                        "root_import": str(root), "refresh_gradle": command is Command.SELECT}
            case Command.INIT:
                identity = Properties(MappingProxyType({
                    "mod_id": arguments.mod_id, "mod_name": arguments.name,
                    "mod_group_id": arguments.group, "mod_authors": arguments.authors,
                    "mod_version": arguments.version, "mod_license": arguments.license,
                    "mod_description": arguments.description,
                }))
                request = NewWorkspace(arguments.destination, identity, tuple(arguments.target))
                data = {"created": str(create_workspace(root, request))}
            case Command.ADD:
                source = select(root, [arguments.source])[0]
                changes = {"loader_version": arguments.loader_version,
                           "java_version": str(arguments.java), "gradle_java_version": str(arguments.gradle_java)}
                for entry in arguments.property:
                    key, separator, value = entry.partition("=")
                    if not separator or not re.fullmatch(r"[a-z][a-z0-9_.]*", key) or "\n" in value or "\r" in value:
                        raise TempleError("--property requires a single-line key=value")
                    changes[key] = value
                request = NewTarget(source, arguments.minecraft,
                                    Properties(MappingProxyType(changes)), arguments.gradle_version)
                data = add_target(root, request).info()
            case Command.VERIFY:
                data = [audit_jar(target) for target in select(root, arguments.target)]
            case Command.BUILD | Command.RUN | Command.PUBLISH:
                targets = select(root, arguments.target)
                if command in (Command.RUN, Command.PUBLISH) and len(targets) != 1:
                    raise TempleError("Run and publish require exactly one --target")
                task = "build"
                if command is Command.RUN:
                    task = {"client": "runClient", "server": "runServer", "data": "runData"}[arguments.side]
                    if arguments.side == "data" and targets[0].properties.required("loader") == "fabric":
                        task = "runDatagen"
                if command is Command.PUBLISH:
                    task = "publish" if arguments.platform == "maven" else "publishMods"
                gradle_arguments = [task, *arguments.gradle_arg]
                if command is Command.PUBLISH:
                    gradle_arguments.append(f"-Ppublish_platform={arguments.platform}")
                calls = [invocation(target, tuple(gradle_arguments)) for target in targets]
                planning = arguments.plan or (command is Command.PUBLISH and not arguments.execute)
                if planning:
                    data = [plan(call) for call in calls]
                else:
                    if arguments.json and arguments.log_dir is None:
                        raise TempleError("--json execution requires --log-dir to keep Gradle output separate")
                    data = [execute(call, arguments.log_dir) for call in calls]
                    print(json.dumps(data, ensure_ascii=False, indent=2))
                    return int(any(item["exit_code"] != 0 for item in data))
            case unreachable:
                assert_never(unreachable)
        print(json.dumps(data, ensure_ascii=False, indent=2))
        return 0
    except (TempleError, OSError, BadZipFile, TOMLDecodeError, json.JSONDecodeError) as error:
        print(json.dumps({"error": str(error)}, ensure_ascii=False), file=sys.stderr)
        return 2

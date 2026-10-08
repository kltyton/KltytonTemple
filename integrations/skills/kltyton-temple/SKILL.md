---
name: kltyton-temple
description: Create, maintain, or migrate Minecraft projects to KltytonTemple, including official MDKs, MultiLoader, Stonecutter, Architectury and SighsTemple.
---

# KltytonTemple

Locate the template checkout containing buildSrc, targets and integrations. Read the active edit and acceptance contract before running anything. This interface needs no MCP service or Python runtime.

## Select the work

For a new project, use the Minecraft Development wizard or the native Gradle createProject form. The target selector reads official catalogs; it does not upgrade existing target pins. Adding a catalog version is scaffolding, not proof of compatibility.

For migration, read [migration.md](references/migration.md). Identify the actual build system first and preserve its required transforms and task dependencies. Use the repository's Chinese or English prompt when the developer needs a copyable request:
- ../../prompts/migrate.zh-CN.md
- ../../prompts/migrate.en.md

## Target facts

Read root identity and every relevant target's properties, Wrapper, settings/build, source paths and Loader metadata. Keep mod_java_package and mod_entry_class when a consumer separates its Java namespace from Maven coordinates.

Target IDs use loader-Minecraft; task suffixes replace hyphens and dots with underscores. Targets are independent Gradle projects, preserving their Wrapper and Gradle JDK. Default selection affects root task aliases, not IDE module inclusion.

- listTargets reports configured target facts.
- select_<suffix> sets the default target.
- build_<suffix>, runClient_<suffix>, runServer_<suffix>, runDatagen_<suffix> execute the target's own Wrapper.
- verify_<suffix> inspects an existing release JAR without building.
- createProject/addTarget open version forms.
- publish_<platform>_<suffix> opens a publishing form and needs upload authorization.

## Preserve consumer behavior

Shared code must compile against each target's actual Minecraft environment. Keep client-only classes isolated. Preserve handwritten resources, generated-resource ownership, registry order, Mixin/refmap, AT/AW/ClassTweaker, optional compatibility boundaries, dependencies and publishing artifacts.

The IDEA extension's temple_idea_project=true marker enables automatic target linking. Keep it off for a consumer whose current contract prohibits IDE/Gradle import or runtime validation.

Do not rewrite game code or remove a preprocessor just to match the template directory diagram. Do not edit the reference template as the migration output. A copied blueprint is not accepted support.

Finish production edits before the authorized verification batch. Report source, IDE import, build, JAR and game evidence separately. This Skill does not authorize commits, remote writes, uploads, account changes or messages.

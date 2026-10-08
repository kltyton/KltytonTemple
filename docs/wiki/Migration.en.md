# Migrate an existing project

[中文](Migration.zh-CN.md) · [Wiki](Home.en.md)

After migration, shared logic is maintained once in `common` and used by every target. Version and Loader layers contain only necessary API implementation differences.

## Use an AI

Copy the [migration prompt](../../../integrations/prompts/migrate.en.md). Fill in the existing project, template, destination, edit scope and exact acceptance contract.

The [migration Skill](../../../integrations/skills/kltyton-temple/SKILL.md) can be read directly without an MCP service. It covers official MDKs, MultiLoader, Stonecutter, Architectury and SighsTemple. Keep private session records and global AI rules out of the game repository.

## Inventory

Record each Minecraft/Loader combination's Wrapper, Gradle JDK, compiler JDK, mappings, Loader/API and build-plugin versions. Inspect mod ID, group, package, entry points, resources, access configuration, Mixins, datagen, run settings and publishing IDs.

Compare these facts before and after migration. Template defaults must not overwrite them.

## Preserve the original pipeline

| Source | Preserve |
| --- | --- |
| Official MDK | Loader plugin, dependencies, run tasks, metadata and access hooks |
| MultiLoader | Common producers/consumers, per-loader compilation and resource scope |
| Stonecutter | Controller, nodes, constants, dependency swaps and preprocessing dependencies |
| Architectury | Common transformation, platform declarations, expect/actual, remapping and runtime linkage |
| SighsTemple | Target matrix, source precedence, metadata and Wrapper/JDK pins |

Listing a Stonecutter-generated folder as a source directory does not replace its producer task. Copying common sources does not replace Architectury transformations. Find actual task names in the existing configuration.

## New entry points

Move the active target's game sources and shared resources into `common/src`, then compare all target implementations and read their matching version sources. Put evidenced API differences in version, Loader or target layers. Extract small SPI boundaries and move shared algorithms and business logic back into common; a few differing API calls do not justify copying an entire class per version.

Point `shared_sources` at the migrated layers and update build, access-hook, Mixin, producer and publishing paths. Preserve handwritten-resource ownership. A shared feature should be maintained in common without duplicate edits to each version's implementation.

Root tasks offer target operations and default-target aliases. IDEA links targets independently, each with its own Wrapper. Sync All does not omit other targets merely because Forge 1.20.1 is the default.

## Acceptance

Finish production changes first, then run only the supplied acceptance contract. In source-only work, run no Gradle, tests, datagen or games, but still complete source consolidation, file migration and reference updates.

Report source mapping, IDE modules, target builds, release packages and game checks separately. Keep unexecuted checks marked unverified. Remove obsolete entry points only after the replacement is accepted; preserve user work, saves, local dependencies and still-used transforms.

# Migrate an existing project

[中文](Migration.zh-CN.md) · [Wiki](Home.en.md)

The migrated project uses the same organization as a project created with KltytonTemple. Shared logic is maintained once in `common` and used by every target. Version and Loader layers contain only necessary API implementation differences.

## Use an AI

Copy the [migration prompt](../../integrations/prompts/migrate.en.md). Fill in the existing project, template, destination, edit scope and exact acceptance contract.

The [migration Skill](../../integrations/skills/kltyton-temple/SKILL.md) can be read directly without an MCP service. It covers official MDKs, MultiLoader, Stonecutter, Architectury and SighsTemple. Keep private session records and global AI rules out of the game repository.

## Inventory

Record each Minecraft/Loader combination's Wrapper, Gradle JDK, compiler JDK, mappings, Loader/API and build-plugin versions. Inspect mod ID, group, package, entry points, resources, access configuration, Mixins, datagen, run settings and publishing IDs.

Compare these facts before and after migration. Template defaults must not overwrite them.

## Preserve the original pipeline

| Source | Preserve |
| --- | --- |
| Official MDK | Loader plugin, dependencies, run tasks, metadata and access hooks |
| MultiLoader | Common producers/consumers, per-loader compilation and resource scope |
| Stonecutter | Translate complete version-branch behavior to ordinary Java adapters and dependency swaps to target configuration |
| Architectury | Common transformation, platform declarations, expect/actual, remapping and runtime linkage |
| SighsTemple | Target matrix, source precedence, metadata and Wrapper/JDK pins |

Translate Stonecutter branches into ordinary Java adapters before removing their conditional comments and preprocessing chain. Required Architectury transforms still need their real producers and consumers.

Preserve the original functionality while replacing its project shell. Move producers and consumers into the new `gradle` conventions and `targets` entries, then retire the replaced controllers. Do not keep two parallel build structures calling each other.

## New entry points

Use the [source directory rules](../dev/source-layout.en.md) to establish responsibility packages, compilation layers, selected targets and consumers. Custom API capability layers need explicit predicates. Unassigned production paths prevent completion. Synchronize stale models; if current models still leave a path unassigned, fix its actual layer selection rather than placing all variants under one SDK. Under an authorized acceptance contract, use `verifySourceLayout` for the complete ownership list.

Move the active target's game sources and shared resources into `common/src/main`, then compare all target implementations and read their matching version sources. Put evidenced API differences in `common/src/compat`. Extract small SPI boundaries and move shared algorithms and business logic back into common; a few differing API calls do not justify copying an entire class per version.

Declare shared dependency scopes in `common/build.gradle(.kts)` and include a real `:common` module in each target with that target's SDK. Update build, access-hook, Mixin, producer and publishing paths. Preserve handwritten-resource ownership. A shared feature should be maintained in common without duplicate edits to each version's implementation.

Maintain sources inside common without parallel version or Loader project trees. Add adapters only for actual differences, group Java packages by feature and retain one independent target build per Loader/Minecraft combination.

Root tasks offer target operations and default-target aliases. IDEA links targets independently, each with its own Wrapper. Sync All does not omit other targets merely because Forge 1.20.1 is the default.

## Acceptance

Finish production changes first, then run only the supplied acceptance contract. In source-only work, run no Gradle, tests, datagen or games, but still complete source consolidation, file migration and reference updates.

Report source mapping, IDE modules, target builds, release packages and game checks separately. Keep unexecuted checks marked unverified. Compare source ownership and build roots against the generator's file list. Retire replaced settings/build files, Wrappers, Eclipse metadata, IDE links and old cache/generated directories after checking ownership and consumers. Preserve user work, saves, private configuration and local dependencies. Do not rename retired projects and keep them in the repository. Cache-only old trees or visible IDE modules do not mean structural migration is complete.

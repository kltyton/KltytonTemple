# KltytonTemple

[中文](README.md) · [Wiki](https://github.com/kltyton/KltytonTemple/wiki) · [Migration](docs/wiki/Migration.en.md)

Keep multiple Minecraft versions and mod loaders in one repository. Each target owns its Wrapper, runtime JDK, compiler JDK, mappings and dependencies. Shared sources are compiled against each target's Minecraft classpath.

The template is based on [SighsTemple](https://github.com/Tower-of-Sighs/SighsTemple). MultiLoader, Stonecutter and Architectury informed its layout. Their preprocessors, API transformations and runtime libraries are added only when the project needs them.

## Create or migrate

The Minecraft Development integration provides project fields and version selectors, with Simplified Chinese and English. Minecraft and Loader catalogs come from Mojang, Fabric, Forge and NeoForge. Fabric API versions are queried for the selected Minecraft release from Fabric's project on Modrinth. Java requirements come from Minecraft metadata. Selected versions are written to target configuration; refreshing the catalog does not upgrade existing projects.

Gradle script language offers Groovy (`.gradle`) and Kotlin DSL (`.gradle.kts`). Both use the same target configuration, source layers and tasks. Kotlin projects generate KTS root builds, target builds, settings, buildSrc build scripts and shared conventions. `addTarget` follows the project's selection. This choice does not change the mod source language.

- [Install the extension and create a project](docs/wiki/Minecraft-Development.en.md)
- [Migrate an existing project](docs/wiki/Migration.en.md)
- [Copy the migration prompt](integrations/prompts/migrate.en.md)
- [Migration Skill](integrations/skills/kltyton-temple/SKILL.md)

## Gradle tasks

Run commands from the project root. Use `.\gradlew.bat` on Windows and `./gradlew` elsewhere.

| Task | Purpose |
| --- | --- |
| `listTargets` | List target versions, Java requirements and Wrappers |
| `createProject` | Open the project form; refuse existing output directories |
| `addTarget` | Open the target selector; refuse to replace existing targets |
| `setupProject` | Initialize Wrappers and identity sources without building Minecraft |
| `prepareIde` | Prepare identity sources for all targets |
| `select_<target>` | Set the default target for root tasks |
| `build` | Build the default target |
| `build_<target>` | Build one target |
| `buildAllTargets` | Build all targets |
| `runClient_<target>` | Start a development client |
| `runServer_<target>` | Start a development server |
| `runDatagen_<target>` | Run the target's resource generator |
| `verify_<target>` | Inspect an existing release JAR without rebuilding |
| `verifyAllDistributions` | Inspect all existing release JARs |
| `publish_modrinth_<target>` | Open the Modrinth publishing form |
| `publish_curseforge_<target>` | Open the CurseForge publishing form |
| `publish_maven_<target>` | Open the Maven publishing form |
| `exportMinecraftTemplates` | Generate Minecraft Development template assets |
| `writeCiMatrix` | Emit the enabled-target CI matrix |

Replace hyphens and dots in target IDs with underscores: `fabric-1.21.1` becomes `fabric_1_21_1`.

```sh
./gradlew build_fabric_1_21_1
./gradlew verify_fabric_1_21_1
./gradlew runClient_fabric_1_21_1
./gradlew select_neoforge_26_1_2
```

`runClient`, `runServer` and `runDatagen` address the default target. Selecting a default target does not remove other targets from IDEA.

`-PtemplePlan=true` prints target build/run plans and skips their child builds:

```sh
./gradlew buildAllTargets -PtemplePlan=true
```

A printed plan is not a passing build.

## Configuration

Root `gradle.properties` defines mod identity. Target files define build environments.

| Property | Location | Meaning |
| --- | --- | --- |
| `mod_id` | Root | Registry and resource namespace |
| `mod_name` | Root | Display name |
| `mod_group_id` | Root | Maven group and default Java package |
| `mod_java_package` | Root, optional | Existing Java package independent of the Maven group |
| `mod_entry_class` | Root, optional | Existing common entry class used by JAR inspection |
| `mod_version` | Root | Mod version |
| `mod_authors` | Root | Authors |
| `mod_license` | Root | License |
| `mod_description` | Root | Description |
| `temple_default_target` | Root | Default target when no local selection exists |
| `temple_idea_project` | Root | Enable independent target linking in IDEA |
| `temple_build_dsl` | Root | `groovy` or `kotlin`; written by creation forms and used for additional target scripts |
| `loader`, `minecraft_version` | Target | Loader and Minecraft release |
| `loader_version` | Target | Loader version |
| `fabric_api_version` | Fabric target | Fabric API version |
| `java_version` | Target | Java version for compiling game code |
| `gradle_java_version` | Target | Java version for running target Gradle |
| `shared_sources` | Target | Comma-separated source layers relative to the root |
| `ci_enabled` | Target | Include the target in CI |
| `datagen_task` | Target, optional | Configured resource-generation task; no datagen alias is registered when absent |

Loader and Minecraft ranges remain in target metadata properties. Each target's `gradle/wrapper/gradle-wrapper.properties` selects its Wrapper version.

## Sources and resources

```text
common/src/                 Shared sources and resources
versions/<minecraft>/src/   Version differences
loaders/<loader>/src/       Loader entry points
targets/<loader>-<mc>/      Target builds, metadata and cross-cutting differences
gradle/target-conventions/  Build conventions
buildSrc/                  Gradle entry points
integrations/              IDEA, wizard and migration interface
```

Maintain shared algorithms and game logic once in `common`, compiled by every target. Version and Loader layers contain actual API differences behind small SPI boundaries; avoid copying entire implementations. `shared_sources` selects each target's source layers. Java files with the same name do not override each other. Resource precedence is target > loader > version > common. Handwritten resources stay in `src/main/resources`; datagen output goes to each target's `src/generated/resources`.

Migration uses the same organization as a newly created project. Each Minecraft release has one `versions/<minecraft>` difference layer; each Loader/Minecraft pair has one target build root. Move sources and required build pipelines into these locations, then retire replaced project shells, IDE links and caches. No version folder is needed without actual differences. Generated projects include build controls and migration interfaces; IDEA plugin sources and wizard assets stay in the template repository.

The IDEA extension links targets as independent Gradle projects. “Sync All Gradle Projects” includes all linked targets, each using its own Wrapper. Development integration code is not packaged in mod JARs.

## Baseline targets

| Minecraft | Loader |
| --- | --- |
| 1.18.2, 1.19.2 | Forge |
| 1.20.1 | Fabric, Forge |
| 1.21.1 | Fabric, NeoForge |
| 26.1.2 | Fabric, NeoForge |
| 26.2.0 | NeoForge |

The nine examples do not limit the dropdown catalog. It includes official Minecraft releases from 1.17 within the available build pipelines, with Loader and Fabric API release history. “Recommended” is a label, not a filter. The form can include preview Loader/API releases. New combinations still require matching APIs and build acceptance; selecting a release does not port existing mod code to it.

[Architecture](docs/dev/architecture.md) · [CI](docs/CI_TARGET_DISCOVERY.md) · [Publishing](docs/PUBLISHING.md)

## License

The template is MIT-licensed and retains SighsTemple's copyright notice. Game code, assets and dependencies retain their own licenses. The IDEA extension depends on the installed Minecraft Development plugin and does not redistribute its binaries.

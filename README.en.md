# KltytonTemple

[中文](README.md) · [Wiki](https://github.com/kltyton/KltytonTemple/wiki) · [Migration](docs/wiki/Migration.en.md)

Keep multiple Minecraft versions and mod loaders in one repository. Each target owns its Wrapper, runtime JDK, compiler JDK, mappings and dependencies. Shared sources are compiled against each target's Minecraft classpath.

The template is based on [SighsTemple](https://github.com/Tower-of-Sighs/SighsTemple). MultiLoader, Stonecutter and Architectury informed its layout. The common module and dependency scopes use native Gradle models. Version adapters are ordinary Java sources without conditional-comment preprocessing.

## Create or migrate

The Minecraft Development integration provides project fields and version selectors, with Simplified Chinese and English. Minecraft and Loader catalogs come from Mojang, Fabric, Forge and NeoForge. Fabric API versions are queried for the selected Minecraft release from Fabric's project on Modrinth. Java requirements come from Minecraft metadata. Selected versions are written to target configuration; refreshing the catalog does not upgrade existing projects.

Gradle script language offers Groovy (`.gradle`) and Kotlin DSL (`.gradle.kts`). Both use the same target configuration, source layers and tasks. Kotlin projects use KTS root builds, target builds, settings, buildSrc build scripts and their matching conventions, while sharing the source-model export script. `addTarget` follows the project's selection. This choice does not change the mod source language.

- [Install the extension and create a project](docs/wiki/Minecraft-Development.en.md)
- [Migrate an existing project](docs/wiki/Migration.en.md)
- [Copy the migration prompt](integrations/prompts/migrate.en.md)
- [Migration Skill](integrations/skills/kltyton-temple/SKILL.md)

## Gradle tasks

The root uses Gradle 9.8.2 and can run on Java 21 or 25. Targets retain their own Wrappers and runtime JDKs; the root version does not select the Minecraft compilation environment.

Run commands from the project root. Use `.\gradlew.bat` on Windows and `./gradlew` elsewhere.

| Task | Purpose |
| --- | --- |
| `listTargets` | List targets, Minecraft / Loader, compiler/runtime Java and CI settings |
| `createProject` | Open the project form; refuse existing output directories |
| `addTarget` | Open the target selector; refuse to replace existing targets |
| `setupProject` | Initialize target Wrappers without building Minecraft |
| `select_<target>` | Select the default target and common editing environment |
| `build` | Build the default target |
| `build_<target>` | Build one target |
| `buildAllTargets` | Build all targets |
| `runClient_<target>` | Start a development client |
| `runServer_<target>` | Start a development server |
| `runDatagen_<target>` | Run the target's resource generator |
| `verify_<target>` | Inspect an existing release JAR without rebuilding |
| `verifyAllDistributions` | Inspect all existing release JARs |
| `verifySourceLayout` | Check all existing source models; report stale models, unassigned paths and duplicate Java paths without synchronizing or building |
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

`runClient`, `runServer` and `runDatagen` address the default target. The selection also determines the common editing environment. Reload Gradle models to use that target's Minecraft, Loader and Java classpath for completion. All targets remain independently linked.

During project creation, Default editing target lists the Minecraft / loader combinations already added to the matrix. It controls common on first import and the default root tasks. Removing that target selects the first remaining entry; another entry can be chosen from the dropdown.

With extension 1.6.0 and all Gradle projects synchronized, other compat directories are registered in matching common source-set modules and use those targets' SDKs. Incomplete models retain existing ownership and report the affected targets. Complete models also report paths used by no target. Shared main keeps one editing environment; changing the default does not copy sources.

When a target directory is removed or renamed, the extension detaches its old Gradle project and removes its cached project data and modules. Cleanup applies only to missing target directories directly under `targets`.

The extension links registered JDKs according to each target's `gradle_java_version`. A missing JDK does not stop other targets from linking. Registering it triggers another attempt, while an existing choice with the required Java version is retained.

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
| `temple_default_target` | Root | Default editing target selected during creation; used when no local selection exists |
| `temple_idea_project` | Root | Enable independent target linking in IDEA |
| `temple_build_dsl` | Root | `groovy` or `kotlin`; written by creation forms and used for additional target scripts |
| `loader`, `minecraft_version` | Target | Loader and Minecraft release |
| `loader_version` | Target | Loader version |
| `fabric_api_version` | Fabric target | Fabric API version |
| `java_version` | Target | Java version for compiling game code |
| `gradle_java_version` | Target | Java version for running target Gradle |
| `ci_enabled` | Target | Include the target in CI |
| `datagen_task` | Target, optional | Configured resource-generation task; no datagen alias is registered when absent |

Loader and Minecraft ranges remain in target metadata properties. Each target's `gradle/wrapper/gradle-wrapper.properties` selects its Wrapper version.

The optional target property `forgegradle_version` selects the ForgeGradle plugin. Version 7.x uses the convention for its new API.

`mod_name` is the display name. Gradle project names use `mod_id`, with the target ID appended for target builds. Names, authors and descriptions retain Unicode, quotes and backslashes in UTF-8, with escaping when written to JSON/TOML. Creation requires nonblank single-line fields and checks Java package keywords.

## Sources and resources

Mod development has two primary directories:

```text
common/
├── build.gradle(.kts)      Common module and shared dependencies
└── src/
    ├── main/              Shared Java and resources, grouped by feature
    └── compat/            Adapters for actual API differences
targets/
└── <loader>-<minecraft>/  Independent build, Wrapper and Loader metadata
```

Declare shared libraries with standard `api`, `implementation`, `compileOnly` and `runtimeOnly` scopes in `common/build.gradle(.kts)`. Each target includes a real `:common` module compiled against its own Minecraft/Loader environment. Common classes are merged into that target's release JAR.

Follow the [source directory rules](docs/dev/source-layout.en.md): write shared implementations once, keep only actual API differences in adapters, and classify registration, commands, networking, configuration, storage, screens, rendering and data generation by responsibility. Custom capability layers need explicit target predicates, and every nonempty production path needs at least one target. See [Architecture](docs/dev/architecture.md) for the build model.

Unselected Loader sources are kept as `.template` assets in `gradle/target-blueprints/common` and restored when that Loader is added. They do not remain as unused Java entry points in production directories.

Preserve handwritten resources. Datagen output remains in each target's `src/generated/resources`. Forge/NeoForge receive common and target resources as existing inputs; Fabric uses the corresponding main resource set.

For Forge, `processResources` generates default `pack.mcmeta` only when no handwritten file exists. Formats come from the target SDK's `version.json`. Legacy metadata declares separate Forge client-resource and server-data formats. Formats with minor versions use the server-data range; Forge merges client resources into its mod resource pack. Handwritten files retain their resource-layer priority, extra sections and filters. Supply custom metadata when exposing the mod as a separate resource pack.

New Forge 1.21.5 and later 1.21.x targets use ForgeGradle 7.0.40 with Gradle 9.5.0. Earlier non-baseline Forge targets keep the ForgeGradle 6 path. The nine baseline targets retain their configured plugins and Wrappers. Groovy and Kotlin DSL use the same selection rules.

ForgeGradle 7 uses separate JDKs for its build tools. The current Mavenizer uses Java 25, and official mapping conversion also looks for Java 8. Set `JAVA_HOME_8` to an existing Java 8 installation, or let the official tool provision it. These do not change the target's game compilation version. Proxy settings must also reach the Mavenizer process: Gradle JVM `systemProp.*` settings are not automatically inherited. Its `JAVA_TOOL_OPTIONS` environment variable can enable the Java system proxy.

A migrated project follows the same structure as a new project. Move game implementations into common, retain independent target builds and necessary metadata, and retire old project shells once all consumers have moved. Build controls, IDEA extension code and wizard assets stay out of mod JARs.

## Baseline targets

| Minecraft | Loader |
| --- | --- |
| 1.18.2, 1.19.2 | Forge |
| 1.20.1 | Fabric, Forge |
| 1.21.1 | Fabric, NeoForge |
| 26.1.2 | Fabric, NeoForge |
| 26.2 | NeoForge |

The nine examples do not limit the dropdown catalog. It includes official Minecraft releases from 1.17 within the available build pipelines, with Loader and Fabric API release history. “Recommended” is a label, not a filter. The form can include preview Loader/API releases. New combinations still require matching APIs and build acceptance; selecting a release does not port existing mod code to it.

[Architecture](docs/dev/architecture.md) · [CI](docs/CI_TARGET_DISCOVERY.md) · [Publishing](docs/PUBLISHING.md)

## License

The template is MIT-licensed and retains SighsTemple's copyright notice. Game code, assets and dependencies retain their own licenses. The IDEA extension depends on the installed Minecraft Development plugin and does not redistribute its binaries.

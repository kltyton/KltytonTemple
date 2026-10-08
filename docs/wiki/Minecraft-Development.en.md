# Minecraft Development: installation and project creation

[中文](Minecraft-Development.zh-CN.md) · [Wiki](Home.en.md)

This guide targets IntelliJ IDEA 2026.2.3, Minecraft Development 2026.2-1.8.22 and KltytonTemple 1.1.1. The current ZIP targets IDEA build 262. Other IDEA releases need a matching extension build.

KltytonTemple adds to Minecraft Development. Its existing Forge, Fabric, NeoForge, MultiLoader and Architectury templates remain available.

## 1. Prepare

Install and enable Minecraft Development and Gradle support. Register JDK 21 in IDEA; Minecraft 26.x targets also need JDK 25. Download the KltytonTemple repository. “Template directory” below means that checkout, not the new mod's destination.

The extension source is in `integrations/idea-plugin`. Its build creates:

```text
integrations/idea-plugin/build/distributions/KltytonTemple-IDEA-1.1.1.zip
```

To build it yourself, use the template's Java 25 Wrapper and specify the IDEA installation and Minecraft Development plugin directories:

```sh
./targets/neoforge-26.1.2/gradlew -p integrations/idea-plugin buildPlugin \
  -PideaHome=<IDEA installation> \
  -PminecraftDevHome=<Minecraft Development plugin directory>
```

On Windows, use that directory's `gradlew.bat`. This builds the extension, not a game target.

## 2. Install the extension

1. Open IDEA Settings.
2. Select Plugins, then Installed.
3. Open the gear menu beside the tabs and choose Install Plugin from Disk.
4. Select `KltytonTemple-IDEA-1.1.1.zip` itself. Do not extract it or select Minecraft Development's JAR.
5. Apply the change. If IDEA explicitly requires a restart, save current work before restarting.
6. Search Installed for KltytonTemple and confirm it is enabled.

Minecraft Development is a required dependency. Install or enable it before enabling KltytonTemple.

## 3. Add the template repository

The extension supplies target selection and linking. A template repository supplies the generated files.

1. Search Settings for Minecraft Development and open its page.
2. Locate Project Template Repositories in the project-creation section.
3. Use the list's + action to add a repository.
4. Name it KltytonTemple and select the Local provider.
5. Select `integrations/minecraft-development/templates` inside the template checkout.
6. Apply. Keep the Built In repository.

The selected directory must contain `.mcdev.template.json` and `assets`. For a checkout at `D:/Projects/KltytonTemple`, use:

```text
D:/Projects/KltytonTemple/integrations/minecraft-development/templates
```

Do not select the checkout root, the assets subdirectory or the new mod's destination. If the same Local entry already exists, verify it instead of adding a duplicate.

## 4. Create a project

1. Open File → New → Project.
2. Select Minecraft in the left-hand list. Use the current wizard rather than Minecraft (Old Wizard).
3. Select the Mod group and KltytonTemple template.
4. Fill in project name and location. Keep the new project separate from the template checkout.
5. Fill in mod ID, display name, authors, description, license and build coordinates.
6. Use JDK 21 for the root Gradle control project. Target compiler and Gradle JDKs are maintained independently.

Use lowercase letters, digits and underscores for mod IDs, such as `temple_demo`. Use your own Java namespace, such as `io.github.kltyton.templedemo`; retain no example group or template mod ID.

## 5. Select targets

Wait for the official version catalogs to load.

1. Choose Minecraft from its dropdown.
2. Choose an available Loader for that release.
3. Choose Loader version. Fabric also needs a Fabric API version.
4. The Minecraft Java requirement is displayed automatically.
5. Use Add target to put the combination into the table.
6. For another Loader on the same Minecraft release, change Loader and add another row.
7. Repeat for additional releases. Each row represents one target.
8. Select a row and use Remove selected to remove it. Keep at least one target.

For Fabric and NeoForge on 1.21.1, add `1.21.1 + fabric`, then `1.21.1 + neoforge`. They become independent targets with matching dependency environments.

Refresh versions downloads the catalog again without rewriting rows already selected. The current catalog covers official releases within the blueprint range, starting at Minecraft 1.18.2. Old and newer Forge generations use different build pipelines. The nine baseline examples do not limit the dropdown list.

## 6. Import all Gradle targets

Open the generated project and allow the root Gradle model to load. With `temple_idea_project=true`, the extension links every configured target as an independent Gradle project.

For an existing project, set `temple_idea_project=true` in the root `gradle.properties`. Version 1.1.1 watches both this marker and target configuration changes. Reopen the project after installing or updating the extension to load its current linking listener.

Use Sync All Gradle Projects. After completion, the root and all target projects should be present, with matching source modules and classpaths.

Selecting a default target does not unlink the others. `select_<target>` changes only the root build/runClient aliases.

Each target uses its own Wrapper and Gradle JDK. JDK 21 and JDK 25 targets can coexist; do not replace their configurations with one shared version.

## 7. Develop

See [README](../../README.en.md) for complete task and property mappings. The Gradle tool window and command line invoke the same tasks.

- Build: `build_<target>`.
- Inspect an existing release JAR: `verify_<target>`.
- Client, server and datagen: use the target tasks within the current acceptance scope.
- Add a target: `addTarget`, using the same selection table.
- Publish: platform tasks open a form and keep credentials in the publishing process.

Shared sources still need to match each Minecraft API.

## 8. Troubleshooting

| Symptom | Check |
| --- | --- |
| KltytonTemple is missing from New Project | Extension enabled; Local path directly contains the descriptor |
| Unknown kltyton_targets property | Extension not loaded or mismatched version |
| Version download failed | Network, IDEA proxy and official repositories; Refresh versions |
| A Loader is absent | Official data and blueprint coverage for that release |
| Target already exists | Edit that target or choose another combination instead of overwriting it |
| Only root/buildSrc modules are visible | Marker, extension and linked targets; Sync All |
| One target fails to sync | First error for that target; JDK, Wrapper, repositories and dependencies |
| Generated target does not compile | Matching APIs and build plugins; creation is not cross-version acceptance |

Check actual target modules and their source classpaths after synchronization. Game startup and newly selected version combinations still need their own acceptance.

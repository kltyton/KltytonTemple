# Migrate to KltytonTemple

Give this prompt to an AI that can read the project. Fill in the paths, edit scope and acceptance contract.

```text
Migrate this Minecraft mod project's build and project entry points to KltytonTemple.

Existing project: <absolute path>
KltytonTemple: <absolute path or pinned commit>
Destination: <in place or a new directory>
Allowed edits: <specific files or directories>
Acceptance contract: <targets, tasks, counts and prohibited operations>

Read project rules, Git status, settings/build/gradle.properties, Wrappers,
Loader metadata, source layers, access configuration, datagen, run and
publishing configuration. Confirm every version, Loader, JDK, mapping,
package and mod ID from the files rather than folder names.

Identify official MDKs, MultiLoader, Stonecutter, Architectury, SighsTemple
or a combination. Read integrations/skills/kltyton-temple/SKILL.md from
KltytonTemple, then the migration reference matching the existing build.

Required outcome:
- Preserve every existing target's Wrapper, runtime/compiler JDK, mappings,
  Loader/API and build-plugin versions. Add no unrequested release targets.
- Add native Gradle entry points and independent IDEA target linking.
  Default-target selection affects only root build/runClient aliases.
- Preserve game code, assets, registries, networking, Mixin/refmap,
  AT/AW/ClassTweaker, datagen, optional dependencies, run and publishing paths.
- Prefer existing source paths through shared_sources or existing task outputs.
  Keep Stonecutter preprocessing in each target's real compile pipeline.
  Keep Architectury common transformation, expect/actual and runtime linkage.
- Preserve mod ID, group, package, entry class and copyright. Retain no
  example namespaces, template mod IDs or template publishing project IDs.
- Refuse existing output directories. Preserve unrelated changes, private
  configuration, saves and local dependencies. Make no unrequested backups.
- Finish production changes before running the exact acceptance contract.
  Run no Gradle, tests, datagen or clients during a source-only phase.
- Report the target matrix, path mapping, changed files, actual commands and
  results. Separate source completion, IDE import, builds, release-package
  checks and game acceptance.

For a capability gap, identify its task dependency or call chain and implement
the smallest suitable adaptation. Do not replace it with disabled features,
swallowed failures, removed dependencies or version-string changes alone.
Commits, pushes, PRs, publishing and inter-agent messages require the
authorization supplied for this migration.
```

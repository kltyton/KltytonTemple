# Migration reference

Read only the section matching the existing build. A project can combine these structures.

## Inventory before editing

Record the existing Minecraft/Loader matrix, mod identity, Java and Wrapper pins, plugin and mapping versions, actual source paths, resources and build outputs. Inspect each target's metadata and publishing project IDs. Check dirty work and local rules.

Map original modules and source sets to actual destination files. Preserve all game behavior and resources; migrate their maintenance locations rather than wrapping the old project trees.

## Shared logic first

Start with the active target's sources and shared resources in common/src. Compare all existing variants and read matching Minecraft/Loader sources before assigning a difference to a version or Loader layer.

Extract algorithms, business rules, state and resource handling into one common implementation. Differing imports or a few API calls do not justify copying a whole class. Use small typed SPI boundaries for the actual API/type/lifecycle differences, with concrete implementations in versions/<minecraft>/src, loaders/<loader>/src or target src. Identical file hashes help find immediate merges; they do not identify all shareable logic.

For each SPI extraction, map the original inputs, outputs, side effects and consumers. Rendering boundaries must retain native material attributes, colors, lighting, normals and layer identity. Persistence and networking boundaries retain fields and round-trip semantics. Shared algorithms are complete only when every platform consumer preserves that contract; defaults must not hide discarded data.

Wire every target to the same common sources and the necessary variant implementations. Update resource, access-hook, Mixin, producer and publishing paths. Inspect consumers and remaining duplicate methods before declaring source migration complete. Remove obsolete copied sources and build entry points after their contents and callers are accounted for; preserve user data and required producer pipelines.

## Official Loader templates

An official MDK usually has one Loader and one Minecraft environment. Keep its plugin DSL, dependency configuration, run tasks, metadata and access hooks. Place its build entry under targets/<loader>-<minecraft> or adapt the existing module as a target; update relative paths without rewriting the MDK's semantics.

Do not replace ForgeGradle with ModDevGradle across unsupported generations. The legacy ModDevGradle plugin supports Minecraft 1.17–1.20.1; newer Forge targets need their matching ForgeGradle pipeline. NeoForge and Fabric require their own build plugins and metadata.

## MultiLoader

Inspect common sources and resources, loader entry points, source-set wiring, transformed artifacts and dependency scopes. Preserve the per-loader compile environment.

Migrate shared game logic into common and isolate Loader API differences in their source layers. shared_sources must reference those migrated layers. Where the build consumes a transformed or shaded common artifact, retain its producer and consumer dependency rather than replacing it with file copies.

## Stonecutter

Find stonecutter.gradle(.kts), settings/controller configuration, version nodes, constants, dependency swaps and preprocessing directives. Determine the exact preprocessing tasks from the current project; do not invent task names.

Keep preprocessing before each target's compilation and keep generated-source paths distinct from handwritten sources. A generated directory listed in shared_sources without a producer dependency is incomplete.

When the controller owns generation, keep it as the producer. Adapt target entry points to request the right variant and consume its output. Preserve version-specific switches, active-version editor support and publishing variants. Remove the old controller only when equivalent behavior has been implemented and accepted.

## Architectury

Inspect common/platform modules, architectury.common.json, platform declarations, Loom settings, common configurations, transformProduction tasks, remapping and expect/actual implementations.

Keep the common transformation and platform artifact relationships intact. Plain shared-source compilation cannot replace Architectury transformations by itself. Preserve Architectury API and platform runtime dependencies where the original code uses them.

## SighsTemple / earlier KltytonTemple

Keep the existing target matrix, source precedence, metadata and Wrapper/Java pins. Replace obsolete command entry points with native Gradle tasks. Private machine configuration can be retired only after its required JDK/path information is represented by the new entry points.

A prior one-target composite import must not remain the sole IDE model. Link the independent targets so Sync All includes the full matrix. Root default-target selection is a task convenience only.

## Acceptance and cleanup

Implement the authorized production batch first. Under source-only, stop at source/file review and report unverified behavior; do not run Gradle or clients.

Under an authorized build contract, execute only its exact targets, order and counts. Inspect real release packages where packaging changed. A successful build is not gameplay or visual acceptance.

Clean only files made obsolete by this migration. Keep licenses, handwritten assets, local dependencies and user data. Inspect the staged/public file list before delivery; public migration prompts and this reusable Skill are intentional product files, not private session records.

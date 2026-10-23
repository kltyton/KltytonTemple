package io.github.kltyton.temple.idea.model;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VfsUtilCore;
import io.github.kltyton.temple.ui.UiText;
import io.github.kltyton.temple.workspace.sources.SourceLayout;
import io.github.kltyton.temple.workspace.sources.SourceLayout.SourceRoot;
import io.github.kltyton.temple.workspace.sources.SourceLayout.Target;
import io.github.kltyton.temple.workspace.sources.SourceLayout.Snapshot;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class CompatSourceModel {
    private static final Logger LOG = Logger.getInstance(CompatSourceModel.class);
    private static final Gson JSON = new Gson();
    private static final String STATE_KEY = "kltytonTemple.compatSourceOwnership";
    private final Project project;
    private final AtomicBoolean pending = new AtomicBoolean();
    private final AtomicInteger revision = new AtomicInteger();
    private List<String> lastProblems = List.of();
    private Notification notification;
    private record Binding(Target target, Module module) {}
    private record Managed(List<String> roots, List<String> dependencies) {}

    public CompatSourceModel(Project project) {
        this.project = project;
    }

    public void refresh() {
        revision.incrementAndGet();
        if (project.isDisposed() || !pending.compareAndSet(false, true)) return;
        int requested = revision.get();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Snapshot snapshot = readModels();
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        if (!project.isDisposed() && snapshot != null && revision.get() == requested)
                            WriteAction.run(() -> apply(snapshot));
                    } catch (IllegalArgumentException | JsonParseException error) {
                        LOG.warn("Could not assign KltytonTemple compatibility source roots", error);
                        report(List.of(UiText.system().get("sourceAssignmentFailed")));
                    } finally { finished(requested); }
                });
            } catch (IOException | IllegalArgumentException | JsonParseException error) {
                LOG.warn("Could not read KltytonTemple source models; synchronize the affected Gradle projects", error);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!project.isDisposed()) report(List.of(UiText.system().get("sourceReadFailed")));
                });
                finished(requested);
            }
        });
    }

    private void finished(int requested) {
        pending.set(false);
        if (!project.isDisposed() && revision.get() != requested) refresh();
    }

    private Snapshot readModels() throws IOException {
        String base = project.getBasePath();
        if (base == null) return null;
        Path root = Path.of(base);
        if (!Files.isRegularFile(root.resolve("gradle.properties"))) return null;
        return SourceLayout.read(root, file -> {
            try (Reader reader = Files.newBufferedReader(file)) {
                try { return JSON.fromJson(reader, SourceLayout.Descriptor.class); }
                catch (JsonParseException error) { throw new IllegalArgumentException("Invalid source model: " + file, error); }
            }
        });
    }

    private void apply(Snapshot snapshot) {
        UiText text = UiText.system();
        List<String> problems = new ArrayList<>();
        for (SourceLayout.Problem problem : snapshot.problems())
            problems.add(text.get(problem.code()) + ": " + problem.detail());
        if (!snapshot.modelsComplete()) { report(problems); return; }
        Module[] modules = ModuleManager.getInstance(project).getModules();
        List<Binding> bindings = new ArrayList<>();
        for (Target target : snapshot.targets()) {
            Module main = null, merged = null;
            for (Module module : modules) {
                var properties = ExternalSystemModulePropertyManager.getInstance(module);
                if (!samePath(properties.getRootProjectPath(), target.targetDirectory())
                        || !samePath(properties.getLinkedProjectPath(), target.projectDirectory())) continue;
                String id = properties.getLinkedProjectId();
                if (id != null && id.endsWith(":main")) main = module;
                else if (id != null && id.endsWith(":common")) merged = module;
            }
            if (main != null || merged != null) bindings.add(new Binding(target, main != null ? main : merged));
            else problems.add(text.get("sourceModuleMissing") + ": " + target.id());
        }
        if (!problems.isEmpty()) { report(problems); return; }
        for (String path : snapshot.unassigned()) problems.add(text.get("sourceUnassigned") + ": " + path);
        report(problems);

        Map<Path, Binding> owners = new LinkedHashMap<>();
        for (Binding binding : bindings)
            for (SourceRoot source : binding.target().roots()) owners.putIfAbsent(source.path(), binding);
        Map<Module, Set<String>> desiredRoots = new LinkedHashMap<>(), desiredDependencies = new LinkedHashMap<>();
        for (Binding binding : bindings) {
            Set<String> roots = new LinkedHashSet<>(), dependencies = new LinkedHashSet<>();
            for (SourceRoot source : binding.target().roots()) {
                Binding owner = owners.get(source.path());
                if (owner.module() == binding.module()) roots.add(sourceUrl(source.path()));
                else dependencies.add(owner.module().getName());
            }
            desiredRoots.put(binding.module(), roots);
            desiredDependencies.put(binding.module(), dependencies);
        }
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String saved = properties.getValue(STATE_KEY);
        Map<String, Managed> previous = saved == null ? Map.of() : JSON.fromJson(saved,
                new TypeToken<Map<String, Managed>>() {}.getType());
        if (previous == null || previous.values().stream().anyMatch(value ->
                value == null || value.roots() == null || value.dependencies() == null))
            throw new IllegalArgumentException("Invalid saved compatibility source ownership");

        // Release previously assigned roots before transferring them to another module.
        for (Module module : modules) {
            Managed managed = previous.get(module.getName());
            if (managed == null) continue;
            var external = ExternalSystemModulePropertyManager.getInstance(module);
            String linked = external.getLinkedProjectPath();
            if (linked == null || !Path.of(linked).toAbsolutePath().normalize().startsWith(snapshot.common())) continue;
            ModuleRootModificationUtil.modifyModel(module, model -> {
                for (ContentEntry entry : model.getContentEntries())
                    if (managed.roots().contains(entry.getUrl()) && managedUrl(entry.getUrl(), snapshot.common())
                            && !desiredRoots.getOrDefault(module, Set.of()).contains(entry.getUrl()))
                        model.removeContentEntry(entry);
                for (OrderEntry entry : model.getOrderEntries())
                    if (entry instanceof ModuleOrderEntry dependency && managed.dependencies().contains(dependency.getModuleName())
                            && !desiredDependencies.getOrDefault(module, Set.of()).contains(dependency.getModuleName()))
                        model.removeOrderEntry(entry);
                return model.isChanged();
            });
        }

        Map<String, Managed> updated = new LinkedHashMap<>();
        for (Binding binding : bindings) {
            List<String> roots = new ArrayList<>(), dependencies = new ArrayList<>();
            Managed old = previous.getOrDefault(binding.module().getName(), new Managed(List.of(), List.of()));
            ModuleRootModificationUtil.modifyModel(binding.module(), model -> {
                Set<Module> needed = new LinkedHashSet<>();
                for (SourceRoot source : binding.target().roots()) {
                    Binding owner = owners.get(source.path());
                    if (owner.module() != binding.module()) {
                        needed.add(owner.module());
                        continue;
                    }
                    String url = sourceUrl(source.path());
                    boolean registered = Arrays.stream(model.getContentEntries()).flatMap(entry -> Arrays.stream(entry.getSourceFolders()))
                            .anyMatch(folder -> folder.getUrl().equals(url));
                    if (!registered) {
                        ContentEntry entry = model.addContentEntry(url);
                        if (source.resource()) entry.addSourceFolder(url, JavaResourceRootType.RESOURCE);
                        else entry.addSourceFolder(url, JavaSourceRootType.SOURCE);
                        roots.add(url);
                    } else if (old.roots().contains(url) && Arrays.stream(model.getContentEntries())
                            .anyMatch(entry -> entry.getUrl().equals(url))) {
                        roots.add(url);
                    }
                }
                for (Module dependency : needed) {
                    ModuleOrderEntry entry = model.findModuleOrderEntry(dependency);
                    if (entry != null && !old.dependencies().contains(dependency.getName())) continue;
                    if (entry == null) entry = model.addModuleOrderEntry(dependency);
                    entry.setScope(DependencyScope.COMPILE);
                    entry.setExported(true);
                    dependencies.add(dependency.getName());
                }
                return model.isChanged();
            });
            updated.put(binding.module().getName(), new Managed(roots, dependencies));
        }
        properties.setValue(STATE_KEY, JSON.toJson(updated));
    }

    private void report(List<String> problems) {
        if (lastProblems.equals(problems)) return;
        lastProblems = List.copyOf(problems);
        if (notification != null) { notification.expire(); notification = null; }
        if (problems.isEmpty()) return;
        UiText text = UiText.system();
        String details = String.join("\n", problems);
        LOG.warn("KltytonTemple source layout:\n" + details);
        String body = problems.stream().limit(20).map(value -> value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")).reduce((left, right) -> left + "<br>" + right).orElse("");
        if (problems.size() > 20) body += "<br>" + text.get("sourceMore") + ": " + problems.size();
        notification = new Notification("KltytonTemple Source Layout", text.get("sourceLayoutTitle"),
                body + "<br><br>" + text.get("sourceLayoutHint"), NotificationType.WARNING);
        notification.notify(project);
    }

    private static boolean samePath(String value, Path expected) {
        return value != null && Path.of(value).toAbsolutePath().normalize().equals(expected);
    }

    private static boolean managedUrl(String url, Path common) {
        String src = sourceUrl(common.resolve("src")) + '/';
        String build = sourceUrl(common.resolve("build")) + '/';
        return url.startsWith(src) || url.startsWith(build);
    }

    private static String sourceUrl(Path path) {
        return VfsUtilCore.pathToUrl(path.toString().replace('\\', '/'));
    }
}

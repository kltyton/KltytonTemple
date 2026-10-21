package io.github.kltyton.temple.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkException;
import com.intellij.openapi.externalSystem.action.DetachExternalProjectAction;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import io.github.kltyton.temple.idea.model.CompatSourceModel;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.lang.JavaVersion;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.util.GradleConstants;

public final class TempleProjectActivity implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance(TempleProjectActivity.class);

    @Override
    public void runActivity(Project project) {
        CompatSourceModel sources = new CompatSourceModel(project);
        sources.refresh();
        project.getMessageBus().connect(project).subscribe(ProjectDataImportListener.TOPIC, new ProjectDataImportListener() {
            @Override public void onImportFinished(String projectPath) { sources.refresh(); }
        });
        ApplicationManager.getApplication().getMessageBus().connect(project)
                .subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                String root = project.getBasePath();
                if (root != null && events.stream().anyMatch(event ->
                        event.getPath().equals(root + "/.temple/active-target") ||
                                (event.getPath().startsWith(root + "/common/.gradle/contexts/") &&
                                        event.getPath().endsWith("/ide-sources.json")))) sources.refresh();
                if (root != null && events.stream().anyMatch(event ->
                        event.getPath().equals(root + "/gradle.properties") ||
                        (event.getPath().startsWith(root + "/targets/") &&
                                (event.getPath().endsWith("/gradle.properties") ||
                                        event.getPath().substring((root + "/targets/").length()).indexOf('/') < 0))))
                    ApplicationManager.getApplication().invokeLater(() -> linkTargets(project));
            }
        });
        ApplicationManager.getApplication().getMessageBus().connect(project)
                .subscribe(ProjectJdkTable.JDK_TABLE_TOPIC, new ProjectJdkTable.Listener() {
            @Override public void jdkAdded(Sdk sdk) { scheduleLink(project); }
            @Override public void jdkRemoved(Sdk sdk) { scheduleLink(project); }
            @Override public void jdkNameChanged(Sdk sdk, String previousName) { scheduleLink(project); }
        });
        ExternalProjectsManager.getInstance(project).runWhenInitialized(() -> linkTargets(project));
    }

    private static void scheduleLink(Project project) {
        ApplicationManager.getApplication().invokeLater(() -> linkTargets(project));
    }

    public static void linkTargets(Project project) {
        if (project.isDisposed() || project.getBasePath() == null) return;
        Path root = Path.of(project.getBasePath());
        try {
            Properties identity = read(root.resolve("gradle.properties"));
            if (!"true".equals(identity.getProperty("temple_idea_project"))) return;
            GradleSettings settings = GradleSettings.getInstance(project);
            List<Path> directories;
            try (var paths = Files.list(root.resolve("targets"))) {
                directories = paths.filter(Files::isDirectory).sorted().toList();
            }
            Path targetsRoot = root.resolve("targets").toAbsolutePath().normalize();
            removeMissingTargets(project, targetsRoot, settings);
            for (Path directory : directories) {
                if (!Files.isRegularFile(directory.resolve("gradle.properties"))) continue;
                Properties target;
                int java;
                try {
                    target = read(directory.resolve("gradle.properties"));
                    java = Integer.parseInt(target.getProperty("gradle_java_version"));
                } catch (IOException | IllegalArgumentException error) {
                    LOG.warn("Could not read Gradle JDK requirement for " + directory, error);
                    continue;
                }
                String path = directory.toString().replace('\\', '/');
                GradleProjectSettings linked = settings.getLinkedProjectSettings(path);
                boolean added = linked == null;
                if (!added) {
                    try {
                        Sdk current = ExternalSystemJdkUtil.getJdk(project, linked.getGradleJvm());
                        if (matches(current, java)) continue;
                    } catch (ExternalSystemJdkException error) {
                        LOG.warn("Could not resolve Gradle JDK for " + directory, error);
                    }
                }
                Sdk selected = null;
                for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
                    if (matches(sdk, java)) {
                        selected = sdk;
                        break;
                    }
                }
                if (selected == null) {
                    LOG.warn("Register JDK " + java + " in IDEA for " + directory.getFileName());
                    continue;
                }
                if (added) {
                    linked = new GradleProjectSettings();
                    linked.setExternalProjectPath(path);
                    linked.setDistributionType(DistributionType.DEFAULT_WRAPPED);
                }
                linked.setGradleJvm(selected.getName());
                if (added) settings.linkProject(linked);
            }
        } catch (IOException | IllegalArgumentException error) {
            LOG.warn("Could not link KltytonTemple targets at " + root, error);
        }
    }

    private static void removeMissingTargets(Project project, Path targetsRoot, GradleSettings settings) {
        Map<String, ExternalProjectInfo> projects = new LinkedHashMap<>();
        for (GradleProjectSettings linked : settings.getLinkedProjectsSettings()) {
            projects.put(linked.getExternalProjectPath(), null);
        }
        for (var cached : ExternalProjectsDataStorage.getInstance(project).list(GradleConstants.SYSTEM_ID)) {
            projects.put(cached.getExternalProjectPath(), cached);
        }
        for (var entry : projects.entrySet()) {
            String path = entry.getKey();
            Path directory = Path.of(path).toAbsolutePath().normalize();
            if (!targetsRoot.equals(directory.getParent()) || !Files.notExists(directory)) continue;
            ExternalProjectInfo cached = entry.getValue();
            if (cached != null && cached.getExternalProjectStructure() != null) {
                DetachExternalProjectAction.detachProject(project, GradleConstants.SYSTEM_ID,
                        cached.getExternalProjectStructure().getData(), null);
            } else {
                ExternalSystemApiUtil.getLocalSettings(project, GradleConstants.SYSTEM_ID)
                        .forgetExternalProjects(Collections.singleton(path));
                settings.unlinkExternalProject(path);
                ExternalProjectsManagerImpl.getInstance(project)
                        .forgetExternalProjectData(GradleConstants.SYSTEM_ID, path);
            }
        }
    }

    private static boolean matches(Sdk sdk, int feature) {
        if (sdk == null) return false;
        JavaVersion version = JavaVersion.tryParse(sdk.getVersionString());
        return version != null && version.feature == feature;
    }

    private static Properties read(Path path) throws IOException {
        Properties properties = new Properties();
        if (!Files.isRegularFile(path)) return properties;
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        return properties;
    }
}

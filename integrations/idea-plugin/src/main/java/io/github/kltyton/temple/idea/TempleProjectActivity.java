package io.github.kltyton.temple.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.startup.StartupActivity;
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

public final class TempleProjectActivity implements StartupActivity.DumbAware {
    private static final Logger LOG = Logger.getInstance(TempleProjectActivity.class);

    @Override
    public void runActivity(Project project) {
        linkTargets(project);
        project.getMessageBus().connect(project).subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                String root = project.getBasePath();
                if (root != null && events.stream().anyMatch(event -> event.getPath().startsWith(root + "/targets/") && event.getPath().endsWith("/gradle.properties")))
                    ApplicationManager.getApplication().invokeLater(() -> linkTargets(project));
            }
        });
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
            for (Path directory : directories) {
                if (!Files.isRegularFile(directory.resolve("gradle.properties"))) continue;
                Properties target = read(directory.resolve("gradle.properties"));
                int java = Integer.parseInt(target.getProperty("gradle_java_version"));
                String path = directory.toString().replace('\\', '/');
                if (settings.getLinkedProjectSettings(path) != null) continue;
                GradleProjectSettings linked = new GradleProjectSettings();
                linked.setExternalProjectPath(path);
                linked.setDistributionType(DistributionType.DEFAULT_WRAPPED);
                for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
                    JavaVersion parsed = JavaVersion.tryParse(sdk.getVersionString());
                    if (parsed != null && parsed.feature == java) {
                        linked.setGradleJvm(sdk.getName());
                        break;
                    }
                }
                if (linked.getGradleJvm() == null)
                    throw new IllegalStateException("Register JDK " + java + " in IDEA before linking " + directory.getFileName());
                settings.linkProject(linked);
            }
        } catch (IOException | IllegalArgumentException error) {
            LOG.warn("Could not link KltytonTemple targets at " + root, error);
        }
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

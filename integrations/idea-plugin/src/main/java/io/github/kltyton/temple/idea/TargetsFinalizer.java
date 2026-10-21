package io.github.kltyton.temple.idea;

import com.demonwav.mcdev.creator.custom.finalizers.CreatorFinalizer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.Project;
import io.github.kltyton.temple.workspace.TargetPlan;
import io.github.kltyton.temple.workspace.BuildScripts;
import io.github.kltyton.temple.workspace.ProjectIdentity;
import io.github.kltyton.temple.idea.project.WizardIdentity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

import kotlin.Unit;
import kotlin.coroutines.Continuation;

public final class TargetsFinalizer implements CreatorFinalizer {
    @Override
    public Object execute(WizardContext context, Project project, Map<String, ? extends Object> properties,
                          Map<String, ? extends Object> templateProperties, Continuation<? super Unit> continuation) {
        List<Map<String, String>> selections = new Gson().fromJson(templateProperties.get("TARGET_PLAN").toString(),
                new TypeToken<List<Map<String, String>>>() {
                }.getType());
        try {
            Path root = Path.of(context.getProjectFileDirectory());
            Properties identity = WizardIdentity.collect(templateProperties::get);
            ProjectIdentity.validate(identity);
            identity.setProperty("org.gradle.jvmargs", "-Xmx3G -Dfile.encoding=UTF-8");
            identity.setProperty("org.gradle.daemon", "false");
            identity.setProperty("temple_idea_project", "true");
            try (var writer = Files.newBufferedWriter(root.resolve("gradle.properties"), StandardCharsets.UTF_8)) {
                identity.store(writer, null);
            }
            Object defaultTarget = templateProperties.get("DEFAULT_TARGET");
            TargetPlan.apply(root, selections, true,
                    defaultTarget == null ? null : defaultTarget.toString());
            BuildScripts.createProject(root);
        } catch (IOException error) {
            throw new IllegalStateException("Could not create selected targets", error);
        }
        TempleProjectActivity.linkTargets(project);
        return Unit.INSTANCE;
    }
}

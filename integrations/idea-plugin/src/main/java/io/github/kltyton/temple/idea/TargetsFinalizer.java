package io.github.kltyton.temple.idea;

import com.demonwav.mcdev.creator.custom.finalizers.CreatorFinalizer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.Project;
import io.github.kltyton.temple.workspace.TargetPlan;

import java.io.IOException;
import java.nio.file.Path;
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
            TargetPlan.apply(Path.of(context.getProjectFileDirectory()), selections, true);
        } catch (IOException error) {
            throw new IllegalStateException("Could not create selected targets", error);
        }
        TempleProjectActivity.linkTargets(project);
        return Unit.INSTANCE;
    }
}

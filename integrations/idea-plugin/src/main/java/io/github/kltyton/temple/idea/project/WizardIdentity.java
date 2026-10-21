package io.github.kltyton.temple.idea.project;

import com.demonwav.mcdev.creator.custom.model.BuildSystemCoordinates;
import io.github.kltyton.temple.workspace.BuildScripts;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public final class WizardIdentity {
    private WizardIdentity() {}

    public static Properties collect(Function<String, ?> value) {
        Properties identity = new Properties();
        Map.of("mod_id", "MOD_ID", "mod_name", "MOD_NAME", "mod_authors", "AUTHORS",
                "mod_license", "LICENSE", "mod_description", "DESCRIPTION").forEach((key, name) -> {
            Object raw = value.apply(name);
            if (!(raw instanceof String text))
                throw new IllegalArgumentException("Missing project property: " + name);
            identity.setProperty(key, text);
        });
        if (!(value.apply("BUILD_COORDS") instanceof BuildSystemCoordinates coordinates))
            throw new IllegalArgumentException("Missing build coordinates");
        identity.setProperty("mod_group_id", coordinates.getGroupId());
        identity.setProperty("mod_version", coordinates.getVersion());
        identity.setProperty("temple_build_dsl", BuildScripts.Dsl.parse((String) value.apply("BUILD_DSL")).id);
        return identity;
    }
}


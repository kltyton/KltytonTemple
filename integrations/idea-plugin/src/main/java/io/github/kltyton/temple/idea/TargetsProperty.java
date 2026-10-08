package io.github.kltyton.temple.idea;

import com.demonwav.mcdev.creator.custom.CreatorContext;
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor;
import com.demonwav.mcdev.creator.custom.types.CreatorProperty;
import com.demonwav.mcdev.creator.custom.types.CreatorPropertyFactory;
import com.google.gson.Gson;
import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.dsl.builder.Align;
import com.intellij.ui.dsl.builder.Panel;
import io.github.kltyton.temple.ui.TargetSelectionPanel;

import java.util.*;

import kotlin.Unit;

public final class TargetsProperty extends CreatorProperty<String> {
    private final GraphProperty<String> value;

    public TargetsProperty(TemplatePropertyDescriptor descriptor, CreatorContext context) {
        super(descriptor, context, String.class);
        value = context.getGraph().property("[]");
    }

    @Override
    public GraphProperty<String> getGraphProperty() {
        return value;
    }

    @Override
    public String createDefaultValue(Object raw) {
        return "[]";
    }

    @Override
    public String serialize(String input) {
        return input;
    }

    @Override
    public String deserialize(String input) {
        return input;
    }

    @Override
    public void buildUi(Panel panel) {
        Gson json = new Gson();
        TargetSelectionPanel selection = new TargetSelectionPanel(text -> json.fromJson(text, Object.class), List.of(),
                targets -> value.set(json.toJson(targets)));
        panel.row("", row -> {
            row.cell(selection).resizableColumn().align(Align.FILL)
                    .validationRequestor((kotlin.jvm.functions.Function0<Unit> requestor) -> {
                        value.afterPropagation(requestor);
                        return Unit.INSTANCE;
                    })
                    .validationOnApply((builder, component) -> selection.selections().isEmpty() ? new ValidationInfo("Select at least one target", selection) : null);
            return Unit.INSTANCE;
        });
    }

    public static final class Factory implements CreatorPropertyFactory {
        @Override
        public CreatorProperty<?> create(TemplatePropertyDescriptor descriptor, CreatorContext context) {
            return new TargetsProperty(descriptor, context);
        }
    }
}

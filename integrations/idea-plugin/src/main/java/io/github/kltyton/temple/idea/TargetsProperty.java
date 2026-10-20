package io.github.kltyton.temple.idea;

import com.demonwav.mcdev.creator.custom.CreatorContext;
import com.demonwav.mcdev.creator.custom.BuiltinValidations;
import com.demonwav.mcdev.creator.custom.TemplatePropertyDescriptor;
import com.demonwav.mcdev.creator.custom.types.CreatorProperty;
import com.demonwav.mcdev.creator.custom.types.CreatorPropertyFactory;
import com.google.gson.Gson;
import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.dsl.builder.Align;
import com.intellij.ui.dsl.builder.BuilderKt;
import com.intellij.ui.dsl.builder.Cell;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.ui.dsl.builder.TextFieldKt;
import com.intellij.util.ui.JBUI;
import io.github.kltyton.temple.ui.TargetSelectionPanel;

import java.util.*;
import java.awt.Dimension;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import javax.swing.*;

import kotlin.Unit;
import kotlin.text.Regex;

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
        CreatorProperty<?> defaultTarget = getProperties().get("DEFAULT_TARGET");
        if (defaultTarget != null) {
            @SuppressWarnings("unchecked")
            CreatorProperty<String> targetProperty = (CreatorProperty<String>) defaultTarget;
            selection.enableDefaultTargetSelection(target -> targetProperty.getGraphProperty().set(target));
        }
        JComponent form = getDescriptor().getParameters() != null
                && "split".equals(getDescriptor().getParameters().get("layout")) ? splitForm(selection) : selection;
        panel.row("", row -> {
            row.cell(form).resizableColumn().align(Align.FILL)
                    .validationRequestor((kotlin.jvm.functions.Function0<Unit> requestor) -> {
                        value.afterPropagation(requestor);
                        return Unit.INSTANCE;
                    })
                    .validationOnApply((builder, component) -> selection.selections().isEmpty() ? new ValidationInfo(selection.messages().get("selectTarget"), selection.targetsPanel()) : null);
            return Unit.INSTANCE;
        }).resizableRow();
    }

    private JComponent splitForm(TargetSelectionPanel selection) {
        DialogPanel configuration = BuilderKt.panel(left -> {
            stringField(left, "MOD_ID", new JLabel("Mod ID"));
            stringField(left, "MOD_NAME", fieldLabel(selection, "name"));
            selection.configurationRows((label, component) -> left.row(label, row -> {
                row.cell(component).resizableColumn().align(Align.FILL);
                return Unit.INSTANCE;
            }));
            stringField(left, "AUTHORS", fieldLabel(selection, "authors"));
            CreatorProperty<String> description = getContext().property("DESCRIPTION");
            JTextArea descriptionText = new JTextArea(3, 24);
            descriptionText.setLineWrap(true);
            descriptionText.setWrapStyleWord(true);
            left.row(fieldLabel(selection, "description"), row -> {
                Cell<JTextArea> field = TextFieldKt.bindText(row.scrollCell(descriptionText).resizableColumn().align(Align.FILL),
                        description.toStringProperty(description.getGraphProperty()));
                field.validationOnInput((builder, component) -> descriptionValidation(selection, component));
                field.validationOnApply((builder, component) -> descriptionValidation(selection, component));
                return Unit.INSTANCE;
            });
            licenseField(left, fieldLabel(selection, "license"));
            ((BuildDslProperty) getProperties().get("BUILD_DSL")).buildUi(left, fieldLabel(selection, "buildDsl"));
            getProperties().get("BUILD_COORDS").buildUi(left);
            getProperties().get("JDK").buildUi(left);
            return Unit.INSTANCE;
        });
        JBScrollPane configurationScroll = new JBScrollPane(configuration,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        configurationScroll.setBorder(JBUI.Borders.empty());
        configurationScroll.getVerticalScrollBar().setUnitIncrement(JBUI.scale(16));
        configurationScroll.setMinimumSize(JBUI.size(280, 120));
        selection.targetsPanel().setMinimumSize(JBUI.size(350, 120));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configurationScroll, selection.targetsPanel()) {
            private boolean dividerInitialized;
            private JScrollPane wizardScroll;
            private int verticalScrollPolicy;
            private int horizontalScrollPolicy;
            private boolean wheelScrollingEnabled;

            @Override public void addNotify() {
                super.addNotify();
                JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
                if (viewport != null && viewport.getParent() instanceof JScrollPane scroll) {
                    wizardScroll = scroll;
                    verticalScrollPolicy = scroll.getVerticalScrollBarPolicy();
                    horizontalScrollPolicy = scroll.getHorizontalScrollBarPolicy();
                    wheelScrollingEnabled = scroll.isWheelScrollingEnabled();
                    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    scroll.setWheelScrollingEnabled(false);
                    scroll.getVerticalScrollBar().setValue(0);
                    scroll.getHorizontalScrollBar().setValue(0);
                    revalidate();
                }
            }

            @Override public void removeNotify() {
                if (wizardScroll != null) {
                    wizardScroll.setVerticalScrollBarPolicy(verticalScrollPolicy);
                    wizardScroll.setHorizontalScrollBarPolicy(horizontalScrollPolicy);
                    wizardScroll.setWheelScrollingEnabled(wheelScrollingEnabled);
                    wizardScroll = null;
                }
                super.removeNotify();
            }

            @Override public Dimension getPreferredSize() {
                Dimension preferred = new Dimension(super.getPreferredSize());
                JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
                if (viewport != null && viewport.getExtentSize().height > 0) {
                    int offset = SwingUtilities.convertPoint(this, 0, 0, viewport.getView()).y;
                    preferred.height = Math.min(preferred.height, Math.max(getMinimumSize().height,
                            viewport.getExtentSize().height - offset - JBUI.scale(12)));
                    preferred.width = Math.min(preferred.width, viewport.getExtentSize().width);
                }
                return preferred;
            }

            @Override public void doLayout() {
                if (!dividerInitialized && getWidth() > 0) {
                    setDividerLocation(0.5);
                    dividerInitialized = true;
                }
                super.doLayout();
            }
        };
        split.setBorder(JBUI.Borders.empty());
        split.setContinuousLayout(true);
        split.setResizeWeight(0.5);
        split.setPreferredSize(JBUI.size(1040, 420));
        split.addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override public void ancestorMoved(HierarchyEvent event) { split.revalidate(); }
            @Override public void ancestorResized(HierarchyEvent event) { split.revalidate(); }
        });
        DialogPanel[] owner = new DialogPanel[1];
        split.addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) == 0) return;
            DialogPanel parent = (DialogPanel) SwingUtilities.getAncestorOfClass(DialogPanel.class, split);
            if (parent == owner[0]) return;
            if (owner[0] != null) owner[0].unregisterIntegratedPanel(configuration);
            owner[0] = parent;
            if (parent != null) parent.registerIntegratedPanel(configuration);
            split.revalidate();
        });
        return split;
    }

    private JLabel fieldLabel(TargetSelectionPanel selection, String key) {
        JLabel label = new JLabel(selection.messages().get(key));
        selection.addPropertyChangeListener("language", event -> label.setText(selection.messages().get(key)));
        return label;
    }

    private ValidationInfo descriptionValidation(TargetSelectionPanel selection, JTextArea component) {
        String input = component.getText();
        return input.indexOf('\n') >= 0 || input.indexOf('\r') >= 0
                ? new ValidationInfo(selection.messages().get("singleLineDescription"), component) : null;
    }

    private void stringField(Panel panel, String name, JLabel label) {
        CreatorProperty<String> property = getContext().property(name);
        panel.row(label, row -> {
            Cell<JTextField> field = TextFieldKt.bindText(row.cell(new JTextField(24))
                            .resizableColumn().align(Align.FILL),
                    property.toStringProperty(property.getGraphProperty()));
            if (property.getDescriptor().getValidator() instanceof String validator)
                TextFieldKt.textValidation(field, BuiltinValidations.INSTANCE.byRegex(new Regex(validator)));
            return Unit.INSTANCE;
        });
    }

    private void licenseField(Panel panel, JLabel label) {
        CreatorProperty<String> property = getContext().property("LICENSE");
        JComboBox<String> license = new JComboBox<>(new String[]{"MIT", "Apache-2.0", "BSD-2-Clause",
                "BSD-3-Clause", "GPL-3.0-only", "LGPL-3.0-only", "MPL-2.0", "Unlicense", "CC0-1.0",
                "All Rights Reserved"});
        license.setEditable(true);
        license.setSelectedItem(property.get());
        panel.row(label, row -> {
            row.cell(license).resizableColumn().align(Align.FILL)
                    .onApply(() -> {
                        property.getGraphProperty().set(license.getEditor().getItem().toString());
                        return Unit.INSTANCE;
                    })
                    .onReset(() -> {
                        license.setSelectedItem(property.get());
                        return Unit.INSTANCE;
                    })
                    .onIsModified(() -> !Objects.equals(property.get(), license.getEditor().getItem()));
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

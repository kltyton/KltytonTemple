package io.github.kltyton.temple.ui;

import io.github.kltyton.temple.catalog.VersionCatalog;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public final class TargetSelectionPanel extends JPanel {
    private final JComboBox<String> minecraft = new JComboBox<>(), loader = new JComboBox<>(), version = new JComboBox<>(), api = new JComboBox<>();
    private final JLabel status = new JLabel("Loading official version catalogs…"), toolchain = new JLabel("Java / Gradle");
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Minecraft", "Loader", "Loader version", "Fabric API", "Java"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final List<Map<String, String>> selected = new ArrayList<>();
    private final Consumer<List<Map<String, String>>> changed;
    private VersionCatalog catalog;
    private int javaVersion;
    private final JButton add = new JButton("Add target"), refresh = new JButton("Refresh versions");

    public TargetSelectionPanel(Function<String, Object> parser, List<Map<String, String>> initial, Consumer<List<Map<String, String>>> changed) {
        super(new BorderLayout(0, 12));
        this.changed = changed;
        JPanel selectors = new JPanel(new GridLayout(0, 2, 12, 8));
        for (Object[] pair : new Object[][]{{"Minecraft", minecraft}, {"Loader", loader}, {"Loader version", version}, {"Fabric API", api}, {"Toolchain", toolchain}}) {
            selectors.add(new JLabel(pair[0].toString()));
            selectors.add((Component) pair[1]);
        }
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(add);
        buttons.add(refresh);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(selectors, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        JTable table = new JTable(model);
        table.setRowHeight(Math.max(26, table.getFont().getSize() + 12));
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(660, 190));
        add(scroll, BorderLayout.CENTER);
        JButton remove = new JButton("Remove selected");
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.add(status, BorderLayout.CENTER);
        bottom.add(remove, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        initial.forEach(row -> {
            selected.add(new LinkedHashMap<>(row));
            append(row);
        });
        minecraft.setEditable(false);
        loader.setEditable(false);
        version.setEditable(false);
        api.setEditable(false);
        minecraft.addActionListener(event -> loadJava());
        loader.addActionListener(event -> updateLoader());
        add.addActionListener(event -> addSelection());
        remove.addActionListener(event -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                selected.remove(row);
                model.removeRow(row);
                changed.accept(selections());
            }
        });
        refresh.addActionListener(event -> loadCatalog(parser));
        changed.accept(selections());
        loadCatalog(parser);
    }

    public List<Map<String, String>> selections() {
        return selected.stream().map(LinkedHashMap::new).map(value -> (Map<String, String>) value).toList();
    }

    private void append(Map<String, String> row) {
        model.addRow(new Object[]{row.get("minecraft_version"), row.get("loader"), row.get("loader_version"), row.getOrDefault("fabric_api_version", ""), row.get("java_version")});
    }

    private void loadCatalog(Function<String, Object> parser) {
        add.setEnabled(false);
        refresh.setEnabled(false);
        status.setText("Loading official version catalogs…");
        new SwingWorker<VersionCatalog, Void>() {
            @Override
            protected VersionCatalog doInBackground() throws Exception {
                VersionCatalog data = new VersionCatalog(parser);
                data.load();
                return data;
            }

            @Override
            protected void done() {
                refresh.setEnabled(true);
                try {
                    catalog = get();
                    minecraft.setModel(new DefaultComboBoxModel<>(catalog.minecraftVersions().toArray(String[]::new)));
                    status.setText(catalog.minecraftVersions().size() + " Minecraft releases · Mojang / Fabric / Forge / NeoForge");
                    loadJava();
                } catch (Exception error) {
                    status.setText("Version download failed. Refresh to retry.");
                    toolchain.setText(error.getCause() == null ? error.getMessage() : error.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void loadJava() {
        if (catalog == null || minecraft.getSelectedItem() == null) return;
        String mc = minecraft.getSelectedItem().toString();
        add.setEnabled(false);
        toolchain.setText("Loading Minecraft Java requirement…");
        loader.setModel(new DefaultComboBoxModel<>(catalog.loaders(mc).toArray(String[]::new)));
        updateLoader();
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return catalog.javaVersion(mc);
            }

            @Override
            protected void done() {
                if (!mc.equals(minecraft.getSelectedItem())) return;
                try {
                    javaVersion = get();
                    toolchain.setText("Java " + javaVersion + " · Gradle " + (javaVersion >= 25 ? "9.x" : "8.14.4"));
                    add.setEnabled(version.getItemCount() > 0);
                } catch (Exception error) {
                    toolchain.setText("Could not read Minecraft Java metadata");
                }
            }
        }.execute();
    }

    private void updateLoader() {
        if (catalog == null || minecraft.getSelectedItem() == null || loader.getSelectedItem() == null) return;
        String mc = minecraft.getSelectedItem().toString(), platform = loader.getSelectedItem().toString();
        version.setModel(new DefaultComboBoxModel<>(catalog.loaderVersions(platform, mc).toArray(String[]::new)));
        api.setModel(new DefaultComboBoxModel<>(("fabric".equals(platform) ? catalog.apiVersions(mc) : List.<String>of()).toArray(String[]::new)));
        api.setEnabled("fabric".equals(platform));
    }

    private void addSelection() {
        String mc = minecraft.getSelectedItem().toString(), platform = loader.getSelectedItem().toString();
        if (selected.stream().anyMatch(row -> mc.equals(row.get("minecraft_version")) && platform.equals(row.get("loader")))) {
            status.setText("This target is already in the list.");
            return;
        }
        Map<String, String> row = new LinkedHashMap<>();
        row.put("minecraft_version", mc);
        row.put("loader", platform);
        row.put("loader_version", version.getSelectedItem().toString());
        row.put("java_version", Integer.toString(javaVersion));
        row.put("gradle_java_version", Integer.toString(javaVersion >= 25 ? 25 : 21));
        row.put("blueprint", switch (platform) {
            case "fabric" -> javaVersion >= 25 ? "fabric-26.1.2" : "fabric-1.20.1";
            case "forge" -> "forge-1.20.1";
            default -> javaVersion >= 25 ? "neoforge-26.1.2" : "neoforge-1.21.1";
        });
        row.put("wrapper_version", javaVersion >= 25 ? ("fabric".equals(platform) ? "9.7.1" : "9.5.1") : "8.14.4");
        if ("fabric".equals(platform) && VersionCatalog.compare(mc, "1.21.4") > 0) {
            row.put("loom_version", "1.18.2");
            row.put("wrapper_version", "9.7.1");
        }
        if ("fabric".equals(platform)) row.put("fabric_api_version", api.getSelectedItem().toString());
        if ("forge".equals(platform) && VersionCatalog.compare(mc, "1.20.1") > 0)
            row.put("forgegradle_version", "6.0.54");
        selected.add(row);
        append(row);
        changed.accept(selections());
        status.setText(selected.size() + " targets selected");
    }
}

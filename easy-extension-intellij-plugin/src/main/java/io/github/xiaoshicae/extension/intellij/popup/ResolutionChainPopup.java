package io.github.xiaoshicae.extension.intellij.popup;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import io.github.xiaoshicae.extension.intellij.EasyExtensionIcons;
import io.github.xiaoshicae.extension.intellij.util.EasyExtensionAnnotations;
import io.github.xiaoshicae.extension.intellij.util.PsiSearchUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Custom popup that shows the extension point resolution chain grouped by Business.
 */
public final class ResolutionChainPopup {

    private ResolutionChainPopup() {
    }

    public static void show(PsiClass extensionPointClass, MouseEvent mouseEvent) {
        Project project = extensionPointClass.getProject();

        DefaultMutableTreeNode root = ReadAction.compute(() -> {
            DefaultMutableTreeNode r = new DefaultMutableTreeNode("root");
            buildResolutionTree(r, extensionPointClass, project);
            return r;
        });

        Tree tree = new Tree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new PopupTreeCellRenderer());

        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        JBScrollPane scrollPane = new JBScrollPane(tree);
        Dimension treePreferred = tree.getPreferredSize();
        int width = Math.max(Math.min(treePreferred.width + 40, 600), 250);
        int height = Math.max(Math.min(treePreferred.height + 10, 400), 50);
        scrollPane.setPreferredSize(new Dimension(width, height));

        JBPopup popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(scrollPane, tree)
                .setTitle("Implementations of " + extensionPointClass.getName())
                .setMovable(true)
                .setResizable(true)
                .setRequestFocus(true)
                .createPopup();

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    var path = tree.getSelectionPath();
                    if (path == null) return;
                    var node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.getUserObject() instanceof NodeData data && data.psiClass != null) {
                        Navigatable nav = (Navigatable) data.psiClass;
                        if (nav.canNavigate()) {
                            popup.cancel();
                            nav.navigate(true);
                        }
                    }
                }
            }
        });

        popup.show(new RelativePoint(mouseEvent));
    }

    private static void buildResolutionTree(DefaultMutableTreeNode root, PsiClass ep, Project project) {
        List<PsiClass> allImpls = PsiSearchUtil.findExtensionPointImplementations(ep);

        Set<String> implQNames = new HashSet<>();
        PsiClass defaultImpl = null;
        for (PsiClass impl : allImpls) {
            String qn = impl.getQualifiedName();
            if (qn != null) {
                implQNames.add(qn);
            }
            if (PsiSearchUtil.hasAnnotation(impl, EasyExtensionAnnotations.DEFAULT_IMPLEMENTATION)) {
                defaultImpl = impl;
            }
        }

        Collection<PsiClass> allBusinesses = PsiSearchUtil.findAllBusinesses(project);

        for (PsiClass biz : allBusinesses) {
            PsiAnnotation bizAnn = biz.getAnnotation(EasyExtensionAnnotations.BUSINESS);
            if (bizAnn == null) {
                continue;
            }

            List<ChainItem> chain = new ArrayList<>();

            // Check if Business itself implements this EP
            String bizQName = biz.getQualifiedName();
            if (bizQName != null && implQNames.contains(bizQName)) {
                int priority = parseAnnotationPriority(bizAnn);
                chain.add(new ChainItem(biz, priority, false));
            }

            // Check mounted abilities that implement this EP
            List<String> abilityRawValues = PsiSearchUtil.parseAbilityRawValues(bizAnn);
            for (String raw : abilityRawValues) {
                int sep = raw.indexOf("::");
                String code = sep > 0 ? raw.substring(0, sep) : raw;
                int abilityPriority = sep > 0 ? parsePriorityInt(raw.substring(sep + 2)) : 0;

                PsiClass abilityClass = PsiSearchUtil.findAbilityByCode(project, code);
                if (abilityClass != null) {
                    String abilityQName = abilityClass.getQualifiedName();
                    if (abilityQName != null && implQNames.contains(abilityQName)) {
                        chain.add(new ChainItem(abilityClass, abilityPriority, true));
                    }
                }
            }

            if (chain.isEmpty()) {
                continue;
            }

            chain.sort(Comparator.comparingInt(ChainItem::priority));

            String bizCode = PsiSearchUtil.getAnnotationStringValue(bizAnn, "code");
            String bizName = Objects.requireNonNullElse(biz.getName(), "?");
            String bizDisplay = bizCode != null
                    ? String.format("%s (code: %s)", bizName, bizCode)
                    : bizName;

            DefaultMutableTreeNode bizNode = new DefaultMutableTreeNode(
                    new NodeData(bizDisplay, biz, NodeType.BUSINESS));

            for (int i = 0; i < chain.size(); i++) {
                ChainItem item = chain.get(i);
                String itemName = Objects.requireNonNullElse(item.psiClass.getName(), "?");
                String label = item.isMountedAbility ? "mounted" : "own impl";
                String display = String.format("%d. %s (%s, priority: %d)",
                        i + 1, itemName, label, item.priority);
                NodeType type = item.isMountedAbility ? NodeType.ABILITY : NodeType.BUSINESS_IMPL;
                bizNode.add(new DefaultMutableTreeNode(new NodeData(display, item.psiClass, type)));
            }

            root.add(bizNode);
        }

        // Default impl at the bottom, only once
        if (defaultImpl != null) {
            String defName = Objects.requireNonNullElse(defaultImpl.getName(), "?");
            root.add(new DefaultMutableTreeNode(
                    new NodeData("[Default] " + defName, defaultImpl, NodeType.DEFAULT_IMPL)));
        }
    }

    private static int parseAnnotationPriority(PsiAnnotation annotation) {
        var val = annotation.findAttributeValue("priority");
        if (val == null) {
            return 0;
        }
        return parsePriorityInt(val.getText());
    }

    private static int parsePriorityInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record ChainItem(PsiClass psiClass, int priority, boolean isMountedAbility) {
    }

    enum NodeType {
        BUSINESS, BUSINESS_IMPL, ABILITY, DEFAULT_IMPL
    }

    record NodeData(String displayText, PsiClass psiClass, NodeType type) {
        @Override
        public String toString() {
            return displayText;
        }
    }

    private static class PopupTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof NodeData data) {
                switch (data.type) {
                    case BUSINESS -> setIcon(EasyExtensionIcons.BUSINESS);
                    case BUSINESS_IMPL -> setIcon(EasyExtensionIcons.BUSINESS);
                    case ABILITY -> setIcon(EasyExtensionIcons.ABILITY);
                    case DEFAULT_IMPL -> setIcon(EasyExtensionIcons.DEFAULT_IMPL);
                }
            }
            return this;
        }
    }
}

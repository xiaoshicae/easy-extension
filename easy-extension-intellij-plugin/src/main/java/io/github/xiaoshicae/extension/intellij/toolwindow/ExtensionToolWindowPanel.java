package io.github.xiaoshicae.extension.intellij.toolwindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.icons.AllIcons;
import io.github.xiaoshicae.extension.intellij.EasyExtensionIcons;
import io.github.xiaoshicae.extension.intellij.util.EasyExtensionAnnotations;
import io.github.xiaoshicae.extension.intellij.util.PsiSearchUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 扩展点拓扑视图面板
 */
public class ExtensionToolWindowPanel extends JPanel {

    private final Project project;
    private final Tree tree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootNode;

    public ExtensionToolWindowPanel(Project project) {
        super(new BorderLayout());
        this.project = project;

        rootNode = new DefaultMutableTreeNode("Easy Extension");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new Tree(treeModel);
        tree.setCellRenderer(new ExtensionTreeCellRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        // 双击导航到源码
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateToSource();
                }
            }
        });

        // 工具栏
        ActionToolbar toolbar = createToolbar();
        toolbar.setTargetComponent(this);

        add(toolbar.getComponent(), BorderLayout.NORTH);
        add(new JBScrollPane(tree), BorderLayout.CENTER);

        // 异步初始加载，避免阻塞 EDT
        ApplicationManager.getApplication().invokeLater(this::refresh);
    }

    private ActionToolbar createToolbar() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction("Refresh", "Rescan extension points", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                refresh();
            }
        });
        group.add(new AnAction("Expand All", "Expand all nodes", AllIcons.Actions.Expandall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                expandAll();
            }
        });
        group.add(new AnAction("Collapse All", "Collapse all nodes", AllIcons.Actions.Collapseall) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                collapseAll();
            }
        });
        return ActionManager.getInstance().createActionToolbar("EasyExtensionToolbar", group, true);
    }

    /**
     * 刷新扩展点拓扑树（PSI 读操作在后台线程，UI 更新回 EDT）
     */
    public void refresh() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            DefaultMutableTreeNode newRoot = ReadAction.compute(() -> {
                DefaultMutableTreeNode root = new DefaultMutableTreeNode("Easy Extension");
                buildTree(root);
                return root;
            });

            // UI 操作必须在 EDT 上执行
            ApplicationManager.getApplication().invokeLater(() -> {
                rootNode.removeAllChildren();
                // 把新构建的子节点移到 rootNode 下
                while (newRoot.getChildCount() > 0) {
                    rootNode.add((DefaultMutableTreeNode) newRoot.getFirstChild());
                }
                treeModel.reload();
                expandAll();
            });
        });
    }

    private void buildTree(DefaultMutableTreeNode root) {
        Collection<PsiClass> extensionPoints = PsiSearchUtil.findAllExtensionPoints(project);
        if (extensionPoints.isEmpty()) {
            root.add(new DefaultMutableTreeNode(
                    new NodeData("No extension points found (ensure the project depends on easy-extension-core)", null, NodeType.INFO)));
            return;
        }

        Collection<PsiClass> allBusinesses = PsiSearchUtil.findAllBusinesses(project);

        for (PsiClass ep : extensionPoints) {
            String epName = ep.getName();
            if (epName == null) {
                continue;
            }

            DefaultMutableTreeNode epNode = new DefaultMutableTreeNode();

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

            int businessCount = 0;

            for (PsiClass biz : allBusinesses) {
                PsiAnnotation bizAnn = biz.getAnnotation(EasyExtensionAnnotations.BUSINESS);
                if (bizAnn == null) {
                    continue;
                }

                List<ResolutionChainItem> chain = new ArrayList<>();

                String bizQName = biz.getQualifiedName();
                if (bizQName != null && implQNames.contains(bizQName)) {
                    int priority = parseAnnotationPriority(bizAnn);
                    chain.add(new ResolutionChainItem(biz, priority, false));
                }

                List<String> abilityRawValues = PsiSearchUtil.parseAbilityRawValues(bizAnn);
                for (String raw : abilityRawValues) {
                    int sep = raw.indexOf("::");
                    String code = sep > 0 ? raw.substring(0, sep) : raw;
                    int abilityPriority = sep > 0 ? parsePriorityInt(raw.substring(sep + 2)) : 0;

                    PsiClass abilityClass = PsiSearchUtil.findAbilityByCode(project, code);
                    if (abilityClass != null) {
                        String abilityQName = abilityClass.getQualifiedName();
                        if (abilityQName != null && implQNames.contains(abilityQName)) {
                            chain.add(new ResolutionChainItem(abilityClass, abilityPriority, true));
                        }
                    }
                }

                if (chain.isEmpty()) {
                    continue;
                }

                chain.sort(Comparator.comparingInt(ResolutionChainItem::priority));
                businessCount++;

                String bizCode = PsiSearchUtil.getAnnotationStringValue(bizAnn, "code");
                String bizName = Objects.requireNonNullElse(biz.getName(), "?");
                String bizDisplay = bizCode != null
                        ? String.format("%s (code: %s)", bizName, bizCode)
                        : bizName;
                DefaultMutableTreeNode bizNode = new DefaultMutableTreeNode(
                        new NodeData(bizDisplay, biz, NodeType.BUSINESS));

                for (int i = 0; i < chain.size(); i++) {
                    ResolutionChainItem item = chain.get(i);
                    String itemName = Objects.requireNonNullElse(item.psiClass().getName(), "?");
                    String label = item.isMountedAbility() ? "mounted" : "own impl";
                    String display = String.format("%d. %s (%s, priority: %d)",
                            i + 1, itemName, label, item.priority());
                    NodeType type = item.isMountedAbility() ? NodeType.ABILITY : NodeType.BUSINESS;
                    bizNode.add(new DefaultMutableTreeNode(new NodeData(display, item.psiClass(), type)));
                }

                epNode.add(bizNode);
            }

            if (defaultImpl != null) {
                String defName = Objects.requireNonNullElse(defaultImpl.getName(), "?");
                epNode.add(new DefaultMutableTreeNode(
                        new NodeData("[Default] " + defName, defaultImpl, NodeType.DEFAULT_IMPL)));
            }

            String summary = String.format("%s  [%d Business]", epName, businessCount);
            epNode.setUserObject(new NodeData(summary, ep, NodeType.EXTENSION_POINT));
            root.add(epNode);
        }
    }

    private record ResolutionChainItem(PsiClass psiClass, int priority, boolean isMountedAbility) {}

    private int parseAnnotationPriority(PsiAnnotation annotation) {
        var val = annotation.findAttributeValue("priority");
        if (val == null) {
            return 0;
        }
        return parsePriorityInt(val.getText());
    }

    private int parsePriorityInt(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void navigateToSource() {
        var path = tree.getSelectionPath();
        if (path == null) {
            return;
        }
        var node = (DefaultMutableTreeNode) path.getLastPathComponent();
        var data = node.getUserObject();
        if (data instanceof NodeData nodeData && nodeData.psiClass() != null) {
            PsiClass psiClass = nodeData.psiClass();
            if (psiClass instanceof Navigatable) {
                Navigatable navigatable = (Navigatable) psiClass;
                if (navigatable.canNavigate()) {
                    navigatable.navigate(true);
                }
            }
        }
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void collapseAll() {
        for (int i = tree.getRowCount() - 1; i >= 0; i--) {
            tree.collapseRow(i);
        }
    }

    // ---- 树节点数据模型 ----

    enum NodeType {
        EXTENSION_POINT, ABILITY, BUSINESS, DEFAULT_IMPL, PROPERTY, INFO
    }

    record NodeData(String displayText, PsiClass psiClass, NodeType type) {
        @Override
        public String toString() {
            return displayText;
        }
    }

    // ---- 自定义树节点渲染器 ----

    private static class ExtensionTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof NodeData data) {
                switch (data.type()) {
                    case EXTENSION_POINT -> setIcon(EasyExtensionIcons.EXTENSION_POINT);
                    case ABILITY -> setIcon(EasyExtensionIcons.ABILITY);
                    case BUSINESS -> setIcon(EasyExtensionIcons.BUSINESS);
                    case DEFAULT_IMPL -> setIcon(EasyExtensionIcons.DEFAULT_IMPL);
                    case PROPERTY -> {
                        setIcon(AllIcons.Nodes.Property);
                        setForeground(JBColor.GRAY);
                    }
                    case INFO -> {
                        setIcon(AllIcons.General.Information);
                        setForeground(JBColor.GRAY);
                    }
                }
            }
            return this;
        }
    }
}

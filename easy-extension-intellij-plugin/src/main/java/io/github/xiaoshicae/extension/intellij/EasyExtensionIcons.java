package io.github.xiaoshicae.extension.intellij;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * Easy-Extension 插件图标常量
 */
public final class EasyExtensionIcons {

    /** 扩展点接口图标 */
    public static final Icon EXTENSION_POINT = IconLoader.getIcon("/icons/extensionPoint.svg", EasyExtensionIcons.class);

    /** 能力图标 */
    public static final Icon ABILITY = IconLoader.getIcon("/icons/ability.svg", EasyExtensionIcons.class);

    /** 业务图标 */
    public static final Icon BUSINESS = IconLoader.getIcon("/icons/business.svg", EasyExtensionIcons.class);

    /** 默认实现图标 */
    public static final Icon DEFAULT_IMPL = IconLoader.getIcon("/icons/defaultImpl.svg", EasyExtensionIcons.class);

    /** Gutter: 导航到实现 */
    public static final Icon GUTTER_IMPL = IconLoader.getIcon("/icons/gutterImpl.svg", EasyExtensionIcons.class);

    /** Gutter: 导航到接口 */
    public static final Icon GUTTER_INTERFACE = IconLoader.getIcon("/icons/gutterInterface.svg", EasyExtensionIcons.class);

    /** Tool Window 图标 */
    public static final Icon TOOL_WINDOW = IconLoader.getIcon("/icons/toolWindow.svg", EasyExtensionIcons.class);

    private EasyExtensionIcons() {
    }
}

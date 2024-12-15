package com.github.hanfeng21050.utils;

import com.intellij.ide.presentation.Presentation;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @Author hanfeng32305
 * @Date 2024/7/17 18:53
 */
@Presentation
public interface EasyIcons {
    Icon mainIcon = IconLoader.getIcon("/icons/icon-e.svg", EasyIcons.class);
    Icon deleteIcon = IconLoader.getIcon("/icons/icon-delete.svg", EasyIcons.class);
    Icon genIcon = IconLoader.getIcon("/icons/icon-gen.png", EasyIcons.class);
    Icon testIcon = IconLoader.getIcon("/icons/icon-test.png", EasyIcons.class);
    Icon sqlIcon = IconLoader.getIcon("/icons/icon-sql.svg", EasyIcons.class);
}

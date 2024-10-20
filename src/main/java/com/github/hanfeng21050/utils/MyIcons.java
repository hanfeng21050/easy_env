package com.github.hanfeng21050.utils;

import com.intellij.ide.presentation.Presentation;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @Author hanfeng32305
 * @Date 2024/7/17 18:53
 */
@Presentation
public interface MyIcons {
    Icon mainIcon = IconLoader.getIcon("/icons/icon-e.svg", MyIcons.class);
    Icon deleteIcon = IconLoader.getIcon("/icons/icon-delete.svg", MyIcons.class);
    Icon genIcon = IconLoader.getIcon("/icons/icon-gen.png", MyIcons.class);
    Icon testIcon = IconLoader.getIcon("/icons/icon-test.png", MyIcons.class);
    Icon sqlIcon = IconLoader.getIcon("/icons/icon-sql.svg", MyIcons.class);
}

package com.github.hanfeng21050.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.*;
import java.lang.reflect.Method;

/**
 * ContentFactory工具类，用于处理不同版本IDEA中ContentFactory API的兼容性问题
 *
 * @author hanfeng21050
 * @date 2024/12/15
 */
public class ContentFactoryUtils {
    private static final Logger LOG = Logger.getInstance(ContentFactoryUtils.class);

    /**
     * 创建Content实例
     * 兼容不同版本的IDEA API
     *
     * @param component   组件
     * @param displayName 显示名称
     * @param isLockable  是否可锁定
     * @return Content实例
     */
    public static Content createContent(JComponent component, String displayName, boolean isLockable) {
        // 方法1：尝试使用反射调用ContentFactory.getInstance() (2019.1+)
        try {
            Method getInstanceMethod = ContentFactory.class.getMethod("getInstance");
            ContentFactory factory = (ContentFactory) getInstanceMethod.invoke(null);
            return factory.createContent(component, displayName, isLockable);
        } catch (Exception e) {
            LOG.debug("ContentFactory.getInstance() not available, trying SERVICE approach");
        }

        // 方法2：尝试使用SERVICE.getInstance() (早期版本)
        try {
            java.lang.reflect.Field serviceField = ContentFactory.class.getField("SERVICE");
            Object service = serviceField.get(null);
            Method getInstanceMethod = service.getClass().getMethod("getInstance");
            ContentFactory factory = (ContentFactory) getInstanceMethod.invoke(service);
            return factory.createContent(component, displayName, isLockable);
        } catch (Exception ex) {
            LOG.debug("ContentFactory.SERVICE.getInstance() not available, trying direct creation");
        }

        // 方法3：直接创建ContentImpl实例 (最后的回退方案)
        try {
            Class<?> contentImplClass = Class.forName("com.intellij.ui.content.impl.ContentImpl");
            Object content = contentImplClass.getConstructor(JComponent.class, String.class, boolean.class)
                    .newInstance(component, displayName, isLockable);
            return (Content) content;
        } catch (Exception e) {
            LOG.error("Failed to create content with all methods", e);
            throw new RuntimeException("Failed to create content: " + e.getMessage(), e);
        }
    }
}
package com.github.hanfeng21050.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;

/**
 * 服务工具类，用于处理不同版本IDEA中ServiceManager API的兼容性问题
 *
 * @author hanfeng21050
 * @date 2024/12/15
 */
public class ServiceUtils {
    private static final Logger LOG = Logger.getInstance(ServiceUtils.class);

    /**
     * 获取应用级别的服务实例
     * 兼容不同版本的IDEA API
     *
     * @param serviceClass 服务类
     * @param <T>          服务类型
     * @return 服务实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(Class<T> serviceClass) {
        try {
            // 首先尝试使用新的API (2019.3+)
            return ApplicationManager.getApplication().getService(serviceClass);
        } catch (NoSuchMethodError | Exception e) {
            // 如果新API不可用，使用旧的API
            try {
                return ServiceManager.getService(serviceClass);
            } catch (Exception ex) {
                LOG.error("Failed to get service: " + serviceClass.getName(), ex);
                throw new RuntimeException("Failed to get service: " + serviceClass.getName(), ex);
            }
        }
    }

    /**
     * 获取项目级别的服务实例
     * 兼容不同版本的IDEA API
     *
     * @param project      项目实例
     * @param serviceClass 服务类
     * @param <T>          服务类型
     * @return 服务实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(com.intellij.openapi.project.Project project, Class<T> serviceClass) {
        try {
            // 首先尝试使用新的API (2019.3+)
            return project.getService(serviceClass);
        } catch (NoSuchMethodError | Exception e) {
            // 如果新API不可用，使用旧的API
            try {
                return ServiceManager.getService(project, serviceClass);
            } catch (Exception ex) {
                LOG.error("Failed to get project service: " + serviceClass.getName(), ex);
                throw new RuntimeException("Failed to get project service: " + serviceClass.getName(), ex);
            }
        }
    }
} 
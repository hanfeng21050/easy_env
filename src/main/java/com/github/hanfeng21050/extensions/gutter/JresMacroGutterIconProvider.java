package com.github.hanfeng21050.extensions.gutter;

import com.github.hanfeng21050.utils.EasyIcons;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

/**
 * JresMacro注解的装订线图标提供器
 * 在带有@JRESMacro注解的代码行左侧添加一个可点击的图标
 * 点击图标可以快速跳转到宏模板文档
 *
 * @author hanfeng32305
 * @date 2024/12/15
 */
public class JresMacroGutterIconProvider implements LineMarkerProvider {

    /**
     * 为指定的PSI元素创建装订线标记信息
     *
     * @param element 需要处理的PSI元素
     * @return 如果元素是JRESMacro注解则返回LineMarkerInfo，否则返回null
     */
    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        // 检查元素是否为注解
        if (element instanceof PsiAnnotation) {
            PsiAnnotation annotation = (PsiAnnotation) element;
            // 检查是否为JRESMacro注解
            if ("com.hundsun.jres.studio.annotation.JRESMacro".equals(annotation.getQualifiedName())) {
                // 创建新的行标记信息
                return new LineMarkerInfo<>(
                        element,                     // 目标元素
                        element.getTextRange(),      // 图标显示范围
                        EasyIcons.mainIcon,          // 使用自定义图标
                        psiElement -> "EasyEnv", // 鼠标悬停提示文本
                        new GutterIconNavigationHandler<PsiElement>() {
                            /**
                             * 处理图标点击事件
                             * 在点击时打开对应的宏模板文档
                             *
                             * @param e 鼠标事件
                             * @param elt 目标PSI元素
                             */
                            @Override
                            public void navigate(MouseEvent e, PsiElement elt) {
                                // todo
                            }
                        },
                        GutterIconRenderer.Alignment.LEFT,  // 图标显示在左侧
                        () -> List.of("JresMacro Documentation").toString()  // 工具提示文本
                );
            }
        }
        return null;
    }

    /**
     * 收集需要较长时间处理的行标记
     * 本实现中不需要收集额外的标记，因此为空
     *
     * @param elements 要处理的PSI元素列表
     * @param result   收集的结果集合
     */
    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        // 不需要收集额外的行标记
    }
}

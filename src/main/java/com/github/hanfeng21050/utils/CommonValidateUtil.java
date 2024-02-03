package com.github.hanfeng21050.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonValidateUtil {


    /**
     * 校验网址是否合法
     *
     * @param url
     * @return
     */
    public static boolean isValidURL(String url) {
        // 定义URL的正则表达式模式
        String urlRegex = "^(https?://[^/]+)$";

        // 创建Pattern对象
        Pattern pattern = Pattern.compile(urlRegex);

        // 创建Matcher对象
        Matcher matcher = pattern.matcher(url);

        // 检查匹配结果
        return matcher.matches();
    }


    public static boolean isFileNameMatch(String fileName, String pattern) {
        // 创建 PathMatcher
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        // 将文件名转换为 Path 对象
        Path path = Paths.get(fileName);

        // 执行匹配
        return matcher.matches(path);
    }
}

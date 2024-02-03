package com.github.hanfeng21050.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CryptUtil {

    public static String encryptPassword(String password) throws Exception {
        // 使用 sha512 加密
        String sha512Result = sha512(password);

        // 使用 md5 加密
        String md5Result = md5(password);

        // 拼接 sha512 和 md5 的结果
        return sha512Result + "," + md5Result;
    }



    private static String sha512(String input) {
        return hash(input, "SHA-512");
    }

    private static String md5(String input) {
        return hash(input, "MD5");
    }

    private static String hash(String input, String algorithm) {
        try {
            // 获取摘要算法实例
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            // 计算哈希值
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 将字节数组转换为十六进制字符串
            StringBuilder hexStringBuilder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                hexStringBuilder.append(hex.length() == 1 ? "0" : "").append(hex);
            }

            return hexStringBuilder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}

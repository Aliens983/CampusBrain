package com.kb.infrastructure.common;

import java.security.SecureRandom;

/**
 * 6 位数字验证码生成器
 *
 * @author forever-king
 */
public class CodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成 6 位数字验证码（000000 ~ 999999）
     */
    public static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }
}

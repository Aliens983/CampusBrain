package com.laoliu.cas.common.util;

import java.security.SecureRandom;

/**
 * @author forever-king
 */
public class CodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }
}

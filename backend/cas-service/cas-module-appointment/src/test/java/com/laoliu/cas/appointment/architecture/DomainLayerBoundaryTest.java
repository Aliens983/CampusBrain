package com.laoliu.cas.appointment.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 领域层边界守卫（审查报告 2.1，D-A3）。
 * <p>
 * domain 是四层最内层，禁止 compile-time 依赖外层包
 * （BookingRepository/ConsultantRepository 曾直接返回 ServiceStatusResponse /
 * TimeSlotResponse，形成 domain→interfaces 反向依赖）。2.1 后领域查询统一走
 * domain/view 下的读模型；2.2 后请求/响应 DTO 整体移居 application/dto。
 * 本测试扫描 target/classes 下 domain 包全部 class 字节码，任何对外层包
 * {@code com/laoliu/cas/appointment/interfaces} 或
 * {@code com/laoliu/cas/appointment/application} 的符号引用都视为回归。
 *
 * @author forever-king
 */
class DomainLayerBoundaryTest {

    private static final String DOMAIN_PATH = "com/laoliu/cas/appointment/domain/";
    private static final byte[][] FORBIDDEN_TOKENS = {
            "com/laoliu/cas/appointment/interfaces".getBytes(),
            "com/laoliu/cas/appointment/application".getBytes()
    };

    @Test
    @DisplayName("2.1 domain 层任何 class 不得引用 interfaces 包（反向依赖守卫）")
    void domainMustNotReferenceInterfacesLayer() throws Exception {
        List<String> violations = new ArrayList<>();
        Enumeration<URL> roots = Thread.currentThread().getContextClassLoader()
                .getResources(DOMAIN_PATH);
        boolean scanned = false;
        while (roots.hasMoreElements()) {
            File dir = new File(roots.nextElement().toURI());
            if (!dir.isDirectory()) {
                continue; // jar 内的同名包跳过（只守卫本模块 target/classes）
            }
            scanned = true;
            try (Stream<Path> classes = Files.walk(dir.toPath())) {
                classes.filter(p -> p.toString().endsWith(".class")).forEach(classFile -> {
                    try {
                        byte[] bytes = Files.readAllBytes(classFile);
                        for (byte[] token : FORBIDDEN_TOKENS) {
                            if (contains(bytes, token)) {
                                violations.add(classFile + " -> " + new String(token).replace('/', '.'));
                            }
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException("读取 class 失败: " + classFile, e);
                    }
                });
            }
        }

        assertTrue(scanned, "必须扫到 target/classes 下的 domain 包，否则守卫形同虚设");
        assertTrue(violations.isEmpty(),
                () -> "domain 层 class 不得引用 interfaces 层，请改用 domain/view 读模型 + interfaces 层 Convert："
                        + violations);
    }

    /** 朴素字节数组包含判定（class 文件较小，避免引第三方依赖） */
    private static boolean contains(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }
}

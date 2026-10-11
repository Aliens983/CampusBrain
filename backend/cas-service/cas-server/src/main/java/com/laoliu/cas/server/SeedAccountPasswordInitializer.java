package com.laoliu.cas.server;

import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 种子账号口令首启重置（默认不启用）
 * <p>
 * Flyway V2/V3 播种的 11 个账号（管理员、普通用户、9 名教师）统一使用演示口令
 * 123456 的固定 BCrypt 哈希。部署环境通过环境变量 {@code SEED_ACCOUNT_PASSWORD}
 * （→ {@code app.security.seed-account-password}）注入新口令后，本组件在启动阶段
 * 仅把「仍保持原始种子哈希」的账号重置为新口令；已经自行改过密码的账号不受影响。
 * </p>
 * <p>未设置环境变量时完全跳过，本地开发与存量环境行为不变。</p>
 *
 * @author forever-king
 */
@Slf4j
@Component
public class SeedAccountPasswordInitializer implements ApplicationRunner {

    /** V2/V3 种子脚本中 123456 对应的固定 BCrypt 哈希（$2b$ 版本，BCryptPasswordEncoder 可校验） */
    static final String SEED_DEFAULT_PASSWORD_HASH =
            "$2b$10$X.JQtw4f4kuFXmbCWuHZOOHsgL46rNCV3hXzDrAd5OSNTSXDZNmv2";

    /** V2/V3 播种的全部种子账号邮箱 */
    static final List<String> SEED_EMAILS = List.of(
            "admin@campus.com",
            "user@campus.com",
            "xiao@campus.com",
            "zhou@campus.com",
            "liu@campus.com",
            "shi@campus.com",
            "guan@campus.com",
            "yao@campus.com",
            "qiu@campus.com",
            "sun@campus.com",
            "guanxs@campus.com"
    );

    /** 视为生产部署、需要执行弱口令 fail-fast 的 profile（容器统一走 docker） */
    private static final Set<String> PRODUCTION_PROFILES = Set.of("docker", "prod");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final String seedAccountPassword;
    private final boolean productionProfile;

    public SeedAccountPasswordInitializer(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.seed-account-password:}") String seedAccountPassword,
            @Value("${spring.profiles.active:}") String activeProfiles) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.seedAccountPassword = seedAccountPassword;
        this.productionProfile = Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(PRODUCTION_PROFILES::contains);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedAccountPassword == null || seedAccountPassword.isBlank()) {
            // 生产 profile 下留空不能无条件放行：必须确认种子账号都已改过密码，
            // 否则带着公开演示口令 123456 的实例一旦暴露到网络即被接管。
            // 已全部改密后允许留空（兼容部署文档"生效后删除该变量"的约定）。
            if (productionProfile) {
                enforceSeedPasswordsChanged();
            }
            return;
        }
        String newHash = passwordEncoder.encode(seedAccountPassword);
        int changed = 0;
        for (String email : SEED_EMAILS) {
            String currentHash = userMapper.getEncodePasswordByEmail(email);
            // 账号不存在（种子脚本被裁剪）或用户已自行改密（哈希不再等于种子哈希）时跳过
            if (currentHash == null || !SEED_DEFAULT_PASSWORD_HASH.equals(currentHash)) {
                continue;
            }
            userMapper.updatePasswordByEmail(email, newHash);
            changed++;
        }
        log.warn("种子账号口令初始化：SEED_ACCOUNT_PASSWORD 已注入，{} 个仍使用演示口令的"
                + "种子账号已重置，其余账号未动；确认生效后请从部署环境移除该变量", changed);
    }

    /**
     * 生产部署的弱口令闸门：仍有种子账号保持公开演示哈希时拒绝启动。
     * <p>
     * 处置方式二选一：① 注入 SEED_ACCOUNT_PASSWORD 后重启（首启自动重置）；
     * ② 手工修改全部种子账号口令后再重启。全部改密后本检查自然放行，
     * 此后可永久移除该环境变量。
     * </p>
     */
    private void enforceSeedPasswordsChanged() {
        for (String email : SEED_EMAILS) {
            String currentHash = userMapper.getEncodePasswordByEmail(email);
            if (SEED_DEFAULT_PASSWORD_HASH.equals(currentHash)) {
                throw new IllegalStateException("""
                        生产环境种子账号 %s 仍使用公开演示口令 123456，拒绝启动。\
                        请注入 SEED_ACCOUNT_PASSWORD 后重启（仍为演示口令的种子账号会被自动重置），\
                        或手工修改全部种子账号口令；确认全部改密后可移除该环境变量。""".formatted(email));
            }
        }
        log.info("生产环境未注入 SEED_ACCOUNT_PASSWORD，且全部种子账号均已改密，跳过口令初始化");
    }
}

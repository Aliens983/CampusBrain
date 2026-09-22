package com.laoliu.cas.server;

import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

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

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final String seedAccountPassword;

    public SeedAccountPasswordInitializer(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.seed-account-password:}") String seedAccountPassword) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.seedAccountPassword = seedAccountPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedAccountPassword == null || seedAccountPassword.isBlank()) {
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
}

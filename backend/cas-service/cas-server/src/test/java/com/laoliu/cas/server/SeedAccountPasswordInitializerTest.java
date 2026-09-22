package com.laoliu.cas.server;

import com.laoliu.cas.system.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SeedAccountPasswordInitializer} 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
class SeedAccountPasswordInitializerTest {

    private static final String DEFAULT_HASH = SeedAccountPasswordInitializer.SEED_DEFAULT_PASSWORD_HASH;

    @Mock
    private UserMapper userMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void blankPassword_skipsEverything() {
        SeedAccountPasswordInitializer initializer =
                new SeedAccountPasswordInitializer(userMapper, passwordEncoder, "  ");

        initializer.run(null);

        verify(userMapper, never()).updatePasswordByEmail(anyString(), anyString());
        verify(userMapper, never()).getEncodePasswordByEmail(anyString());
    }

    @Test
    void nullPassword_skipsEverything() {
        SeedAccountPasswordInitializer initializer =
                new SeedAccountPasswordInitializer(userMapper, passwordEncoder, null);

        initializer.run(null);

        verify(userMapper, never()).updatePasswordByEmail(anyString(), anyString());
        verify(userMapper, never()).getEncodePasswordByEmail(anyString());
    }

    @Test
    void accountStillOnSeedHash_isResetToEncodedNewPassword() {
        // 默认账号不存在，仅 admin 仍为种子哈希
        when(userMapper.getEncodePasswordByEmail("admin@campus.com")).thenReturn(DEFAULT_HASH);

        new SeedAccountPasswordInitializer(userMapper, passwordEncoder, "New-Strong-Pwd-9")
                .run(null);

        verify(userMapper).updatePasswordByEmail(eq("admin@campus.com"),
                org.mockito.ArgumentMatchers.argThat(hash ->
                        passwordEncoder.matches("New-Strong-Pwd-9", hash)));
        // 其余种子账号（查询返回 null）不更新
        verify(userMapper, never()).updatePasswordByEmail(eq("user@campus.com"), anyString());
    }

    @Test
    void accountAlreadyChanged_isLeftUntouched() {
        String userChangedHash = passwordEncoder.encode("user-own-password");
        when(userMapper.getEncodePasswordByEmail("user@campus.com")).thenReturn(userChangedHash);
        when(userMapper.getEncodePasswordByEmail("admin@campus.com")).thenReturn(DEFAULT_HASH);

        new SeedAccountPasswordInitializer(userMapper, passwordEncoder, "New-Strong-Pwd-9")
                .run(null);

        verify(userMapper, never()).updatePasswordByEmail(eq("user@campus.com"), anyString());
        verify(userMapper).updatePasswordByEmail(eq("admin@campus.com"), anyString());
    }
}

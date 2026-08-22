package com.kb.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link UserRepository} with H2 in-memory database.
 * Uses {@code @SpringBootTest} + test profile (H2 datasource).
 * @author forever-king
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserRepository 集成测试")
class UserRepositoryIntegrationTest {

    // 隔离外部中间件：只验证 H2 持久化，不依赖 MQ/Redis
    @MockBean private ConnectionFactory rabbitConnectionFactory;
    @MockBean private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("持久化和查询")
    class PersistenceAndQuery {

        @Test
        @DisplayName("保存用户后可按 ID 查询")
        void shouldFindByIdAfterSave() {
            KbUser user = KbUser.builder()
                    .username("testuser")
                    .email("test@example.com")
                    .passwordHash("encoded-password")
                    .role("USER")
                    .nickname("测试用户")
                    .enabled(true)
                    .build();

            KbUser saved = userRepository.save(user);
            assertThat(saved.getId()).isNotNull();

            Optional<KbUser> found = userRepository.findById(saved.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("testuser");
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("按用户名查找应返回正确用户")
        void shouldFindByUsername() {
            KbUser user = KbUser.builder()
                    .username("alice")
                    .email("alice@example.com")
                    .passwordHash("pw")
                    .role("USER")
                    .nickname("Alice")
                    .enabled(true)
                    .build();
            userRepository.save(user);

            Optional<KbUser> result = userRepository.findByUsername("alice");
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("alice@example.com");
            assertThat(result.get().getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("不存在的用户名应返回 empty")
        void shouldReturnEmptyForUnknownUsername() {
            Optional<KbUser> result = userRepository.findByUsername("nobody");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("按邮箱查找应返回正确用户")
        void shouldFindByEmail() {
            KbUser user = KbUser.builder()
                    .username("bob")
                    .email("bob@example.com")
                    .passwordHash("pw")
                    .role("USER")
                    .nickname("Bob")
                    .enabled(true)
                    .build();
            userRepository.save(user);

            Optional<KbUser> result = userRepository.findByEmail("bob@example.com");
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("bob");
        }
    }
}

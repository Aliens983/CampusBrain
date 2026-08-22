package com.kb.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.domain.user.KbUser;
import com.kb.domain.user.UserRepository;
import com.kb.infrastructure.persistence.mysql.dataobject.OAuth2AccessTokenDO;
import com.kb.infrastructure.security.EmailVerificationService;
import com.kb.infrastructure.security.OAuth2TokenService;
import com.kb.interfaces.dto.LoginRequest;
import com.kb.interfaces.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MVC slice tests for {@link AuthController}.
 * <p>
 * 使用 standalone MockMvc（手动装配 Controller + GlobalExceptionHandler + Validator），
 * 避免 {@code @WebMvcTest} 因主类显式 {@code @ComponentScan} / {@code @MapperScan} 而加载
 * 完整应用上下文导致 MyBatis / MQ 等中间件 Bean 缺失。
 * </p>
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController MVC 切片测试")
class AuthControllerMvcTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OAuth2TokenService tokenService;
    @Mock private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                userRepository, passwordEncoder, tokenService, emailVerificationService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Nested
    @DisplayName("POST /kb/auth/login")
    class Login {

        @Test
        @DisplayName("正确凭据应返回双 Token")
        void shouldReturnTokensOnValidCredentials() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("testuser");
            req.setPassword("password123");

            KbUser user = KbUser.builder()
                    .id(1L).username("testuser").passwordHash("encoded")
                    .role("USER").nickname("测试用户").build();

            OAuth2AccessTokenDO tokenDO = new OAuth2AccessTokenDO();
            tokenDO.setAccessToken("access-token-xxx");
            tokenDO.setRefreshToken("refresh-token-xxx");
            tokenDO.setUserId(1L);
            tokenDO.setRole("USER");
            tokenDO.setExpiresTime(LocalDateTime.now().plusHours(2));

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "encoded")).thenReturn(true);
            when(tokenService.createTokens(1L, "testuser", "USER", "测试用户"))
                    .thenReturn(tokenDO);

            mockMvc.perform(post("/kb/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token-xxx"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-xxx"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @DisplayName("用户名不能为空")
        void shouldRejectEmptyUsername() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("");
            req.setPassword("password123");

            mockMvc.perform(post("/kb/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("密码错误应返回 401")
        void shouldReturnUnauthorizedOnWrongPassword() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("testuser");
            req.setPassword("wrongpass");

            KbUser user = KbUser.builder()
                    .id(1L).username("testuser").passwordHash("encoded").build();

            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

            mockMvc.perform(post("/kb/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /kb/auth/register")
    class Register {

        @Test
        @DisplayName("验证码缺失应返回 400")
        void shouldRejectMissingVerificationCode() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("newuser");
            req.setEmail("new@test.com");
            req.setCode("");        // 空验证码
            req.setPassword("password123");

            mockMvc.perform(post("/kb/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("用户名过短应返回 400")
        void shouldRejectShortUsername() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("a");
            req.setEmail("a@test.com");
            req.setCode("123456");
            req.setPassword("password123");

            mockMvc.perform(post("/kb/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /kb/auth/logout")
    class Logout {

        @Test
        @DisplayName("无 Authorization 头也应成功返回")
        void shouldReturnSuccessEvenWithoutAuthHeader() throws Exception {
            mockMvc.perform(post("/kb/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}

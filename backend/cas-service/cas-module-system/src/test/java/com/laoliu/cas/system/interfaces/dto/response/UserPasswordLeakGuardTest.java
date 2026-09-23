package com.laoliu.cas.system.interfaces.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.system.interfaces.convert.UserConvert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户敏感信息外泄守护测试：
 * 对外 DTO 的 JSON 不得包含 password；实体/DO 的 toString 不得带出密码哈希。
 *
 * @author forever-king
 */
class UserPasswordLeakGuardTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void userResponse_jsonNeverContainsPassword() throws Exception {
        User user = User.builder()
                .id(1L).name("张三").email("a@b.com").role(0)
                .password("$2b$10$SECRETHASHSECRETHASHSECRETHASHxx")
                .build();

        UserResponse response = UserConvert.INSTANCE.convert(user);
        String json = objectMapper.writeValueAsString(response);

        assertThat(json).doesNotContain("password").doesNotContain("SECRETHASH");
    }

    @Test
    void userInfoAndBookingsResponse_jsonNeverContainsPassword() throws Exception {
        User user = User.builder()
                .id(1L).name("张三").email("a@b.com").role(1)
                .password("$2b$10$SECRETHASHSECRETHASHSECRETHASHxx")
                .build();

        UserInfoAndServicesViaMPResponse resp = new UserInfoAndServicesViaMPResponse();
        resp.setUser(UserConvert.INSTANCE.convert(user));

        String json = objectMapper.writeValueAsString(resp);

        assertThat(json).doesNotContain("password").doesNotContain("SECRETHASH");
    }

    @Test
    void changeRoleResponse_jsonNeverContainsPassword() throws Exception {
        User user = User.builder()
                .id(1L).name("张三").email("a@b.com").role(3)
                .password("$2b$10$SECRETHASHSECRETHASHSECRETHASHxx")
                .build();

        String json = objectMapper.writeValueAsString(ChangeRoleResponse.of(user, "教师"));

        assertThat(json).doesNotContain("password").doesNotContain("SECRETHASH");
    }

    @Test
    void userEntity_toStringExcludesPasswordHash() {
        User user = User.builder()
                .id(1L).name("张三")
                .password("$2b$10$SECRETHASHSECRETHASHSECRETHASHxx")
                .build();

        assertThat(user.toString()).doesNotContain("SECRETHASH").doesNotContain("password");
    }
}

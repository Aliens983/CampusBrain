package com.kb.interfaces.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApiResponse} unified response wrapper.
 * @author forever-king
 */
@DisplayName("ApiResponse 单元测试")
class ApiResponseTest {

    @Nested
    @DisplayName("success 方法")
    class Success {

        @Test
        @DisplayName("success(data) 应返回 code=0, message=成功")
        void shouldReturnSuccessWithData() {
            ApiResponse<String> resp = ApiResponse.success("hello");

            assertThat(resp.getCode()).isEqualTo(0);
            assertThat(resp.getMessage()).isEqualTo("成功");
            assertThat(resp.getData()).isEqualTo("hello");
            assertThat(resp.getTimestamp()).isPositive();
        }

        @Test
        @DisplayName("success() 无参应返回 code=0, data=null")
        void shouldReturnSuccessWithoutData() {
            ApiResponse<Void> resp = ApiResponse.success();

            assertThat(resp.getCode()).isEqualTo(0);
            assertThat(resp.getData()).isNull();
        }
    }

    @Nested
    @DisplayName("error 方法")
    class Error {

        @Test
        @DisplayName("badRequest 应返回 code=400")
        void shouldReturn400() {
            ApiResponse<Void> resp = ApiResponse.badRequest("参数错误");
            assertThat(resp.getCode()).isEqualTo(400);
            assertThat(resp.getMessage()).isEqualTo("请求参数错误: 参数错误");
        }

        @Test
        @DisplayName("notFound 应返回 code=404")
        void shouldReturn404() {
            ApiResponse<Void> resp = ApiResponse.notFound("资源不存在");
            assertThat(resp.getCode()).isEqualTo(404);
        }

        @Test
        @DisplayName("serverError 应返回 code=500")
        void shouldReturn500() {
            ApiResponse<Void> resp = ApiResponse.serverError("服务器错误");
            assertThat(resp.getCode()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Builder 模式")
    class Builder {

        @Test
        @DisplayName("通过 Builder 创建对象应正常")
        void shouldBuildCorrectly() {
            ApiResponse<Integer> resp = ApiResponse.<Integer>builder()
                    .code(201)
                    .message("created")
                    .data(42)
                    .timestamp(System.currentTimeMillis())
                    .build();

            assertThat(resp.getCode()).isEqualTo(201);
            assertThat(resp.getData()).isEqualTo(42);
        }
    }
}

package com.laoliu.cas.thirdparty.api.impl;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.thirdparty.api.WeatherApi;
import com.laoliu.cas.thirdparty.interfaces.dto.response.WeatherResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WeatherApiImpl 单元测试。
 *
 * @author forever-king
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("天气API服务单元测试")
class WeatherApiImplTest {

    @Mock
    private RestTemplate restTemplate;

    private WeatherApi weatherApi;

    private static final String API_URL = "https://api.example.com/weather";
    private static final String API_ID = "test-api-id";
    private static final String API_KEY = "test-api-key";
    private static final String PROVINCE = "浙江";
    private static final String CITY = "杭州";

    @BeforeEach
    void setUp() {
        weatherApi = new WeatherApiImpl(restTemplate);
        ReflectionTestUtils.setField(weatherApi, "weatherApiUrl", API_URL);
        ReflectionTestUtils.setField(weatherApi, "apiId", API_ID);
        ReflectionTestUtils.setField(weatherApi, "apiKey", API_KEY);
    }

    @Nested
    @DisplayName("获取天气 - getWeather")
    class GetWeatherTests {

        @Test
        @DisplayName("应当成功获取天气信息")
        void shouldGetWeatherSuccessfully() {
            // Given
            WeatherResponse mockResponse = buildWeatherResponse();
            when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class)))
                    .thenReturn(mockResponse);

            // When
            WeatherResponse result = weatherApi.getWeather(PROVINCE, CITY);

            // Then
            assertNotNull(result);
            assertEquals(200, result.getCode());
            assertEquals("浙江", result.getSheng());
            assertEquals("杭州", result.getShi());
            assertEquals("晴", result.getWeather1());
        }

        @Test
        @DisplayName("API 返回 null 时应当抛出 WEATHER_QUERY_FAILED 异常")
        void shouldThrowExceptionWhenResponseIsNull() {
            // Given
            when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class)))
                    .thenReturn(null);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> weatherApi.getWeather(PROVINCE, CITY));
            assertEquals(CommonErrorCode.WEATHER_QUERY_FAILED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("API 调用异常时应当抛出 WEATHER_QUERY_FAILED 异常")
        void shouldThrowExceptionWhenApiCallFails() {
            // Given
            when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> weatherApi.getWeather(PROVINCE, CITY));
            assertEquals(CommonErrorCode.WEATHER_QUERY_FAILED.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("API 返回异常状态码时应当抛出 WEATHER_QUERY_FAILED 异常")
        void shouldThrowExceptionWhenApiResponseCodeNot200() {
            // Given
            WeatherResponse errorResponse = buildErrorWeatherResponse();
            when(restTemplate.getForObject(anyString(), eq(WeatherResponse.class)))
                    .thenReturn(errorResponse);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> weatherApi.getWeather(PROVINCE, CITY));
            assertEquals(CommonErrorCode.WEATHER_QUERY_FAILED.getCode(), exception.getCode());
        }
    }

    // ======================== 辅助方法 ========================

    private WeatherResponse buildWeatherResponse() {
        WeatherResponse response = new WeatherResponse();
        response.setCode(200);
        response.setGuo("中国");
        response.setSheng("浙江");
        response.setShi("杭州");
        response.setName("杭州");
        response.setWeather1("晴");
        response.setTemp("25");
        return response;
    }

    private WeatherResponse buildErrorWeatherResponse() {
        WeatherResponse response = new WeatherResponse();
        response.setCode(500);
        response.setMessage("Internal Server Error");
        return response;
    }
}

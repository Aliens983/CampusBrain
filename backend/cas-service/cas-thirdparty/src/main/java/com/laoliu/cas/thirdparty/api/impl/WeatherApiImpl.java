package com.laoliu.cas.thirdparty.api.impl;

import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.thirdparty.api.WeatherApi;
import com.laoliu.cas.thirdparty.interfaces.dto.response.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author forever-king
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherApiImpl implements WeatherApi {

    private final RestTemplate restTemplate;

    @Value("${weather.api.url}")
    private String weatherApiUrl;

    @Value("${weather.api.id}")
    private String apiId;

    @Value("${weather.api.key}")
    private String apiKey;

    @Override
    @Cacheable(value = "weather", key = "#sheng + ':' + #place")
    public WeatherResponse getWeather(String sheng, String place) {
        try {
            log.info("调用天气API - 省份: {}, 城市: {}", sheng, place);

            // 用 UriComponentsBuilder 显式 URL 编码，避免中文参数在 URL 中被错误处理
            String url = UriComponentsBuilder.fromHttpUrl(weatherApiUrl)
                    .queryParam("id", apiId)
                    .queryParam("key", apiKey)
                    .queryParam("sheng", sheng)
                    .queryParam("place", place)
                    .build()
                    .encode()
                    .toUriString();

            log.info("请求URL: {}", url);

            WeatherResponse response = restTemplate.getForObject(url, WeatherResponse.class);

            if (response == null || response.getCode() != 200) {
                log.warn("天气API返回异常: response={}", response);
                throw new BusinessException(500, "获取天气信息失败");
            }

            return response;
        } catch (Exception e) {
            log.error("获取天气信息失败", e);
            throw new BusinessException(CommonErrorCode.WEATHER_QUERY_FAILED);
        }
    }
}

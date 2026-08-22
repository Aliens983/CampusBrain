package com.laoliu.cas.thirdparty.interfaces.controller;

import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.thirdparty.api.WeatherApi;
import com.laoliu.cas.thirdparty.interfaces.dto.response.WeatherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author forever-king
 */
@Slf4j
@Tag(name = "天气接口")
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherApi weatherApi;
    private final RestTemplate restTemplate;

    @Operation(summary = "获取天气信息", description = "根据省份和城市名称获取天气信息")
    @GetMapping
    public CommonResult<WeatherResponse> getWeatherInfo(
            @Parameter(description = "省份名称", required = true) @RequestParam String sheng,
            @Parameter(description = "城市名称", required = true) @RequestParam String place) {
        if (!StringUtils.hasText(sheng) || !StringUtils.hasText(place)) {
            return CommonResult.badRequest("省份和城市参数不能为空");
        }
        return CommonResult.success(weatherApi.getWeather(sheng, place));
    }

    @Operation(summary = "根据IP自动定位获取天气", description = "通过服务端出口IP自动定位城市并返回天气")
    @GetMapping("/local")
    public CommonResult<WeatherResponse> getLocalWeather() {
        try {
            // myip.ipip.net — 国内可用的 IP 定位服务
            String resp = restTemplate.getForObject("https://myip.ipip.net", String.class);
            log.info("IP 定位响应: {}", resp);

            // 解析 "当前 IP：x.x.x.x  来自于：中国 浙江 杭州  移动"
            String region = null, city = null;
            if (resp != null && resp.contains("来自于：")) {
                String addr = resp.substring(resp.indexOf("来自于：") + 4).trim();
                String[] parts = addr.split("\\s+");
                if (parts.length >= 3) {
                    region = parts[1];
                    city = parts[2];
                }
            }

            if (!StringUtils.hasText(region) || !StringUtils.hasText(city)) {
                return CommonResult.badRequest("无法获取服务端位置信息");
            }
            log.info("定位结果: {} {}", region, city);
            return CommonResult.success(weatherApi.getWeather(region, city));
        } catch (Exception e) {
            log.error("获取天气失败", e);
            return CommonResult.internalServerError("获取天气信息失败");
        }
    }
}

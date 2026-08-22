package com.laoliu.cas.thirdparty.api;

import com.laoliu.cas.thirdparty.interfaces.dto.response.WeatherResponse;

/**
 * 天气查询 API。
 *
 * @author forever-king
 */
public interface WeatherApi {

    WeatherResponse getWeather(String sheng, String place);
}

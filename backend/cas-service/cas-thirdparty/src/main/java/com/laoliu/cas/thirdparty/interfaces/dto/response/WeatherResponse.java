package com.laoliu.cas.thirdparty.interfaces.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author forever-king
 */
@Data
@Schema(description = "天气响应VO")
public class WeatherResponse {

    @Schema(description = "响应状态码")
    private Integer code;

    @Schema(description = "国家")
    private String guo;

    @Schema(description = "省份")
    private String sheng;

    @Schema(description = "城市")
    private String shi;

    @Schema(description = "区县")
    private String qu;

    @Schema(description = "天气名称")
    private String name;

    @Schema(description = "当前天气")
    private String weather1;

    @Schema(description = "当前天气图标")
    private String weather1Img;

    @Schema(description = "当前温度")
    private String temp;

    @Schema(description = "提示信息")
    private String message;
}

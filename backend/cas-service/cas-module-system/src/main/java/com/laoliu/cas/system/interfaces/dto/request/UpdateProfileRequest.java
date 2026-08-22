package com.laoliu.cas.system.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新个人资料请求。
 *
 * @author forever-king
 */
@Data
@Schema(description = "更新个人资料请求")
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "用户名长度必须在2-50之间")
    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "年级", example = "大一")
    private String grade;

    @Schema(description = "性别", example = "男")
    private String sex;

    @Min(value = 1, message = "年龄必须大于0")
    @Max(value = 150, message = "年龄不能超过150")
    @Schema(description = "年龄", example = "18")
    private Integer age;
}

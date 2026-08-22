package com.laoliu.cas.common.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页参数基类 — 所有分页请求 VO 继承此类即可获得 pageNo / pageSize 参数。
 *
 * @author forever-king
 */
@Data
@Schema(description = "分页参数")
public class PageParam implements Serializable {

    private static final Integer DEFAULT_PAGE_NO = 1;
    private static final Integer DEFAULT_PAGE_SIZE = 10;

    /** 不分页时使用此值，查询全部数据（谨慎使用） */
    public static final Integer PAGE_SIZE_NONE = -1;

    @Schema(description = "页码，从 1 开始", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = DEFAULT_PAGE_NO;

    @Schema(description = "每页条数，最大值为 200", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 200, message = "每页条数最大值为 200")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

}

package com.laoliu.cas.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应体。
 *
 * @param <T> 分页记录类型
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应")
public class PageResult<T> {

    @Schema(description = "当前页记录列表")
    private List<T> records;

    @Schema(description = "总记录数", example = "100")
    private long total;

    @Schema(description = "每页大小", example = "10")
    private long pageSize;

    @Schema(description = "当前页码", example = "1")
    private long current;

    @Schema(description = "总页数", example = "10")
    private long pages;

    /**
     * 快速构造（仅 records + total，其余字段为 0）。
     */
    public PageResult(List<T> records, long total) {
        this.records = records;
        this.total = total;
        this.pageSize = 0;
        this.current = 0;
        this.pages = 0;
    }

    /**
     * 从 MyBatis-Plus 的 IPage 对象构建 PageResult。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return PageResult.<T>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .pageSize(page.getSize())
                .current(page.getCurrent())
                .pages(page.getPages())
                .build();
    }

    /**
     * 构建空结果。
     */
    public static <T> PageResult<T> empty(long pageSize, long current) {
        return PageResult.<T>builder()
                .records(Collections.emptyList())
                .total(0)
                .pageSize(pageSize)
                .current(current)
                .pages(0)
                .build();
    }
}

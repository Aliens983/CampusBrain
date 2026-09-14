package com.laoliu.cas.infra.interfaces.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author forever-king
 */
@Data
@Schema(description = "文件上传请求")
public class FileUploadRequest {

    @NotNull(message = "文件不能为空")
    @Schema(description = "待上传的文件")
    private MultipartFile file;

    /**
     * 子目录只允许单层、字母数字下划线短横，最长 32 字符。
     * 从入口层杜绝 {@code ../} 路径穿越（service 层另有纵深校验，4.1.14）。
     * 不传（null）时落在 uploads 根目录。
     */
    @Pattern(regexp = "[a-zA-Z0-9_-]{1,32}", message = "subDir 只能包含字母、数字、下划线、短横，长度 1-32")
    @Schema(description = "存放子目录（如 carousel），不传则落在 uploads 根目录")
    private String subDir;
}

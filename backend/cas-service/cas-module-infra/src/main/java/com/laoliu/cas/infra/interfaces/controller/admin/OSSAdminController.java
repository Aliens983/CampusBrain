package com.laoliu.cas.infra.interfaces.controller.admin;

import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.thirdparty.application.service.OSSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理员端对象存储接口。
 *
 * @author forever-king
 */
@Tag(name = "对象存储（管理）")
@RestController
@RequestMapping("/admin/files")
@RequiredArgsConstructor
public class OSSAdminController {

    private final OSSService ossService;

    @Operation(summary = "上传文件到OSS", description = "上传文件到阿里云OSS对象存储，返回文件的访问URL地址")
    @PostMapping("/oss")
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<String> upload(
            @Parameter(description = "待上传的文件", required = true) @RequestParam("file") MultipartFile file) {
        String url = ossService.uploadFile(file);
        return CommonResult.success(url);
    }
}

package com.laoliu.cas.infra.interfaces.controller.admin;

import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.infra.application.service.FileService;
import com.laoliu.cas.infra.interfaces.dto.request.FileUploadReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员端文件管理接口。
 *
 * @author forever-king
 */
@Tag(name = "文件管理（管理）")
@RestController
@RequestMapping("/admin/files")
@RequiredArgsConstructor
public class FileAdminController {

    private final FileService fileService;

    @Operation(summary = "上传文件", description = "上传本地文件并获取文件的访问URL地址")
    @PostMapping
    @RequireRole({UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<String> uploadFile(@Validated FileUploadReqVO fileUploadReqVO) {
        String fileUrl = fileService.uploadFile(fileUploadReqVO.getFile());
        return CommonResult.success(fileUrl);
    }
}

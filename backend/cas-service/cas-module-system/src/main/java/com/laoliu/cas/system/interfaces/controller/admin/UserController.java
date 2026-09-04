package com.laoliu.cas.system.interfaces.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.common.annotation.RequireRole;
import com.laoliu.cas.common.api.GetUserIdViaTokenApi;
import com.laoliu.cas.common.pojo.PageParam;
import com.laoliu.cas.system.domain.entity.User;
import com.laoliu.cas.common.enums.UserRoleEnum;
import com.laoliu.cas.common.exception.BusinessException;
import com.laoliu.cas.common.exception.code.CommonErrorCode;
import com.laoliu.cas.common.result.CommonResult;
import com.laoliu.cas.common.result.PageResult;
import com.laoliu.cas.common.util.PasswordUtils;
import com.laoliu.cas.system.application.service.NotificationSettingsService;
import com.laoliu.cas.system.application.service.UserService;
import com.laoliu.cas.system.domain.repository.UserRepository;
import com.laoliu.cas.system.interfaces.convert.UserConvert;
import com.laoliu.cas.system.interfaces.dto.NotifyPrefDTO;
import com.laoliu.cas.system.interfaces.dto.request.AdminCreateUserRequest;
import com.laoliu.cas.system.interfaces.dto.request.ChangePasswordRequest;
import com.laoliu.cas.system.interfaces.dto.request.UpdateProfileRequest;
import com.laoliu.cas.system.interfaces.dto.request.UserPageReqVO;
import com.laoliu.cas.system.interfaces.dto.response.UserInfoAndServicesViaMPRespVO;
import com.laoliu.cas.system.interfaces.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;




/**
 * @author forever-king
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "用户接口")
public class UserController {

    private final UserRepository userRepository;
    private final GetUserIdViaTokenApi getUserIdViaTokenApi;
    private final UserService userService;
    private final NotificationSettingsService notificationSettings;

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的基本信息，包含用户名、邮箱、角色等")
    @GetMapping({"/", "/me"})
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<UserResponse> getUserByParseToken() {
        try {
            Long userId = getUserIdViaTokenApi.getUserId();
            if (userId == null) {
                return CommonResult.unauthorized("用户未登录或登录已过期");
            }
            log.info("获取用户信息成功，用户 ID：{}", userId);
            User user = userRepository.findById(userId).orElse(null);
            log.info("User :{}", user);
            return CommonResult.success(UserConvert.INSTANCE.convert(user));
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }
    }

    @Operation(summary = "获取所有用户列表（分页+筛选）", description = "管理员分页获取用户列表，支持按姓名、邮箱模糊搜索和角色筛选")
    @GetMapping("/list")
    @RequireRole(UserRoleEnum.ADMIN)
    public CommonResult<PageResult<UserResponse>> getAllUsers(@Valid UserPageReqVO reqVO) {
        try {
            IPage<User> userPage = userRepository.getAllUsers(
                    reqVO.getPageNo(), reqVO.getPageSize(),
                    reqVO.getName(), reqVO.getEmail(), reqVO.getRole());
            PageResult<User> pageResult = PageResult.of(userPage);
            return CommonResult.success(UserConvert.INSTANCE.convertPage(pageResult));
        } catch (Exception e) {
            log.error("获取所有用户失败", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }
    }

    @Operation(summary = "创建新用户", description = "超级管理员创建新用户，需要提供用户名、邮箱、密码等基本信息，参数校验由 Bean Validation 自动完成")
    @PostMapping
    @RequireRole(UserRoleEnum.SUPER_ADMIN)
    public CommonResult<String> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        try {
            Long existUserId = userRepository.getUserIdByEmail(request.getEmail());
            if (existUserId != null) {
                return CommonResult.badRequest("该邮箱已被注册");
            }

            User user = User.builder()
                    .name(request.getName()).email(request.getEmail())
                    .password(PasswordUtils.encode(request.getPassword()))
                    .grade(request.getGrade()).sex(request.getSex())
                    .age(request.getAge())
                    .role(request.getRole() != null ? request.getRole() : 0)
                    .build();

            userRepository.save(user);

            return CommonResult.success("创建用户成功");
        } catch (Exception e) {
            log.error("创建用户失败", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }
    }

    @Operation(summary = "更新个人资料", description = "当前登录用户更新姓名、年级、性别、年龄")
    @PutMapping("/me")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getUserIdViaTokenApi.getUserId();
        if (userId == null) {
            return CommonResult.unauthorized("用户未登录或登录已过期");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return CommonResult.notFound("用户不存在");
        }
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getGrade() != null) {
            user.setGrade(request.getGrade());
        }
        if (request.getSex() != null) {
            user.setSex(request.getSex());
        }
        if (request.getAge() != null) {
            user.setAge(request.getAge());
        }
        userRepository.save(user);
        return CommonResult.success("更新成功", null);
    }

    @Operation(summary = "获取我的通知偏好", description = "获取当前用户的邮件/站内通知接收偏好")
    @GetMapping("/me/notify")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<NotifyPrefDTO> getMyNotifyPref() {
        Long userId = getUserIdViaTokenApi.getUserId();
        if (userId == null) {
            return CommonResult.unauthorized("用户未登录或登录已过期");
        }
        NotifyPrefDTO dto = new NotifyPrefDTO(
                notificationSettings.isEmailOn(userId),
                notificationSettings.isSiteOn(userId));
        return CommonResult.success(dto);
    }

    @Operation(summary = "保存我的通知偏好", description = "保存当前用户的邮件/站内通知接收偏好")
    @PutMapping("/me/notify")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> saveMyNotifyPref(@RequestBody NotifyPrefDTO dto) {
        Long userId = getUserIdViaTokenApi.getUserId();
        if (userId == null) {
            return CommonResult.unauthorized("用户未登录或登录已过期");
        }
        if (dto.getEmailOn() != null) {
            notificationSettings.setEmailOn(userId, dto.getEmailOn());
        }
        if (dto.getSiteOn() != null) {
            notificationSettings.setSiteOn(userId, dto.getSiteOn());
        }
        return CommonResult.success("已保存", null);
    }

    @Operation(summary = "修改密码", description = "当前登录用户修改自己的密码，需要提供旧密码和新密码，参数校验由 Bean Validation 自动完成")
    @PutMapping("/password")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = getUserIdViaTokenApi.getUserId();
        if (userId == null) {
            return CommonResult.unauthorized("用户未登录或登录已过期");
        }

        String storedPassword = userRepository.getEncodePasswordById(userId);
        if (storedPassword == null) {
            return CommonResult.notFound("用户不存在");
        }
        if (!PasswordUtils.matches(request.getOldPassword(), storedPassword)) {
            return CommonResult.badRequest("旧密码不正确");
        }

        userRepository.updatePasswordById(userId, PasswordUtils.encode(request.getNewPassword()));
        return CommonResult.success("修改密码成功", null);
    }

    @GetMapping("/me/bookings")
    @Operation(summary = "用户查看自己预约的所有服务（分页）")
    @RequireRole({UserRoleEnum.USER, UserRoleEnum.ADMIN, UserRoleEnum.SUPER_ADMIN})
    public CommonResult<UserInfoAndServicesViaMPRespVO> getAllBookings(@Valid PageParam pageParam) {
        Long userId = getUserIdViaTokenApi.getUserId();
        var bookingsPage = userService.getUserBookings(userId, pageParam.getPageNo(), pageParam.getPageSize());
        UserInfoAndServicesViaMPRespVO respVO = new UserInfoAndServicesViaMPRespVO();
        respVO.setUser(userRepository.findById(userId).orElse(null));
        respVO.setBookings(bookingsPage.getRecords());
        return CommonResult.success(respVO);
    }
}

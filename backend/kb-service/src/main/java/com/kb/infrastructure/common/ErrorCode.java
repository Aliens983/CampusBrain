package com.kb.infrastructure.common;

import lombok.Getter;

/**
 * 统一业务错误码枚举。
 * <p>
 * 格式：{category}{sequence}，前端可根据 code 做精准错误处理。
 * <ul>
 *   <li>A — 认证/授权 (Auth)</li>
 *   <li>U — 用户 (User)</li>
 *   <li>D — 文档 (Document)</li>
 *   <li>Q — 问答 (QA)</li>
 *   <li>G — 通用/限流 (General)</li>
 *   <li>S — 系统 (System)</li>
 * </ul>
 * </p>
 * @author forever-king
 */
@Getter
public enum ErrorCode {

    // ---- 通用 ----
    SUCCESS(0, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    RATE_LIMITED("G001", "请求过于频繁，请稍后再试"),

    // ---- 认证 ----
    AUTH_BAD_CREDENTIALS("A001", "用户名或密码错误"),
    AUTH_TOKEN_EXPIRED("A002", "登录已过期，请重新登录"),
    AUTH_TOKEN_INVALID("A003", "无效的登录凭证"),

    // ---- 用户 ----
    USER_NOT_FOUND("U001", "用户不存在"),
    USER_USERNAME_EXISTS("U002", "用户名已存在"),
    USER_EMAIL_EXISTS("U003", "该邮箱已被注册"),
    USER_VERIFICATION_CODE_INVALID("U004", "验证码错误或已过期"),
    USER_PASSWORD_WEAK("U005", "密码强度不足，需包含大小写字母和数字"),

    // ---- 文档 ----
    DOCUMENT_NOT_FOUND("D001", "文档不存在"),
    DOCUMENT_PARSE_FAILED("D002", "文档解析失败"),
    DOCUMENT_UNSUPPORTED_TYPE("D003", "不支持的文件类型"),
    DOCUMENT_TOO_LARGE("D004", "文件大小超过限制"),

    // ---- 问答 ----
    QA_LLM_FAILED("Q001", "AI 回答生成失败，请稍后重试"),
    QA_RETRIEVAL_FAILED("Q002", "文档检索失败"),
    QA_NO_DOCUMENTS("Q003", "知识库中没有相关文档，请先上传文档"),

    // ---- 系统 ----
    SYSTEM_EXTERNAL_SERVICE_UNAVAILABLE("S001", "外部服务暂不可用，请稍后重试");

    // int or String
    private final Object code;
    private final String message;

    ErrorCode(Object code, String message) {
        this.code = code;
        this.message = message;
    }

}

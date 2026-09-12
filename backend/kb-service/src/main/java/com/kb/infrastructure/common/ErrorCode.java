package com.kb.infrastructure.common;

import lombok.Getter;

/**
 * 统一业务错误码枚举。
 * <p>
 * <b>契约（2026-09-12 统一）</b>：{@code code} 一律为 {@code Integer}，成功恒为 <b>200</b>，
 * 与 CAS 的 {@code CommonResult.code} 及前端 {@code request.ts} 的判断保持一致。
 * </p>
 * <p>
 * 数值区间：{@code 400/404/500} 沿用 HTTP 语义；其余按业务分类分段，
 * 末位与原字母码序号对应（A001 → 1001，U002 → 2002 …）。
 * <ul>
 *   <li>1xxx — 认证/授权 (Auth)</li>
 *   <li>2xxx — 用户 (User)</li>
 *   <li>3xxx — 文档 (Document)</li>
 *   <li>4xxx — 问答 (QA)</li>
 *   <li>5xxx — 通用/限流 (General)</li>
 *   <li>6xxx — 系统 (System)</li>
 * </ul>
 * </p>
 *
 * @author forever-king
 */
@Getter
public enum ErrorCode {

    // ---- 通用 ----
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    RATE_LIMITED(5001, "请求过于频繁，请稍后再试"),

    // ---- 认证 ----
    AUTH_BAD_CREDENTIALS(1001, "用户名或密码错误"),
    AUTH_TOKEN_EXPIRED(1002, "登录已过期，请重新登录"),
    AUTH_TOKEN_INVALID(1003, "无效的登录凭证"),

    // ---- 用户 ----
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_USERNAME_EXISTS(2002, "用户名已存在"),
    USER_EMAIL_EXISTS(2003, "该邮箱已被注册"),
    USER_VERIFICATION_CODE_INVALID(2004, "验证码错误或已过期"),
    USER_PASSWORD_WEAK(2005, "密码强度不足，需包含大小写字母和数字"),

    // ---- 文档 ----
    DOCUMENT_NOT_FOUND(3001, "文档不存在"),
    DOCUMENT_PARSE_FAILED(3002, "文档解析失败"),
    DOCUMENT_UNSUPPORTED_TYPE(3003, "不支持的文件类型"),
    DOCUMENT_TOO_LARGE(3004, "文件大小超过限制"),

    // ---- 问答 ----
    QA_LLM_FAILED(4001, "AI 回答生成失败，请稍后重试"),
    QA_RETRIEVAL_FAILED(4002, "文档检索失败"),
    QA_NO_DOCUMENTS(4003, "知识库中没有相关文档，请先上传文档"),

    // ---- 系统 ----
    SYSTEM_EXTERNAL_SERVICE_UNAVAILABLE(6001, "外部服务暂不可用，请稍后重试");

    /** 业务状态码，统一为数值：200 = 成功 */
    private final Integer code;

    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

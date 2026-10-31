package com.kb.domain.document;

import java.util.Locale;

/**
 * 文档可见范围.
 * <p>
 * PUBLIC：所有登录用户可检索（管理员上传的校园知识默认值）；
 * PRIVATE：仅 owner 本人可检索/查看.
 * </p>
 *
 * @author forever-king
 */
public enum DocumentVisibility {

    PUBLIC,
    PRIVATE;

    /**
     * 解析请求参数；空值按 PUBLIC 处理，非法值返回 null，由应用层转成参数错误，
     * 避免领域层依赖 Web/基础设施异常类型.
     */
    public static DocumentVisibility parseOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return PUBLIC;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

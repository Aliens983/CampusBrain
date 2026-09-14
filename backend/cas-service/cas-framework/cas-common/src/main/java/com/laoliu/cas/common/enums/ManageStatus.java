package com.laoliu.cas.common.enums;

import lombok.Getter;

/**
 * @author forever-king
 */

@Getter
public enum ManageStatus {
    SUBMIT(0, "已提交,待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核未通过"),
    CANCELLED(3, "已取消"),
    COMPLETED(4, "已完成");

    private final int code;
    private final String message;

    ManageStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 按状态码解析枚举；非法/未知码返回 null。
     * 供状态描述展示、状态机入参归一化统一复用，避免各处重复 switch 0..4。
     */
    public static ManageStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (ManageStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}

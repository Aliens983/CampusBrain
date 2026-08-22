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
    CANCELLED(3, "已取消");

    private final int code;
    private final String message;

    ManageStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

package com.laoliu.cas.common.enums;

import lombok.Getter;

/**
 * @author forever-king
 */

@Getter
public enum ServiceStatus {
    ONLINE(1, "已开启"),
    CLOSED(0, "已关闭");

    private final int code;
    private final String message;

    ServiceStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

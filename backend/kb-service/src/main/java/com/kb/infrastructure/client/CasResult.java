package com.kb.infrastructure.client;

import lombok.Data;

/**
 * CAS 统一响应包装（对应 CAS 的 CommonResult<T>）
 *
 * @author forever-king
 */
@Data
public class CasResult<T> {

    /** 业务状态码（CAS 中 200 为成功） */
    private Integer code;

    /** 提示消息 */
    private String message;

    /** 业务数据 */
    private T data;

    public boolean isSuccess() {
        return code != null && code == 200;
    }
}

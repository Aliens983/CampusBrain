package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 内容运营（轮播图等）错误码 (40030-40039)
 *
 * @author forever-king
 */
public interface ContentErrorCode {

    /** 1.5：轮播图数量上限（与 V1 预置数据解耦，阈值由 carousel.max-count 配置） */
    ErrorCode CAROUSEL_LIMIT_EXCEEDED = new ErrorCode(40030, "轮播图数量已达上限，请先删除部分");

}

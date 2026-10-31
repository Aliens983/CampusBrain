package com.laoliu.cas.appointment.domain.service;

/**
 * 预约数量等业务硬限制的唯一来源.
 *
 * @author forever-king
 */
public final class BookingLimits {

    /**
     * 单次设备借用数量上限.
     * <p>
     * 除防止 int 溢出（occupied + quantity）外，也避免一条请求把库存全部占满；
     * 真实库存通常个位数到几十，10000 已远高于合理借用量.
     */
    public static final int MAX_EQUIPMENT_QUANTITY = 10_000;

    private BookingLimits() {
    }
}

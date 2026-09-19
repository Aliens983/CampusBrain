package com.laoliu.cas.appointment.domain.view;

/**
 * 预约单身份引用（订单 / 归属用户 / 所属服务）。
 * <p>
 * 3.5：取消/完结链路发布 {@code BookingChanged} 事件前，用它拿到订单真实的
 * serviceId 与归属 userId——事件签名是 (userId, serviceId)，不能拿 orderId 顶替。
 *
 * @param orderId   订单 ID
 * @param userId    订单归属用户 ID
 * @param serviceId 所属服务 ID
 * @author forever-king
 */
public record BookingRef(Long orderId, Long userId, Long serviceId) {
}

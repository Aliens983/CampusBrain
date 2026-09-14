package com.laoliu.cas.appointment.application.service;

import com.laoliu.cas.appointment.interfaces.dto.request.RoomBookRequest;
import com.laoliu.cas.appointment.interfaces.dto.response.RoomResponse;

import java.util.List;

/**
 * 教室查询/教室时段预约应用服务接口
 *
 * @author forever-king
 */
public interface RoomService {

    /**
     * 查询某服务（空闲教室）下的教室列表
     *
     * @param serviceId 服务 ID
     * @return 教室列表
     */
    List<RoomResponse> listByService(Long serviceId);

    /**
     * 为指定用户预约教室时段
     *
     * @param userId 预约用户 ID
     * @param roomId 教室 ID
     * @param req    预约请求（含时段日期/起止时间）
     * @return 预约订单 ID
     */
    Long bookRoom(Long userId, Long roomId, RoomBookRequest req);
}

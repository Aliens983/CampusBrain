package com.laoliu.cas.appointment.interfaces.convert;

import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.appointment.domain.view.TimeSlotView;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.TimeSlotResponse;

import java.util.List;

/**
 * 领域读模型 → HTTP 响应 DTO 转换器（2.1）。
 * <p>
 * <b>唯一允许</b>把 {@link BookingQueryView}/{@link TimeSlotView} 映射为 interfaces 层
 * 响应对象的位置：Controller 出口调用，领域/应用层不再感知响应 DTO 的结构。
 * 字段一一显式拷贝（不用反射），新增字段时编译器与代码评审都能直接发现遗漏。
 *
 * @author forever-king
 */
public final class BookingViewConverter {

    private BookingViewConverter() {
    }

    /** 预约查询视图 → 服务状态响应 */
    public static ServiceStatusResponse toResponse(BookingQueryView v) {
        if (v == null) {
            return null;
        }
        ServiceStatusResponse r = new ServiceStatusResponse();
        r.setOrderId(v.getOrderId());
        r.setUserId(v.getUserId());
        r.setUsername(v.getUsername());
        r.setServiceName(v.getServiceName());
        r.setServiceDescribe(v.getServiceDescribe());
        r.setCampus(v.getCampus());
        r.setCreateTime(v.getCreateTime());
        r.setUpdateTime(v.getUpdateTime());
        r.setManageStatus(v.getManageStatus());
        r.setStatusDescription(v.getStatusDescription());
        r.setReason(v.getReason());
        r.setConsultantName(v.getConsultantName());
        r.setSlotDate(v.getSlotDate());
        r.setStartTime(v.getStartTime());
        r.setEndTime(v.getEndTime());
        r.setEquipmentName(v.getEquipmentName());
        r.setQuantity(v.getQuantity());
        r.setRoomName(v.getRoomName());
        return r;
    }

    /** 批量转换 */
    public static List<ServiceStatusResponse> toResponses(List<BookingQueryView> views) {
        return views.stream().map(BookingViewConverter::toResponse).toList();
    }

    /** 时段视图 → 时段响应 */
    public static TimeSlotResponse toResponse(TimeSlotView v) {
        if (v == null) {
            return null;
        }
        return TimeSlotResponse.builder()
                .slotId(v.getSlotId())
                .startTime(v.getStartTime())
                .endTime(v.getEndTime())
                .available(v.getAvailable())
                .build();
    }

    /** 批量转换 */
    public static List<TimeSlotResponse> toSlotResponses(List<TimeSlotView> views) {
        return views.stream().map(BookingViewConverter::toResponse).toList();
    }
}

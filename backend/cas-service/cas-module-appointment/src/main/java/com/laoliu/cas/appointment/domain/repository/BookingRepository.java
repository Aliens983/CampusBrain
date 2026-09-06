package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 预约/订单仓储接口
 *
 * @author forever-king
 */
public interface BookingRepository {

    /** 幂等插入预约，返回实际插入行数 */
    int insertServices(Long userId, List<Integer> serviceIds);

    /** 乐观锁扣减库存（容量充足才 +1），返回 0 表示容量满 */
    int decrementStock(Long serviceId);

    /** 释放库存（取消/审核拒绝时 -1，最低到 0） */
    int releaseStock(Long serviceId);

    /** 幂等插入咨询时段预约（带咨询师/时段链接），返回实际插入行数（0=重复提交） */
    int insertConsultationBooking(Long userId, Long serviceId, Long consultantId, Long slotId,
                                  LocalDate slotDate, String startTime, String endTime);

    /** 释放单个预约单占用的咨询时段（审核拒绝时调用；非咨询预约自动跳过） */
    int releaseSlotByOrderId(Long orderId);

    /** 释放当前用户一批预约单占用的咨询时段（取消预约时调用） */
    int releaseSlotsByBookingIds(Long userId, List<Long> bookingIds);

    /** 幂等插入设备借用（带设备/数量/窗口），返回实际插入行数（0=重复提交） */
    int insertEquipmentBooking(Long userId, Long serviceId, Long equipmentId, Integer quantity,
                               LocalDate date, String startTime, String endTime);

    /** 统计某设备某日时间段内已占用的台数（待审+已通过，用于防超借） */
    int sumEquipmentOverlap(Long equipmentId, LocalDate date, String startTime, String endTime);

    /** 到点自动归还：把已过结束时间且"已通过"的单置为已完成，返回处理条数 */
    int autoCompleteExpired();

    /** 幂等插入教室时段预约，返回实际插入行数（0=重复提交） */
    int insertRoomBooking(Long userId, Long serviceId, Long roomId, LocalDate date, String startTime, String endTime);

    /** 统计某教室某日某时段已被占用条数（>0=已被预约） */
    int countRoomOverlap(Long roomId, LocalDate date, String startTime, String endTime);

    /** 查询当前用户一批待审核预约单对应的服务 ID（用于回退库存，防他人/重复释放） */
    List<Long> selectServiceIdsByBookingIds(Long userId, List<Long> bookingIds);

    /** 查询单个预约单对应的服务 ID（用于审核拒绝回退） */
    Long selectServiceIdByOrderId(Long orderId);

    int cancelBookings(Long userId, List<Long> bookingIds);

    List<ServiceStatusResponse> getServiceStatus();

    /**
     * 分页查询所有服务预约状态（支持按审核状态、服务名称筛选）
     */
    IPage<ServiceStatusResponse> getServiceStatus(int page, int pageSize, Integer manageStatus, String serviceName);

    List<ServiceStatusResponse> getServiceStatusByUserId(Long userId);

    /**
     * 分页查询用户的预约状态（支持按审核状态、服务名称筛选）
     */
    IPage<ServiceStatusResponse> getServiceStatusByUserId(Long userId, int page, int pageSize, Integer manageStatus, String serviceName);

    ServiceStatusResponse getServiceStatusByOrderId(Long orderId);

    /** 按用户与订单 ID 查询预约详情（用户端越权防护） */
    ServiceStatusResponse getServiceStatusByOrderIdAndUserId(Long userId, Long orderId);

    boolean auditService(Long orderId, Integer status, String reason);

    String getUserEmailByOrderId(Long orderId);

    /** 统计各服务的有效预约数（serviceId → count），供 KB 助手查询实时余量 */
    Map<Long, Long> countBookingsByService();
}

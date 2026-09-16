package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.common.enums.ManageStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 预约/订单仓储接口
 *
 * @author forever-king
 */
public interface BookingRepository {

    /**
     * 幂等插入一批普通服务预约，返回本次实际新建订单的 orderId 列表（重复提交被去重的不包含）。
     * 调用方据此拿到真实订单号，不再靠"查用户最新一单"猜测。
     */
    List<Long> insertServices(Long userId, List<Integer> serviceIds);

    /** 乐观锁扣减库存（容量充足才 +1），返回 0 表示容量满 */
    int decrementStock(Long serviceId);

    /** 释放库存（通用下单去重回滚时 -1，最低到 0） */
    int releaseStock(Long serviceId);

    /**
     * 幂等插入咨询时段预约（带咨询师/时段链接），返回新订单 orderId；重复提交返回 null。
     */
    Long insertConsultationBooking(Long userId, Long serviceId, Long consultantId, Long slotId,
                                   LocalDate slotDate, String startTime, String endTime);

    /** 释放单个预约单占用的咨询时段（审核拒绝时调用；非咨询预约自动跳过） */
    int releaseSlotByOrderId(Long orderId);

    /** 审核拒绝时按订单释放库存：仅通用类预约命中，资源类预约（咨询/教室/设备）不扣不补（7.3.6） */
    int releaseStockByOrderId(Long orderId);

    /**
     * 幂等插入设备借用（带设备/数量/窗口），返回新订单 orderId；重复提交返回 null。
     */
    Long insertEquipmentBooking(Long userId, Long serviceId, Long equipmentId, Integer quantity,
                                LocalDate date, String startTime, String endTime);

    /** 统计某设备某日时间段内已占用的台数（待审+已通过，用于防超借） */
    int sumEquipmentOverlap(Long equipmentId, LocalDate date, String startTime, String endTime);

    /** 到点自动归还：把已过结束时间的已通过时段单、及已过 end_date 的无时段单置为已完成，返回处理条数 */
    int autoCompleteExpired();

    /**
     * 幂等插入教室时段预约，返回新订单 orderId；重复提交返回 null。
     */
    Long insertRoomBooking(Long userId, Long serviceId, Long roomId, LocalDate date, String startTime, String endTime);

    /** 统计某教室某日某时段已被占用条数（>0=已被预约） */
    int countRoomOverlap(Long roomId, LocalDate date, String startTime, String endTime);

    /**
     * 取消预约（原子完成状态置取消 + 通用单回补 booked_count + 咨询单释放时段，7.3.6）。
     * 仅当前用户的待审核单、或已通过的活动单命中；重复调用幂等。
     *
     * @return 实际取消的订单行数
     */
    int cancelBookings(Long userId, List<Long> bookingIds);

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

    /** 分页查询教师名下咨询师档期的申请（manageStatus 可空=全部） */
    IPage<ServiceStatusResponse> getTeacherBookings(Long teacherId, int page, int pageSize, Integer manageStatus);

    /** 查询预约单归属的咨询师绑定教师账号ID（无归属返回 null） */
    Long selectConsultantOwnerByOrderId(Long orderId);

    /** 免审直通：把刚提交的活动预约置为已通过 */
    int approveActivityBookings(Long userId, List<Integer> serviceIds);

    /**
     * 审核状态流转：仅当订单当前状态属于 {@code allowedFrom} 时才更新（状态机白名单）。
     *
     * @return true 表示命中并更新；false 表示订单不存在或状态非法跃迁
     */
    boolean auditService(Long orderId, Integer status, String reason, List<ManageStatus> allowedFrom);

    String getUserEmailByOrderId(Long orderId);

    /** 统计各服务的有效预约数（serviceId → count），供 KB 助手查询实时余量 */
    Map<Long, Long> countBookingsByService();
}

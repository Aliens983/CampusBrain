package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ItemDO;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServiceItemDO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceAvailabilityResponse;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * 预约订单 Mapper。
 * <p>
 * 约定：所有状态码都以 {@code xxxCode} 显式入参（由应用/仓储层用
 * {@link com.laoliu.cas.common.enums.ManageStatus} 枚举传入），SQL 中不写死 0/1/3/4；
 * 幂等窗口统一为 {@code dedupeSeconds}（来自 booking.dedupe-seconds 配置）。
 *
 * @author forever-king
 */
@Mapper
public interface ItemMapper extends BaseMapper<ItemDO> {

    /**
     * 幂等插入单个普通服务预约：同用户同服务仍有生效单（待审核/已通过）则不插入。
     * 成功时 orderId 通过 useGeneratedKeys 回填到入参 {@code item}。
     *
     * @return 实际插入行数（0=重复提交）
     */
    int insertSingleService(@Param("item") ItemDO item,
                            @Param("pendingCode") int pendingCode,
                            @Param("approvedCode") int approvedCode);

    /**
     * 幂等插入咨询时段预约。成功时 orderId 回填到 {@code item}。
     *
     * @return 实际插入行数（0=重复提交）
     */
    int insertConsultationBooking(@Param("item") ItemDO item,
                                  @Param("pendingCode") int pendingCode,
                                  @Param("dedupeSeconds") int dedupeSeconds);

    /**
     * 筛出当前用户给定订单中可取消（待审核单、或已通过的活动单）的订单 ID（12-09）。
     * WHERE 与 {@link #cancelByIdsAndRelease} 完全一致。
     */
    List<Long> selectCancellableOrderIds(@Param("userId") Long userId,
                                         @Param("orderIds") List<Long> orderIds,
                                         @Param("pendingCode") int pendingCode,
                                         @Param("approvedCode") int approvedCode);

    /**
     * 取消并释放资源（7.3.6）：一条多表 UPDATE 原子完成「状态置取消 + 通用单回补库存 +
     * 咨询单释放时段」，仅待审核单、或已通过的活动单命中。
     *
     * @return 实际取消的订单行数
     */
    int cancelByIdsAndRelease(@Param("userId") Long userId,
                              @Param("orderIds") List<Long> orderIds,
                              @Param("pendingCode") int pendingCode,
                              @Param("approvedCode") int approvedCode,
                              @Param("cancelledCode") int cancelledCode);

    /**
     * 分页查询所有服务预约状态（支持筛选）
     */
    IPage<ServiceStatusResponse> getServiceStatusWithPage(Page<?> page,
            @Param("manageStatus") Integer manageStatus,
            @Param("serviceName") String serviceName);

    List<ServiceStatusResponse> getServiceStatusByUserId(@Param("userId") Long userId);

    /**
     * 分页查询用户的预约状态（支持筛选）
     */
    IPage<ServiceStatusResponse> getServiceStatusByUserIdWithPage(@Param("userId") Long userId, Page<?> page,
            @Param("manageStatus") Integer manageStatus,
            @Param("serviceName") String serviceName);

    /**
     * 审核状态流转：仅当订单当前状态属于 {@code fromStatuses} 时才更新（状态机白名单）。
     *
     * @param status       目标状态码
     * @param fromStatuses 允许的来源状态码（由业务状态机决定）
     * @return 实际更新行数（0=状态非法跃迁/订单不存在）
     */
    int auditService(@Param("orderId") Long orderId,
                     @Param("status") int status,
                     @Param("reason") String reason,
                     @Param("fromStatuses") List<Integer> fromStatuses);

    ServiceStatusResponse getServiceStatusByOrderId(Long orderId);

    ServiceStatusResponse getServiceStatusByOrderIdAndUserId(@Param("userId") Long userId, @Param("orderId") Long orderId);

    /**
     * 分页查询某教师（咨询师绑定账号）名下咨询档期的申请
     */
    IPage<ServiceStatusResponse> getTeacherBookingsWithPage(@Param("teacherId") Long teacherId, Page<?> page,
            @Param("manageStatus") Integer manageStatus);

    /** 查询某预约单对应的咨询师绑定账号ID（无归属返回 null，用于教师越权校验） */
    Long selectConsultantOwnerByOrderId(@Param("orderId") Long orderId);

    /**
     * 免审直通：把当前用户 dedupeSeconds 秒内刚插入的「活动」待审核单置为已通过
     */
    int approveRecentActivityBookings(@Param("userId") Long userId,
                                      @Param("serviceIds") List<Integer> serviceIds,
                                      @Param("pendingCode") int pendingCode,
                                      @Param("approvedCode") int approvedCode,
                                      @Param("dedupeSeconds") int dedupeSeconds);

    String getUserEmailByOrderId(@Param("orderId") Long orderId);

    List<ServiceItemDO> selectUserServices(Long userId);

    /** 统计各服务的有效预约数（仅待审核+已通过），与 booked_count 扣减语义一致 */
    List<ServiceAvailabilityResponse> countBookingsByService(@Param("pendingCode") int pendingCode,
                                                             @Param("approvedCode") int approvedCode);

    /**
     * 乐观锁扣减库存：仅当容量足够时 +1（原子条件更新，防并发超卖）
     *
     * @return 影响行数，0 表示容量已满
     */
    int decrementStock(@Param("serviceId") Long serviceId);

    /** 释放库存：通用下单去重回滚时 -1（最低到 0） */
    int releaseStock(@Param("serviceId") Long serviceId);

    /** 审核拒绝：按订单释放库存，仅通用类预约命中（资源类预约从未扣减，7.3.6） */
    int releaseStockByOrderId(@Param("orderId") Long orderId);

    /** 释放单个预约单占用的咨询时段（审核拒绝时调用，非咨询预约自动跳过） */
    int releaseSlotByOrderId(@Param("orderId") Long orderId);

    /**
     * 幂等插入设备借用。成功时 orderId 回填到 {@code item}。
     *
     * @return 实际插入行数（0=重复提交）
     */
    int insertEquipmentBooking(@Param("item") ItemDO item,
                               @Param("pendingCode") int pendingCode,
                               @Param("dedupeSeconds") int dedupeSeconds);

    /** 统计某设备某日时间段内已占用的台数（待审+已通过） */
    int sumEquipmentOverlap(@Param("equipmentId") Long equipmentId,
                            @Param("date") java.time.LocalDate date,
                            @Param("startTime") String startTime,
                            @Param("endTime") String endTime,
                            @Param("pendingCode") int pendingCode,
                            @Param("approvedCode") int approvedCode);

    /**
     * 到点自动完结：已通过的时段单按结束时间、无时段的活动/通用单按 services.end_date 置为已完成。
     *
     * @return 实际完结条数
     */
    int autoCompleteExpired(@Param("approvedCode") int approvedCode,
                            @Param("completedCode") int completedCode);

    /**
     * 幂等插入教室时段预约。成功时 orderId 回填到 {@code item}。
     *
     * @return 实际插入行数（0=重复提交）
     */
    int insertRoomBooking(@Param("item") ItemDO item,
                          @Param("pendingCode") int pendingCode,
                          @Param("dedupeSeconds") int dedupeSeconds);

    /** 统计某教室某日某时段已被占用条数（待审+已通过；>0 表示已被他人预约） */
    int countRoomOverlap(@Param("roomId") Long roomId,
                         @Param("date") java.time.LocalDate date,
                         @Param("startTime") String startTime,
                         @Param("endTime") String endTime,
                         @Param("pendingCode") int pendingCode,
                         @Param("approvedCode") int approvedCode);
}

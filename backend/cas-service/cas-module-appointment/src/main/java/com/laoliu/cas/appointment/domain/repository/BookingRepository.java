package com.laoliu.cas.appointment.domain.repository;

import com.laoliu.cas.appointment.domain.view.BookingQueryView;
import com.laoliu.cas.appointment.domain.view.BookingRef;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.laoliu.cas.common.enums.ManageStatus;

import java.time.LocalDate;
import java.util.Collection;
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

    /**
     * 批量统计多台设备某日同窗各自被占用台数（4.7 N+1 收敛，助手设备列表一次 GROUP BY）。
     * 窗口内无占用的设备不出现在返回 Map 中，调用方按 0 兜底。
     */
    Map<Long, Integer> sumEquipmentOverlapBatch(Collection<Long> equipmentIds, LocalDate date,
                                                String startTime, String endTime);

    /** 到点自动归还：把已过结束时间的已通过时段单、及已过 end_date 的无时段单置为已完成，返回处理条数 */
    int autoCompleteExpired();

    /**
     * 幂等插入教室时段预约，返回新订单 orderId；重复提交返回 null。
     */
    Long insertRoomBooking(Long userId, Long serviceId, Long roomId, LocalDate date, String startTime, String endTime);

    /** 统计某教室某日某时段已被占用条数（>0=已被预约） */
    int countRoomOverlap(Long roomId, LocalDate date, String startTime, String endTime);

    /**
     * 批量统计多间教室某日同窗各自占用条数（4.7 N+1 收敛，助手教室列表一次 GROUP BY）。
     * 窗口内无占用的教室不出现在返回 Map 中，调用方按 0 兜底。
     */
    Map<Long, Integer> countRoomOverlapBatch(Collection<Long> roomIds, LocalDate date,
                                             String startTime, String endTime);

    /**
     * 筛出当前用户给定订单中"可取消"的订单引用（12-09 + 3.5）。
     * <p>
     * 可取消条件与原子 UPDATE 的 WHERE 完全一致：归属本人、ID 在入参集合内、
     * 且为待审核单，或已通过的活动单。该查询为 {@code FOR UPDATE} 锁定读（须在事务内）：
     * 调用期间命中行被锁定，返回集合即随后 UPDATE 的实际命中集合，
     * CANCELLED 事件据此逐单按真实 serviceId 发布，杜绝虚假事件与 serviceId 错传。
     */
    List<BookingRef> findCancellableBookings(Long userId, List<Long> orderIds);

    /**
     * 按已锁定的订单集合原子完成「状态置取消 + 通用单回补 booked_count +
     * 咨询单释放时段」（7.3.6）。入参必须来自同事务内的
     * {@link #findCancellableBookings}（FOR UPDATE 已锁定命中行），
     * 正常路径下集合内每一行都会被更新。
     * <p>
     * 注意：多表 UPDATE 返回值是各表匹配行之和（取消 1 单通常返回 2），
     * <b>不能</b>用作取消订单数，只能以 {@code > 0} 判定是否命中；
     * 事件集合与计数以 {@link #findCancellableBookings} 的返回为准（3.5.2）。
     *
     * @return 多表 UPDATE 各表匹配行之和；0 表示无命中
     */
    int cancelByIds(Long userId, List<Long> orderIds);

    /**
     * 管理员强制取消（3.4 僵尸单兜底）：不校验归属与活动分类，待审核/已通过的任意单
     * 置为已取消并释放占用（容量型回补 booked_count、咨询单释放时段）。
     *
     * @return true 表示命中并更新；false 表示订单不存在或已是终态（幂等）
     */
    boolean adminCancelAndRelease(Long orderId, String reason);

    /**
     * 管理员强制完结（3.4 僵尸单兜底）：待审核/已通过单置为已完成，
     * 容量型单回补 booked_count 解锁名额；时段型单不动时段。
     *
     * @return true 表示命中并更新；false 表示订单不存在或已是终态（幂等）
     */
    boolean adminCompleteAndRelease(Long orderId, String reason);

    /**
     * 分页查询所有服务预约状态（支持按审核状态、服务名称筛选）
     */
    IPage<BookingQueryView> getServiceStatus(int page, int pageSize, Integer manageStatus, String serviceName);

    List<BookingQueryView> getServiceStatusByUserId(Long userId);

    /**
     * 分页查询用户的预约状态（支持按审核状态、服务名称筛选）
     */
    IPage<BookingQueryView> getServiceStatusByUserId(Long userId, int page, int pageSize, Integer manageStatus, String serviceName);

    BookingQueryView getServiceStatusByOrderId(Long orderId);

    /** 按用户与订单 ID 查询预约详情（用户端越权防护） */
    BookingQueryView getServiceStatusByOrderIdAndUserId(Long userId, Long orderId);

    /** 分页查询教师名下咨询师档期的申请（manageStatus 可空=全部） */
    IPage<BookingQueryView> getTeacherBookings(Long teacherId, int page, int pageSize, Integer manageStatus);

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

package com.laoliu.cas.appointment.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ItemDO;
import com.laoliu.cas.appointment.infrastructure.persistence.dataobject.ServicesDO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceAvailabilityVO;
import com.laoliu.cas.appointment.interfaces.dto.response.ServiceStatusResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * @author forever-king
 */
@Mapper
public interface ItemMapper extends BaseMapper<ItemDO> {

    void setBookingStatus(@Param("userId") Long userId, @Param("bookingId") Long bookingId);

    /**
     * 幂等插入预约：60 秒内同用户对同一服务（待审核状态）不会重复插入
     *
     * @return 实际插入的行数（用于判断是否全部为重复提交）
     */
    int insertServices(@Param("userId") Long userId, @Param("serviceId") List<Integer> serviceId);

    int setBookingStatusByParts(@Param("userId") Long userId, @Param("bookingIds") List<Long> bookingIds);

    List<ServiceStatusResponse> getServiceStatus();

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

    int auditService(@Param("orderId") Long orderId, @Param("status") Integer status, @Param("reason") String reason);

    ServiceStatusResponse getServiceStatusByOrderId(Long orderId);

    ServiceStatusResponse getServiceStatusByOrderIdAndUserId(@Param("userId") Long userId, @Param("orderId") Long orderId);

    String getUserEmailByOrderId(@Param("orderId") Long orderId);

    List<ServicesDO> selectUserServices(Long userId);

    /** 统计各服务的有效预约数（仅待审核+已通过，与 booked_count 扣减语义一致） */
    @Select("SELECT service_id AS serviceId, COUNT(*) AS bookingCount FROM item WHERE manage_status IN (0, 1) GROUP BY service_id")
    List<ServiceAvailabilityVO> countBookingsByService();

    /**
     * 乐观锁扣减库存：仅当容量足够时 +1（原子条件更新，防并发超卖）
     *
     * @return 影响行数，0 表示容量已满
     */
    int decrementStock(@Param("serviceId") Long serviceId);

    /**
     * 释放库存：取消/审核拒绝时 -1（最低到 0）
     */
    int releaseStock(@Param("serviceId") Long serviceId);

    /** 查询当前用户一批待审核预约单对应的服务 ID（用于回退库存，防他人/重复释放） */
    List<Long> selectServiceIdsByBookingIds(@Param("userId") Long userId, @Param("bookingIds") List<Long> bookingIds);

    /** 查询单个预约单对应的服务 ID（用于审核拒绝回退） */
    Long selectServiceIdByOrderId(@Param("orderId") Long orderId);
}

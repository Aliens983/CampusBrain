package com.laoliu.cas.appointment.domain.view;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约订单查询视图（领域读模型，2.1）。
 * <p>
 * 仓储/应用层在领域边界内传递的只读投影，字段由 Mapper 的联表查询直接填充。
 * 不携带任何 Web/Swagger 注解；对外 HTTP 响应由 interfaces 层的
 * {@code BookingViewConverter} 转换为 {@code ServiceStatusResponse}，
 * 保证 domain 不再 compile-time 依赖 interfaces 的 DTO。
 *
 * @author forever-king
 */
@Data
public class BookingQueryView {

    /** 订单ID */
    private Long orderId;

    /** 所属服务ID（3.5：取消/完结事件需要按真实 serviceId 发布） */
    private Long serviceId;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 服务名称 */
    private String serviceName;

    /** 服务描述 */
    private String serviceDescribe;

    /** 校区: cq仓前 / xs下沙 */
    private String campus;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 请求状态（0-待审核，1-通过，2-拒绝，3-取消，4-完结） */
    private Integer manageStatus;

    /** 服务状态码描述（应用层按枚举填充） */
    private String statusDescription;

    /** 审核拒绝/处理原因 */
    private String reason;

    /** 咨询师姓名（咨询时段预约时非空） */
    private String consultantName;

    /** 咨询预约日期（yyyy-MM-dd） */
    private LocalDate slotDate;

    /** 咨询时段开始 HH:mm */
    private String startTime;

    /** 咨询时段结束 HH:mm */
    private String endTime;

    /** 设备名称（设备借用时非空） */
    private String equipmentName;

    /** 设备借用数量 */
    private Integer quantity;

    /** 教室名称（教室时段预约时非空） */
    private String roomName;
}

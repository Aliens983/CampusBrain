package com.kb.infrastructure.client;

import lombok.Data;

/**
 * CAS 用户预约记录（对应 CAS 的 ServiceStatusResponse）
 *
 * @author forever-king
 */
@Data
public class CasBooking {

    /** 预约单号 */
    private Long orderId;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 服务名称 */
    private String serviceName;

    /** 服务描述 */
    private String serviceDescribe;

    /** 校区编码：cq 仓前 / xs 下沙 */
    private String campus;

    /** 审核状态：0待审/1通过/2拒绝/3取消/4完成 */
    private Integer manageStatus;

    /** 状态描述 */
    private String statusDescription;

    /** 拒绝原因 */
    private String reason;

    /** 咨询师姓名（咨询时段预约时非空） */
    private String consultantName;

    /** 预约日期 yyyy-MM-dd（用 String 接收，避免跨服务日期格式解析差异） */
    private String slotDate;

    /** 开始时间 HH:mm */
    private String startTime;

    /** 结束时间 HH:mm */
    private String endTime;

    /** 设备名称（设备借用时非空） */
    private String equipmentName;

    /** 借用数量 */
    private Integer quantity;

    /** 教室名称（教室时段预约时非空） */
    private String roomName;
}

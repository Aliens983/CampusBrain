package com.kb.infrastructure.client;

import lombok.Data;

/**
 * CAS 用户预约记录（对应 CAS 的 ServiceStatusResponse）。
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

    /** 审核状态：0待审/1通过/2拒绝/3取消 */
    private Integer manageStatus;

    /** 状态描述 */
    private String statusDescription;

    /** 拒绝原因 */
    private String reason;
}

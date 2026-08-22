package com.laoliu.cas.system.infrastructure.persistence.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 预约记录查询结果 DO — 对应 item JOIN services JOIN user 的查询结果。
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRecordDO {

    private Long orderId;
    private String serviceName;
    private String serviceDescribe;
    private String username;
    private Date createTime;
    private Date updateTime;
    private Integer manageStatus;
    private String statusDescription;

}

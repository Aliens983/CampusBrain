package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

/**
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("item")
public class ItemDO {

    @TableId(type = IdType.AUTO)
    private Integer orderId;

    /** 用户ID */
    private Long userId;

    /** 服务ID */
    private Integer serviceId;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 管理状态（0-待审核，1-通过，2-拒绝，3-取消） */
    private Integer manageStatus;

    /** 审核原因/申请理由 */
    private String reason;
}

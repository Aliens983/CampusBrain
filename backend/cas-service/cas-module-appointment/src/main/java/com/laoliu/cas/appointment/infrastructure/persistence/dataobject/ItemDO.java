package com.laoliu.cas.appointment.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

/**
 * 预约订单 DO。
 * <p>
 * 既是 item 表的映射，也作为四类下单 insert（普通服务/咨询/设备/教室）的统一入参载体：
 * insert 成功后 orderId 由 useGeneratedKeys 回填，业务层据此拿到真实订单号。
 * 预约要素字段（consultantId/slotId/slotDate/startTime/endTime/equipmentId/quantity/roomId）
 * 对不需要的下单场景保持 null，由对应 SQL 仅写入相关列。
 *
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

    /** 管理状态（0-待审核，1-通过，2-拒绝，3-取消，4-完成） */
    private Integer manageStatus;

    /** 审核原因/申请理由 */
    private String reason;

    /** 咨询师ID（咨询时段预约时非空） */
    private Long consultantId;

    /** 时段ID（咨询时段预约时非空） */
    private Long slotId;

    /** 预约日期（咨询/设备/教室） */
    private LocalDate slotDate;

    /** 时段开始 HH:mm */
    private String startTime;

    /** 时段结束 HH:mm */
    private String endTime;

    /** 设备ID（设备借用时非空） */
    private Long equipmentId;

    /** 借用数量 */
    private Integer quantity;

    /** 教室ID（教室时段预约时非空） */
    private Long roomId;
}

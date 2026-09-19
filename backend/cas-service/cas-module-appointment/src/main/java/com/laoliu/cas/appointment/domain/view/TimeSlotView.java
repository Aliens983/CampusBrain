package com.laoliu.cas.appointment.domain.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 咨询师可用时段查询视图（领域读模型，2.1）。
 * <p>
 * 仓储联表查询的只读投影；对外 HTTP 响应由 interfaces 层转换为
 * {@code TimeSlotResponse}，避免 domain 反向依赖 interfaces DTO。
 *
 * @author forever-king
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotView {

    /** 时段ID */
    private Long slotId;

    /** 开始时间 HH:mm */
    private String startTime;

    /** 结束时间 HH:mm */
    private String endTime;

    /** 是否可用（Mapper 输出字符串，与原 TimeSlotResponse.available 口径一致） */
    private String available;
}

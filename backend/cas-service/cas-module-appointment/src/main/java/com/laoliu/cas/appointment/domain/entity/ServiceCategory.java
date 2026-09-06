package com.laoliu.cas.appointment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务业务分类（固定 4 类，代码级外键目标）
 * <p>
 * teacher 教师咨询 · equipment 设备借用 · space 教室空间 · activity 活动报名。
 * 由 Flyway V4 种子维护，当前不开放管理端增删改。
 *
 * @author forever-king
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类ID */
    private Long id;

    /** 分类编码（与 services 的业务判断共用，如 activity 免审直通） */
    private String code;

    /** 分类中文名 */
    private String name;

    /** 展示排序 */
    private Integer sort;
}

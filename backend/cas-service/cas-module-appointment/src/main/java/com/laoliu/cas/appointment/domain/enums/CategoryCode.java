package com.laoliu.cas.appointment.domain.enums;

/**
 * 服务业务分类编码（与 service_category 表 V4 种子、services.category_id 对应）。
 * <p>
 * 此前业务侧靠在服务名里匹配"咨询/辅导/设备"等关键词来猜分类（3.1.6），
 * 一旦运营改个名字就会路由错下单链路；现统一以 categoryCode 显式判断。
 *
 * @author forever-king
 */
public enum CategoryCode {

    /** 教师咨询 */
    TEACHER("teacher"),
    /** 设备借用 */
    EQUIPMENT("equipment"),
    /** 教室空间 */
    SPACE("space"),
    /** 活动报名（免审直通） */
    ACTIVITY("activity");

    private final String code;

    CategoryCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 判断给定分类编码是否为本枚举值 */
    public boolean is(String code) {
        return this.code.equals(code);
    }
}

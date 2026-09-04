package com.laoliu.cas.system.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 全局通知策略表（单行 id=1）Mapper。
 * <p>对应 DDL：cas-service/sql/10_notification_settings.sql 中的 notification_policy 表。</p>
 *
 * @author forever-king
 */
@Mapper
public interface NotificationPolicyMapper {

    @Select("SELECT email_enabled FROM notification_policy WHERE id = 1")
    Integer selectEmailEnabled();

    @Select("SELECT site_enabled FROM notification_policy WHERE id = 1")
    Integer selectSiteEnabled();

    @Select("SELECT sms_enabled FROM notification_policy WHERE id = 1")
    Integer selectSmsEnabled();

    @Update("UPDATE notification_policy SET email_enabled = #{on} WHERE id = 1")
    void updateEmailEnabled(@Param("on") boolean on);

    @Update("UPDATE notification_policy SET site_enabled = #{on} WHERE id = 1")
    void updateSiteEnabled(@Param("on") boolean on);

    @Update("UPDATE notification_policy SET sms_enabled = #{on} WHERE id = 1")
    void updateSmsEnabled(@Param("on") boolean on);
}

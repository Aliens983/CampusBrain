package com.laoliu.cas.appointment.application.service;

/**
 * 审核动作的发起方（审核人身份）。
 * <p>
 * 3.1.8：同一笔预约可由管理员或预约所对应的咨询师（教师）审核，
 * 发给用户的通知邮件需据此区分审核人措辞，避免教师审核时仍显示"管理员"。
 *
 * @author forever-king
 */
public enum AuditSource {

    /** 管理员（管理端审核台） */
    ADMIN("管理员"),

    /** 咨询师/教师（仅能审核名下咨询档期的预约） */
    TEACHER("咨询师（教师）");

    private final String reviewerLabel;

    AuditSource(String reviewerLabel) {
        this.reviewerLabel = reviewerLabel;
    }

    public String reviewerLabel() {
        return reviewerLabel;
    }
}

package com.laoliu.cas.common.exception.code;

import com.laoliu.cas.common.exception.ErrorCode;

/**
 * 咨询沟通（学生 ⇄ 教师 1:1 在线留言）错误码
 *
 * @author forever-king
 */
public interface ChatErrorCode {

    /** 仅支持与「教师咨询」类别的咨询教师发起沟通 */
    ErrorCode PEER_NOT_CONSULT_TEACHER = new ErrorCode(40020, "仅支持与教师咨询类咨询师沟通");

    /** 教师只能与自己名下咨询过的学生发起会话 */
    ErrorCode PEER_NOT_CONSULTED = new ErrorCode(40021, "对方尚未向你咨询过，无法发起会话");

    /** 消息内容不能为空 */
    ErrorCode MESSAGE_CONTENT_BLANK = new ErrorCode(40022, "消息内容不能为空");

    /** 会话不存在或无权访问 */
    ErrorCode CONVERSATION_NOT_FOUND = new ErrorCode(40023, "会话不存在或无权访问");

    /** 非咨询预约，不开放沟通 */
    ErrorCode BOOKING_NOT_CONSULT = new ErrorCode(40024, "仅咨询类预约支持在线沟通");

    /** 仅普通用户(学生)/教师可参与咨询沟通 */
    ErrorCode ROLE_NOT_ALLOWED = new ErrorCode(40025, "当前角色不支持咨询沟通");

    /** 不能和自己发起会话 */
    ErrorCode SELF_CHAT = new ErrorCode(40026, "不能和自己发起会话");

    /** 预约单不存在或无权限 */
    ErrorCode BOOKING_NOT_FOUND = new ErrorCode(40027, "预约单不存在或无权限访问");

}

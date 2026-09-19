package com.laoliu.cas.appointment.application.service;

/**
 * 轮播图图片上传载体（2.10）。
 * <p>
 * 应用层不得依赖 Servlet 的 {@code MultipartFile}，否则定时任务/消息消费/单测
 * 都无法复用应用服务。interfaces 层（Controller）负责把 MultipartFile
 * 的原始文件名与字节读入本载体，应用层只面对纯数据。
 *
 * @param originalFilename 原始文件名（仅用于扩展名白名单校验，存储名由存储层重新生成）
 * @param content          文件字节
 * @author forever-king
 */
public record CarouselImage(String originalFilename, byte[] content) {
}

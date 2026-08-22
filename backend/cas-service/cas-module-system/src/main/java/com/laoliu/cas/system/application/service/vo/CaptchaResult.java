package com.laoliu.cas.system.application.service.vo;

import lombok.Builder;
import lombok.Data;

/**
 * @author forever-king
 */
@Data
@Builder
public class CaptchaResult {

    private String uuid;

    private String imageUrl;
}

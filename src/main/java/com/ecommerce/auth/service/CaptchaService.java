package com.ecommerce.auth.service;

import java.util.Map;

public interface CaptchaService {

    /**
     * Generates a new CAPTCHA challenge with distorted image and unique challenge ID.
     * @return Map containing "captchaId" and "captchaImageBase64"
     */
    Map<String, String> generateCaptcha();

    /**
     * Validates client answer against cached challenge in Redis.
     * Automatically invalidates challenge to prevent replay attacks.
     */
    boolean validateCaptcha(String captchaId, String userAnswer);
}

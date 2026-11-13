package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate redisTemplate;

    @Value("${application.security.captcha.expiration-seconds:180}")
    private int captchaExpirationSeconds;

    private static final String CAPTCHA_PREFIX = "sentinel:captcha:";
    private static final String CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excludes easily confused chars (0/O, 1/I)
    private static final int CODE_LENGTH = 6;
    private static final int WIDTH = 180;
    private static final int HEIGHT = 50;

    private final SecureRandom random = new SecureRandom();

    @Override
    public Map<String, String> generateCaptcha() {
        String captchaId = UUID.randomUUID().toString();
        String captchaCode = generateRandomCode(CODE_LENGTH);

        // Store solution in Redis with TTL
        redisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaId,
                captchaCode.toUpperCase(),
                Duration.ofSeconds(captchaExpirationSeconds)
        );

        String base64Image = renderCaptchaImage(captchaCode);

        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("captchaImageBase64", "data:image/png;base64," + base64Image);
        result.put("expiresInSeconds", String.valueOf(captchaExpirationSeconds));
        return result;
    }

    @Override
    public boolean validateCaptcha(String captchaId, String userAnswer) {
        if (captchaId == null || userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }

        String key = CAPTCHA_PREFIX + captchaId;
        String storedCode = redisTemplate.opsForValue().get(key);

        // Single-use: delete immediately to prevent replay attacks
        redisTemplate.delete(key);

        if (storedCode == null) {
            log.warn("CAPTCHA validation failed: Expired or invalid challenge ID {}", captchaId);
            return false;
        }

        return storedCode.equalsIgnoreCase(userAnswer.trim());
    }

    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private String renderCaptchaImage(String code) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // Antialiasing for smooth text rendering
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Dark security background
            g2d.setColor(new Color(25, 30, 45));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            // Interference lines
            for (int i = 0; i < 5; i++) {
                g2d.setColor(new Color(60 + random.nextInt(80), 80 + random.nextInt(90), 120 + random.nextInt(80)));
                int x1 = random.nextInt(WIDTH);
                int y1 = random.nextInt(HEIGHT);
                int x2 = random.nextInt(WIDTH);
                int y2 = random.nextInt(HEIGHT);
                g2d.drawLine(x1, y1, x2, y2);
            }

            // Interference noise dots
            for (int i = 0; i < 80; i++) {
                g2d.setColor(new Color(150 + random.nextInt(100), 150 + random.nextInt(100), 150 + random.nextInt(100), 120));
                g2d.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
            }

            // Draw characters with random rotation & colors
            Font font = new Font("Arial", Font.BOLD, 28);
            g2d.setFont(font);

            for (int i = 0; i < code.length(); i++) {
                char c = code.charAt(i);
                double angle = (random.nextDouble() - 0.5) * 0.4; // -0.2 to +0.2 rad rotation
                g2d.rotate(angle, 25.0 + i * 24, 32.0);
                g2d.setColor(new Color(100 + random.nextInt(155), 180 + random.nextInt(75), 230 + random.nextInt(25)));
                g2d.drawString(String.valueOf(c), 20 + i * 24, 35);
                g2d.rotate(-angle, 25.0 + i * 24, 32.0); // Reset rotation
            }

            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate CAPTCHA image", e);
            throw new RuntimeException("Error rendering CAPTCHA", e);
        }
    }
}

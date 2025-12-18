package org.health.service.auth;

import org.health.exception.CaptchaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Service
public class CaptchaService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${captcha.width:120}")
    private int width;

    @Value("${captcha.height:40}")
    private int height;

    @Value("${captcha.length:4}")
    private int length;

    @Value("${captcha.expire-seconds:120}")
    private int expireSeconds;

    @Value("${captcha.chars:0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ}")
    private String chars;

    private static final String REDIS_KEY_PREFIX = "captcha:";
    private static final Random random = new Random();

    /**
     * 生成验证码
     *
     * @return 验证码结果，包含captchaId和imageBase64
     */
    public CaptchaResult generateCaptcha() {
        // 生成验证码ID
        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 生成验证码字符串
        String captchaCode = generateRandomCode();

        // 生成验证码图片
        BufferedImage image = createImage(captchaCode);
        String imageBase64 = imageToBase64(image);

        // 存储到Redis，设置过期时间
        String redisKey = REDIS_KEY_PREFIX + captchaId;
        redisTemplate.opsForValue().set(redisKey, captchaCode.toUpperCase(), expireSeconds, TimeUnit.SECONDS);

        // 返回结果
        CaptchaResult result = new CaptchaResult();
        result.setCaptchaId(captchaId);
        result.setImageBase64("data:image/png;base64," + imageBase64);
        result.setExpireIn(expireSeconds);

        return result;
    }

    /**
     * 验证验证码
     *
     * @param captchaId   验证码ID
     * @param captchaCode 用户输入的验证码
     * @throws CaptchaException 验证失败时抛出
     */
    public void validateCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.trim().isEmpty()) {
            throw new CaptchaException("验证码ID不能为空");
        }

        if (captchaCode == null || captchaCode.trim().isEmpty()) {
            throw new CaptchaException("验证码不能为空");
        }

        String redisKey = REDIS_KEY_PREFIX + captchaId;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new CaptchaException("验证码已过期或不存在");
        }

        // 不区分大小写比较
        if (!storedCode.equalsIgnoreCase(captchaCode.trim())) {
            // 验证失败，删除验证码（防止暴力破解）
            redisTemplate.delete(redisKey);
            throw new CaptchaException("验证码错误");
        }

        // 验证成功，删除验证码（防止重复使用）
        redisTemplate.delete(redisKey);
    }

    /**
     * 生成随机验证码字符串
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    /**
     * 创建验证码图片
     */
    private BufferedImage createImage(String code) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 设置抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 绘制边框
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, width - 1, height - 1);

        // 绘制干扰线
        g.setColor(getRandomColor(160, 200));
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            g.drawLine(x1, y1, x2, y2);
        }

        // 绘制噪点
        g.setColor(getRandomColor(120, 180));
        for (int i = 0; i < 20; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            g.fillOval(x, y, 2, 2);
        }

        // 绘制验证码字符
        int charWidth = width / (length + 1);
        int charHeight = height - 10;
        g.setFont(new Font("Arial", Font.BOLD, charHeight));

        for (int i = 0; i < code.length(); i++) {
            g.setColor(getRandomColor(20, 130));
            int x = charWidth * (i + 1) - charWidth / 2;
            int y = charHeight + random.nextInt(5);
            // 随机旋转角度
            double angle = (random.nextDouble() - 0.5) * 0.4;
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }

        g.dispose();
        return image;
    }

    /**
     * 获取随机颜色
     */
    private Color getRandomColor(int min, int max) {
        int r = min + random.nextInt(max - min);
        int g = min + random.nextInt(max - min);
        int b = min + random.nextInt(max - min);
        return new Color(r, g, b);
    }

    /**
     * 将图片转换为Base64字符串
     */
    private String imageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            throw new RuntimeException("生成验证码图片失败", e);
        }
    }

    /**
     * 验证码结果
     */
    public static class CaptchaResult {
        private String captchaId;
        private String imageBase64;
        private Integer expireIn;

        public String getCaptchaId() {
            return captchaId;
        }

        public void setCaptchaId(String captchaId) {
            this.captchaId = captchaId;
        }

        public String getImageBase64() {
            return imageBase64;
        }

        public void setImageBase64(String imageBase64) {
            this.imageBase64 = imageBase64;
        }

        public Integer getExpireIn() {
            return expireIn;
        }

        public void setExpireIn(Integer expireIn) {
            this.expireIn = expireIn;
        }
    }
}


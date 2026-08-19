package com.dev.redisspringpra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务自定义配置
 * <p>
 * 通过 @ConfigurationProperties 把 application.yml 中的 app.* 配置绑定到对象，
 * 避免在 Service 中到处写魔法数字。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** 验证码配置 */
    private final Captcha captcha = new Captcha();

    /** Token 配置 */
    private final Token token = new Token();

    /** 接口限流配置 */
    private final RateLimit rateLimit = new RateLimit();

    @Data
    public static class Captcha {
        /** 验证码过期时间（分钟） */
        private int expireMinutes = 5;
        /** 验证码长度 */
        private int length = 6;
    }

    @Data
    public static class Token {
        /** Token 过期时间（分钟） */
        private int expireMinutes = 30;
    }

    @Data
    public static class RateLimit {
        /** 每 IP 每窗口最大请求数 */
        private int maxRequests = 10;
        /** 限流窗口（秒） */
        private int windowSeconds = 60;
    }
}

/**
 * @ConfigurationProperties = 把 application.yml 中的一组配置，批量绑定到 Java 对象。
 * 例如
 * @ConfigurationProperties(prefix = "app")
 * 去 application.yml 找 app 开头的配置，然后绑定到 AppProperties 对象。
 *
 *
 * @Value
 * │
 * └── 一个一个拿配置
 *     ${app.captcha.length}
 *
 *
 * @ConfigurationProperties
 * │
 * └── 一整组拿配置
 *     prefix = "app"
 *
 *     app
 *     ├── captcha
 *     ├── token
 *     └── rate-limit
 * */
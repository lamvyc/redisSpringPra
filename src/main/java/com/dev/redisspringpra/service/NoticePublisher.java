package com.dev.redisspringpra.service;

import com.dev.redisspringpra.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 案例10：消息通知发布者（Pub/Sub）
 * <p>
 * 发布流程：convertAndSend(channel, message) → Redis PUBLISH 命令
 * → 所有订阅了该频道的订阅者收到消息。
 * <p>
 * 为什么用 StringRedisTemplate.convertAndSend？
 * - 底层封装了 PUBLISH 命令，一行代码完成发布；
 * - 消息内容是纯字符串，StringRedisTemplate 不做 JSON 序列化，订阅方收到的字节一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticePublisher {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 发布系统通知
     * <p>
     * 真实场景：订单发货后发布「订单已发货」通知，所有在线订阅者实时收到。
     */
    public void publish(String message) {
        // PUBLISH channel:notice "消息内容"
        Long receivers = stringRedisTemplate.convertAndSend(RedisKeyConstants.CHANNEL_NOTICE, message);
        log.info("【发布通知】频道={}, 内容={}, 收到订阅者数={}", RedisKeyConstants.CHANNEL_NOTICE, message, receivers);
    }
}
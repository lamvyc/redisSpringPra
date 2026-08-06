package com.dev.redisspringpra.listener;

import com.dev.redisspringpra.constant.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 案例10：消息通知订阅者（Pub/Sub）
 * <p>
 * 消息通知流程：
 * 发布者（Publisher）→ PUBLISH channel:notice "订单已发货"
 * 订阅者（Subscriber）→ SUBSCRIBE channel:notice → 收到消息后处理
 * <p>
 * 为什么用 Pub/Sub？
 * - 适合「一对多」广播通知：如系统通知、订单状态变更通知；
 * - 发布者不关心谁在听，订阅者按频道接收，解耦。
 * <p>
 * 缺点（面试重点）：
 * - Pub/Sub 消息不持久化：订阅者不在线则消息丢失；
 * - 需要可靠消息（不丢消息）时用 Stream / 消息队列（RabbitMQ等）。
 */
@Slf4j
@Component
public class NoticeSubscriber implements MessageListener {

    /**
     * 收到消息回调
     * <p>
     * 真实项目在这里做业务处理：推送 App 通知、发送邮件、触发后续流程等。
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 频道
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        // 消息内容
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        if (RedisKeyConstants.CHANNEL_NOTICE.equals(channel)) {
            log.info("【收到通知】频道={}, 内容={}", channel, body);
            // TODO 业务处理：如更新订单状态、推送短信、记录审计日志等
        }
    }
}
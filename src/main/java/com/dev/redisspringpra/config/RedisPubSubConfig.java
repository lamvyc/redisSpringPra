package com.dev.redisspringpra.config;

import com.dev.redisspringpra.constant.RedisKeyConstants;
import com.dev.redisspringpra.listener.NoticeSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Pub/Sub 订阅配置
 * <p>
 * 作用：把 NoticeSubscriber 注册到监听容器，并订阅 channel:notice 频道。
 * 应用启动后，该 Bean 会保持连接持续监听，收到消息回调 NoticeSubscriber.onMessage。
 */
@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final NoticeSubscriber noticeSubscriber;

    /**
     * 消息监听容器
     * <p>
     * 为什么需要容器？
     * - 它管理订阅连接的线程池和生命周期；
     * - 一个容器可以订阅多个频道，每个频道可以有多个监听器。
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // 注册监听器 + 订阅频道：收到 channel:notice 的消息时回调 noticeSubscriber
        container.addMessageListener(new MessageListenerAdapter(noticeSubscriber), new ChannelTopic(RedisKeyConstants.CHANNEL_NOTICE));
        return container;
    }
}
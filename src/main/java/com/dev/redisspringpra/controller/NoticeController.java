package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.service.NoticePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 案例10：消息通知 Controller（Pub/Sub）
 * <p>
 * 测试接口：
 * POST /api/notice/publish?message=订单已发货  发布通知（订阅者实时收到）
 */
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticePublisher noticePublisher;

    /**
     * 发布通知
     * <p>
     * 测试：调用后观察控制台日志，
     * 发布者日志显示「收到订阅者数=1」，订阅者日志显示「收到通知」。
     */
    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(@RequestParam String message) {
        noticePublisher.publish(message);
        return Result.success(Map.of(
                "channel", "channel:notice",
                "message", message,
                "tip", "观察控制台，订阅者已收到该通知"
        ));
    }
}
package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.service.LogDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 案例11：日志使用全流程演示 Controller
 * <p>
 * 测试接口（启动后访问）：
 * GET /api/log/levels?userId=1&name=张三        演示 trace/debug/info/warn 各级别 + 占位符
 * GET /api/log/error                             演示 error 级别 + 异常堆栈记录
 * GET /api/log/flow?orderNo=20260813001          演示带上下文的业务流程日志
 * GET /api/log/collection                        演示集合类型日志输出
 * <p>
 * 注意：trace 默认不会显示（需把日志级别调到 TRACE），debug 需要配置为 debug 级别才显示。
 */
@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogDemoController {

    private final LogDemoService logDemoService;

    /** 演示五种级别 + 占位符用法 */
    @GetMapping("/levels")
    public Result<Void> demoLevels(@RequestParam Long userId,
                                   @RequestParam(required = false) String name) {
        logDemoService.demoLevels(userId, name);
        return Result.success();
    }

    /** 演示 error 级别记录异常堆栈 */
    @GetMapping("/error")
    public Result<Void> demoError() {
        logDemoService.demoError();
        return Result.success();
    }

    /** 演示带上下文的业务流程日志 */
    @GetMapping("/flow")
    public Result<Void> demoFlow(@RequestParam String orderNo) {
        logDemoService.demoBusinessFlow(orderNo);
        return Result.success();
    }

    /** 演示集合类型日志输出 */
    @GetMapping("/collection")
    public Result<Void> demoCollection() {
        logDemoService.demoCollection();
        return Result.success();
    }
}
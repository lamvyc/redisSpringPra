package com.dev.redisspringpra.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 案例11：日志使用全流程演示（@Slf4j + Logback）
 * <p>
 * 核心知识：{@code @Slf4j} 是 Lombok 提供的注解，编译时自动在类里生成：
 * {@code private static final Logger log = LoggerFactory.getLogger(LogDemoService.class);}
 * 底层是 SLF4J 门面 + Logback 实现（Spring Boot 默认自带，无需额外依赖）。
 * <p>
 * 五个日志级别（由低到高）：
 * <pre>
 * TRACE < DEBUG < INFO < WARN < ERROR
 * </pre>
 * 日志级别的作用：只有「大于等于配置级别」的日志才会输出。
 * 例如配置为 INFO，则只输出 INFO/WARN/ERROR，DEBUG 和 TRACE 会被过滤。
 *
 * <p>各级别语义（记忆锚点）：
 * <ul>
 *   <li>{@code trace}：最细粒度，追踪每一步执行轨迹（如进入方法、变量值），生产环境几乎不开启</li>
 *   <li>{@code debug}：调试信息，开发排障时用（如缓存命中/未命中、SQL 参数）</li>
 *   <li>{@code info}：关键业务流程节点（如「订单创建成功」「用户登录成功」）—— 生产环境默认开到这一级</li>
 *   <li>{@code warn}：潜在问题但不影响主流程（如「重试一次」「降级处理」）—— 需要关注但不一定报错</li>
 *   <li>{@code error}：错误，业务或系统异常 —— 必须记录，一般还要带异常堆栈</li>
 * </ul>
 */
@Slf4j
@Service
public class LogDemoService {

    /**
     * 演示：五个级别 + 占位符 {} 的用法。
     * <p>
     * 为什么用占位符 {} 而不用字符串拼接？
     * - 左侧写法：log.debug("userId=" + userId + ", name=" + name);
     *   即使日志级别过滤掉了 DEBUG，字符串拼接仍然执行，浪费性能。
     * - 占位符写法：log.debug("userId={}, name={}", userId, name);
     *   日志级别不满足时，方法内部直接不拼接字符串，零开销（惰性求值）。
     */
    public void demoLevels(Long userId, String name) {
        log.trace("【trace】进入 demoLevels 方法，userId={}, name={}", userId, name);

        log.debug("【debug】调试信息：准备处理用户 userId={}", userId);

        log.info("【info】开始处理用户信息，userId={}, name={}", userId, name);

        if (name == null || name.isBlank()) {
            // warn：可能有问题但流程还能继续（如缺少非关键字段，给默认值）
            log.warn("【warn】用户 name 为空，将使用默认值，userId={}", userId);
        }

        // 模拟正常业务处理
        log.info("【info】用户信息处理完成，userId={}", userId);
    }

    /**
     * 演示：error 级别记录异常，必须把异常对象作为最后一个参数传入。
     * <p>
     * 关键点：
     * log.error("msg", e) 会把完整异常堆栈打印出来 —— 排障必备，丢失堆栈等于白记日志。
     * <p>
     * 反例：log.error("出错了：" + e.getMessage());   ← 只有一句错误信息，没有堆栈，找不到出错代码所在行。
     */
    public void demoError() {
        try {
            // 故意抛出异常模拟业务出错
            int result = 1 / 0;
            log.info("【info】计算结果={}", result);
        } catch (ArithmeticException e) {
            // 正确姿势：异常作为最后一个参数，完整打印堆栈
            log.error("【error】计算发生除零异常，msg={}", e.getMessage(), e);
        }
    }

    /**
     * 演示：带上下文的业务流程，展示「一个请求的完整日志链路」。
     * <p>
     * 实际项目最佳实践：用同一个 traceId / 订单号 贯穿所有日志，方便在海量日志中串起一次完整调用。
     */
    public void demoBusinessFlow(String orderNo) {
        log.info("【info】========== 开始处理订单 orderNo={} ==========", orderNo);

        // 1. 校验
        log.debug("【debug】订单参数校验通过，orderNo={}", orderNo);

        // 2. 查询库存（假设库存不足，给 warn 提示）
        int stock = 0;
        if (stock <= 0) {
            log.warn("【warn】库存不足，orderNo={}, stock={}", orderNo, stock);
        }

        // 3. 调用第三方支付，模拟偶发失败
        log.info("【info】调用支付接口，orderNo={}", orderNo);
        try {
            boolean payOk = false;
            if (!payOk) {
                // 业务异常：主动抛出让上层 catch 记录 error
                throw new IllegalStateException("支付超时");
            }
        } catch (IllegalStateException e) {
            log.error("【error】支付失败，orderNo={}", orderNo, e);
        }

        log.info("【info】========== 结束处理订单 orderNo={} ==========", orderNo);
    }

    /**
     * 演示：批量数据/集合类型的日志输出。
     */
    public void demoCollection() {
        List<String> tags = Arrays.asList("缓存", "Redis", "日志");
        // 集合直接通过占位符输出，底层会调用 toString()
        log.info("【info】标签数量={}，标签内容={}", tags.size(), tags);

        // 逐条输出（数据量大时慎用，避免刷屏）
        tags.forEach(tag -> log.debug("【debug】遍历标签：{}", tag));
    }
}
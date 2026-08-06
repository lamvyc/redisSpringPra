package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 案例5：验证码 Controller（String + TTL）
 * <p>
 * 测试接口：
 * POST /api/captcha/send?phone=13800000001            发送验证码
 * POST /api/captcha/verify?phone=13800000001&code=123456  校验验证码
 */
@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /** 发送验证码（学习项目直接返回验证码，生产环境走短信服务商） */
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestParam String phone) {
        String captcha = captchaService.sendCaptcha(phone);
        return Result.success(Map.of(
                "phone", phone,
                "captcha", captcha,
                "tip", "学习案例直接返回验证码，生产环境通过短信发送"
        ));
    }

    /** 校验验证码 */
    @PostMapping("/verify")
    public Result<Map<String, Object>> verify(@RequestParam String phone, @RequestParam String code) {
        boolean valid = captchaService.verifyCaptcha(phone, code);
        return Result.success(Map.of(
                "valid", valid,
                "message", valid ? "验证码正确" : "验证码错误"
        ));
    }
}
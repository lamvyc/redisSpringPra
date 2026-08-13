package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.dto.UserUpdateRequest;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 案例1：用户信息缓存 Controller（String）
 * <p>
 * 测试接口（运行后访问）：
 * GET  /api/user/{id}          查询用户（走缓存）
 * PUT  /api/user/{id}          修改用户（先更新DB后删除缓存）
 * DELETE /api/user/{id}/cache  删除用户缓存
 * GET  /api/user/{id}/ttl      查看缓存剩余时间
 */
@RestController // @controller:需要配合@ResponseBody才返回JSON @RestController = @Controller + @ResponseBody(默认返回JSON)
@RequestMapping("/api/user")
@RequiredArgsConstructor // 自动生成构造器
public class UserCacheController {

    // `@RequiredArgsConstructor` 只对 **`final`** 或 **`@NonNull`** 字段生成构造器。
    private final UserCacheService userCacheService;

    /** 查询用户信息（首次查库写缓存，之后命中缓存） */
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(userCacheService.getUserById(id));
    }

    /** 修改用户信息（先更新DB，再删除缓存） */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.success(userCacheService.updateUser(id, request.getName(), request.getAge()));
    }

    /** 删除用户缓存 */
    @DeleteMapping("/{id}/cache")
    public Result<Void> deleteCache(@PathVariable Long id) {
        userCacheService.deleteCache(id);
        return Result.success();
    }

    /** 查看缓存剩余过期时间 */
    @GetMapping("/{id}/ttl")
    public Result<Map<String, Object>> getTtl(@PathVariable Long id) {
        return Result.success(Map.of("userId", id, "ttlSeconds", userCacheService.getCacheTtl(id)));
    }
}

/**
 * 字段注入 = 运行时可能 NPE，测试要启动容器，依赖可被修改；
 * 构造器注入 = 启动即报错，测试直接 new，final 保证不可变。Spring 官方推荐构造器注入。
 *
 * NPE = NullPointerException，当程序试图在 null 对象上调用方法或访问属性时抛出的异常。
 *
 *
 * 构造器注入 + Lombok（最推荐）
 * @Service
 * @RequiredArgsConstructor // 自动生成构造器
 * public class EmployeeServiceImpl implements EmployeeService {
 *
 *     private final EmployeeMapper employeeMapper;
 *     private final DeptMapper deptMapper;
 *     // 不用写任何构造器代码
 * }
 *
 *@Service
 * public class EmployeeServiceImpl implements EmployeeService {
 *     private final EmployeeMapper employeeMapper;
 *     private final DeptMapper deptMapper;
 *     // 通过构造器传入
 *     public EmployeeServiceImpl(EmployeeMapper employeeMapper, DeptMapper deptMapper) {
 *         this.employeeMapper = employeeMapper;
 *         this.deptMapper = deptMapper;
 *     }
 * }
 *
 *
 * @Pathvariable:从URL路径取值 (/users/{id})
 * @RequestParam:从查询参数取值 (/users?name=张三)
 * @RequestBody:从请求体取值(JSON/XML)
 * */
package com.dev.redisspringpra.repository;

import com.dev.redisspringpra.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 用户表 Repository（对应原 MockDb 的用户表操作）
 * <p>
 * 继承 JpaRepository 即可获得：
 * - findById：根据主键查询（替代 MockDb.findUserById）
 * - save/update：保存或更新（替代 MockDb.updateUser）
 *
 * Repository = 数据库/数据访问层 + Spring Bean
 *
 * UserRepository 是你自己定义的 Repository 接口，它继承了 Spring Data JPA 提供的 JpaRepository，
 * 因此获得了 Spring Data JPA 提供的各种数据访问能力。
 *
 * Spring Data JPA 本身就会扫描 Repository 并注册代理对象，所以 @Repository 在这种场景下通常不是必须的。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}


/**
 * 我的代码：
 * <p>@Repository
 * <p> public interface UserRepository extends JpaRepository<User, Long> {
 * <p>}
 * <p>
 * 我什么都没写，却能这样用：
 * <p>@Autowired
 * <p>private UserRepository userRepository;
 *
 * <p>User user = userRepository.findById(1L).orElse(null);
 *
 * 底层发生了什么:
 * 启动 Spring Boot 时：
 * 1.扫描到 @Repository
 * 2.发现它继承了 JpaRepository
 * 3.Spring 动态生成一个实现类
 * 4.把这个实现类放进 IOC 容器
 * 5.@Autowired 注入的其实就是这个实现类（代理对象）
 * 所以你拿到的不是接口，而是接口对应的代理对象
 * <p>
 * 场景:TypeScript interface
 * 谁实现接口:你自己
 *
 * <p>
 * 场景:TypeScript interface
 * 谁实现接口:你自己
 *
 * <p>
 * 场景:Spring Repository interface
 * 谁实现接口:Spring 框架
 * <p>
 *  记住一句话：
 *  接口定义能力，框架提供实现；运行时通过动态代理等机制，把接口变成可调用的对象。
 *  你面向接口编程，Spring 面向数据库实现。
 * <p>
 * */
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
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
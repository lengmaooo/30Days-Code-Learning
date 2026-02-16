package com.example.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.learning.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // MyBatis-Plus 已经提供了基础的 CRUD 方法
    // TODO: 自定义 SQL 方法可以在这里添加
    User selectByUsername(@Param("username") String username);

}
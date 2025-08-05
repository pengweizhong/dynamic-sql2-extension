package com.dynamic.sql.ext.mapper;

import com.dynamic.sql.ext.entities.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {
    List<User> queryUsers(@Param("name") String name);
}

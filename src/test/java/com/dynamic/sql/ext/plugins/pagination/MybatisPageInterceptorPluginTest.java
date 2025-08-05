package com.dynamic.sql.ext.plugins.pagination;

import com.dynamic.sql.ext.InitializingContext;
import com.dynamic.sql.ext.entities.User;
import com.dynamic.sql.ext.mapper.UserMapper;
import com.dynamic.sql.plugins.pagination.PageHelper;
import com.dynamic.sql.plugins.pagination.PageInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

class MybatisPageInterceptorPluginTest extends InitializingContext {
    @Test
    void testNotPage() {
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        List<User> users = mapper.queryUsers("Jerry");
        System.out.println("查询结果：" + users);
    }

    @Test
    void testPage() {
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        PageInfo<List<User>> pageInfo = PageHelper.ofMybatis(1, 5).selectPage(() -> mapper.queryUsers(""));
        pageInfo.getRecords().forEach(System.out::println);
        System.out.println("分页结果：" + pageInfo);
    }

    @Test
    void testPage2() {
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        PageInfo<List<User>> pageInfo = PageHelper.ofMybatis(1, 5).selectPage(() -> mapper.queryUsers(""));
        pageInfo.getRecords().forEach(System.out::println);
        System.out.println("分页结果：" + pageInfo);

        while (pageInfo.hasNextPage()) {
            pageInfo.selectNextPage();
            System.out.println("下一页的分页结果：" + pageInfo);
        }
    }
}
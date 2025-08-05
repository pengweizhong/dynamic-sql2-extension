/*
 * Copyright (c) 2024 PengWeizhong. All Rights Reserved.
 *
 * This source code is licensed under the MIT License.
 * You may obtain a copy of the License at:
 * https://opensource.org/licenses/MIT
 *
 * See the LICENSE file in the project root for more information.
 */
package com.dynamic.sql.ext.plugins.pagination;

import com.dynamic.sql.core.database.PreparedSql;
import com.dynamic.sql.core.dml.SqlStatementWrapper;
import com.dynamic.sql.interceptor.ExecutionControl;
import com.dynamic.sql.interceptor.SqlInterceptor;
import com.dynamic.sql.plugins.pagination.AbstractPage;
import com.dynamic.sql.plugins.pagination.DefaultPagePluginType;
import com.dynamic.sql.plugins.pagination.LocalPage;
import com.dynamic.sql.plugins.pagination.PagePluginType;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 目前只兼容MySQL的分页
 */
@Intercepts({
        @Signature(type = org.apache.ibatis.executor.Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        })
})
public class MybatisPageInterceptorPlugin implements SqlInterceptor, PagePluginType, Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        AbstractPage abstractPage = LocalPage.getCurrentPage();
        if (abstractPage == null || !abstractPage.getPagePluginTypeName().equalsIgnoreCase(this.getPluginName())) {
            //不分页，直接返回原结果
            return invocation.proceed();
        }

        // 1. 获取原始参数
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object paramObj = invocation.getArgs()[1];

        BoundSql boundSql = ms.getBoundSql(paramObj);
        String originalSql = boundSql.getSql().trim();
        Long total = abstractPage.getCacheTotal();
        if (total == null) {
            total = executeCountSql(originalSql, invocation);
        }
        //没有数据就没有必要继续执行
        if (total == 0) {
            return ExecutionControl.SKIP;
        }
        // 计算分页的偏移量 (pageIndex - 1) * pageSize
        int offset = (abstractPage.getPageIndex() - 1) * abstractPage.getPageSize();
        // 构造分页 SQL
        String pageSql = "SELECT * FROM (" + originalSql + ") AS _PAGE_TEMP LIMIT " + offset + ", " + abstractPage.getPageSize();
        // 包装新的 BoundSql
        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), pageSql,
                boundSql.getParameterMappings(), paramObj);

        // 创建新的 MappedStatement
        MappedStatement newMs = copyFromMappedStatement(ms, new BoundSqlSqlSource(newBoundSql), false);

        // 替换参数并执行
        invocation.getArgs()[0] = newMs;

        return invocation.proceed();
    }

    private long executeCountSql(String originalSql, Invocation invocation) throws SQLException {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object paramObj = invocation.getArgs()[1];

        BoundSql boundSql = ms.getBoundSql(paramObj);
        //  执行 count 查询
        String countSql = "SELECT COUNT(*) FROM (" + originalSql + ") AS _COUNT_PAGE_TEMP";
        // 构造 count 用的 BoundSql
        BoundSql countBoundSql = new BoundSql(ms.getConfiguration(), countSql,
                boundSql.getParameterMappings(), paramObj);
        // 创建 count 的 MappedStatement
        MappedStatement countMs = copyFromMappedStatement(ms, new BoundSqlSqlSource(countBoundSql), true);

        // 执行 count 查询
        org.apache.ibatis.executor.Executor executor = (org.apache.ibatis.executor.Executor) invocation.getTarget();
        List<Object> countResultList = executor.query(countMs, paramObj, RowBounds.DEFAULT, null);
        // 从结果中提取 count
        long total = ((Number) countResultList.get(0)).longValue();
        AbstractPage abstractPage = LocalPage.getCurrentPage();
        abstractPage.setTotal(total);
        abstractPage.initTotalPage();
        return total;
    }


    // 复制 MappedStatement
    private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource, boolean isCountQuery) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(), ms.getId() + (isCountQuery ? "_count" : ""), newSqlSource, ms.getSqlCommandType());

        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        builder.keyProperty(String.join(",", ms.getKeyProperties() != null ? ms.getKeyProperties() : new String[0]));
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        if (isCountQuery) {
            List<ResultMap> resultMaps = Collections.singletonList(
                    new ResultMap.Builder(ms.getConfiguration(), ms.getId() + "_count_result", Long.class, new ArrayList<>()).build()
            );
            builder.resultMaps(resultMaps);
        } else {
            builder.resultMaps(ms.getResultMaps());
        }
        return builder.build();
    }


    static class BoundSqlSqlSource implements SqlSource {
        private final BoundSql boundSql;

        public BoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }

    @Override
    public ExecutionControl beforeExecution(SqlStatementWrapper sqlStatementWrapper, Connection connection) {
        return null;
    }

    @Override
    public void afterExecution(PreparedSql preparedSql, Object applyResult, Exception exception) {
        //IGNORE
    }

    @Override
    public String getPluginName() {
        return DefaultPagePluginType.MYBATIS.getPluginName();
    }

}

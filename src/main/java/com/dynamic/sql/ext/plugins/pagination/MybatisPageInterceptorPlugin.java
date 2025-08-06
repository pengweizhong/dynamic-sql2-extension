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
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * 目前只兼容MySQL的分页
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
})
@SuppressWarnings({"rawtypes", "unchecked"})
public class MybatisPageInterceptorPlugin implements SqlInterceptor, PagePluginType, Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        AbstractPage abstractPage = LocalPage.getCurrentPage();
        if (abstractPage == null || !abstractPage.getPagePluginTypeName().equalsIgnoreCase(this.getPluginName())) {
            //不分页，直接返回原结果
            return invocation.proceed();
        }
        // 1. 获取原始参数
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object paramObj = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        ResultHandler resultHandler = (ResultHandler) args[3];
        Executor executor = (Executor) invocation.getTarget();
        BoundSql boundSql;
        CacheKey cacheKey;
        if (args.length == 4) {
            boundSql = ms.getBoundSql(paramObj);
            cacheKey = executor.createCacheKey(ms, paramObj, rowBounds, boundSql);
        } else {
            cacheKey = (CacheKey) args[4];
            boundSql = (BoundSql) args[5];
        }
        Long total = abstractPage.getCacheTotal();
        if (total == null) {
            total = executeCountSql(ms, paramObj, boundSql, executor, resultHandler);
        }
        //没有数据就没有必要继续执行
        if (total == 0) {
            return new ArrayList<>();
        }
        // 计算分页的偏移量 (pageIndex - 1) * pageSize
        int offset = (abstractPage.getPageIndex() - 1) * abstractPage.getPageSize();
        // 构造分页 SQL
        String originalSql = boundSql.getSql().trim();
        String pageSql = "SELECT * FROM (" + originalSql + ") AS _PAGE_TEMP LIMIT " + offset + ", " + abstractPage.getPageSize();
        // 包装新的 BoundSql
        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), pageSql, boundSql.getParameterMappings(), paramObj);

        //添加原始的 additionalParameters
        copyAdditionalParameters(boundSql, newBoundSql);
        return executor.query(ms, paramObj, RowBounds.DEFAULT, resultHandler, cacheKey, newBoundSql);
    }

    private long executeCountSql(MappedStatement ms, Object paramObj, BoundSql boundSql, Executor executor, ResultHandler resultHandler) throws SQLException {
        //  执行 count 查询
        String originalSql = boundSql.getSql().trim();
        String countSql = "SELECT COUNT(*) FROM (" + originalSql + ") AS _COUNT_PAGE_TEMP";
        // 构造 count 用的 BoundSql
        BoundSql countBoundSql = new BoundSql(ms.getConfiguration(), countSql, boundSql.getParameterMappings(), paramObj);
        //添加原始的 additionalParameters
        copyAdditionalParameters(boundSql, countBoundSql);
        CacheKey pageCacheKey = executor.createCacheKey(ms, paramObj, RowBounds.DEFAULT, countBoundSql);
        // 创建 count 的 MappedStatement
        MappedStatement countMs = copyFromMappedStatement(ms, new BoundSqlSqlSource(countBoundSql));
        // 执行 count 查询
        List<Object> countResultList = executor.query(countMs, paramObj, RowBounds.DEFAULT, resultHandler, pageCacheKey, countBoundSql);

        // 从结果中提取 count
        long total = ((Number) countResultList.get(0)).longValue();
        AbstractPage abstractPage = LocalPage.getCurrentPage();
        abstractPage.setTotal(total);
        abstractPage.initTotalPage();
        return total;
    }

    @SuppressWarnings("all")
    public static void copyAdditionalParameters(BoundSql source, BoundSql target) {
        try {
            Field additionalParametersField = BoundSql.class.getDeclaredField("additionalParameters");
            additionalParametersField.setAccessible(true);
            Map<String, Object> sourceParams = (Map<String, Object>) additionalParametersField.get(source);
            Map<String, Object> targetParams = (Map<String, Object>) additionalParametersField.get(target);
            if (sourceParams != null && targetParams != null) {
                targetParams.putAll(sourceParams);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy additionalParameters from BoundSql", e);
        }
    }

    @SuppressWarnings("all")
    public static void setBoundSqlString(BoundSql boundSql, String newSql) {
        try {
            Field sql = BoundSql.class.getDeclaredField("sql");
            sql.setAccessible(true);
            sql.set(boundSql, newSql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy additionalParameters from BoundSql", e);
        }
    }

    // 复制 MappedStatement
    private MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(), ms.getId() + "Count", newSqlSource, ms.getSqlCommandType());
        builder.keyColumn(Arrays.toString(ms.getKeyColumns()));
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
        builder.resultOrdered(ms.isResultOrdered());
        builder.lang(ms.getLang());
        builder.databaseId(ms.getDatabaseId());
        List<ResultMap> resultMaps = Collections.singletonList(
                new ResultMap.Builder(ms.getConfiguration(), ms.getId() + "CountResult", Long.class, new ArrayList<>()).build()
        );
        builder.resultMaps(resultMaps);
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
        //IGNORE
        return ExecutionControl.PROCEED;
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

//package com.dynamic.sql.ext.plugins.conversion;
//
//import com.dynamic.sql.anno.Table;
//import com.dynamic.sql.exception.DynamicSqlException;
//import com.dynamic.sql.table.FieldMeta;
//import com.dynamic.sql.table.TableProvider;
//import com.dynamic.sql.utils.CollectionUtils;
//import com.dynamic.sql.utils.ConverterUtils;
//import com.dynamic.sql.utils.ReflectUtils;
//import org.apache.ibatis.executor.resultset.ResultSetHandler;
//import org.apache.ibatis.mapping.MappedStatement;
//import org.apache.ibatis.mapping.ResultMap;
//import org.apache.ibatis.mapping.SqlCommandType;
//import org.apache.ibatis.plugin.*;
//import org.apache.ibatis.reflection.MetaObject;
//import org.apache.ibatis.reflection.SystemMetaObject;
//
//import java.sql.ResultSet;
//import java.sql.ResultSetMetaData;
//import java.sql.Statement;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Intercepts({
//        @Signature(
//                type = ResultSetHandler.class,
//                method = "handleResultSets",
//                args = {Statement.class}
//        )
//})
//public class MybatisAttributeMappingInterceptor implements Interceptor {
//
//    @Override
//    public Object intercept(Invocation invocation) throws Throwable {
//        // 获取原始结果集
//        Statement statement = (Statement) invocation.getArgs()[0];
//        ResultSet resultSet = statement.getResultSet();
//        if (resultSet == null) {
//            return invocation.proceed();
//        }
//        MetaObject metaObject = SystemMetaObject.forObject(invocation.getTarget());
//        // Access the Configuration
//        Object configuration = metaObject.getValue("configuration");
//        if (configuration == null) {
//            throw new DynamicSqlException("Unable to retrieve Configuration from ResultSetHandler.");
//        }
//
//        // Get the MappedStatement
//        Object mappedStatement = metaObject.getValue("mappedStatement");
//        if (mappedStatement == null) {
//            throw new DynamicSqlException("Unable to retrieve MappedStatement from ResultSetHandler.");
//        }
//        // 获取返回结果类型
//        MappedStatement ms = (MappedStatement) mappedStatement;
//        // 只处理查询操作
//        if (ms.getSqlCommandType() != SqlCommandType.SELECT) {
//            return invocation.proceed(); // 不是查询操作，跳过处理
//        }
//        List<ResultMap> resultMaps = ms.getResultMaps();
//        if (CollectionUtils.isEmpty(resultMaps)) {
//            // 调用原始逻辑继续处理
//            return invocation.proceed();
//        }
//        ResultMap resultMap = resultMaps.get(0);
//        Class<?> resultType = resultMap.getType();
//        if (resultType.getClassLoader() == null) {
//            return invocation.proceed();
//        }
//        // 判断映射类是表还是视图
//        List<FieldMeta> columnMetas;
//        if (resultType.isAnnotationPresent(Table.class)) {
//            columnMetas = TableProvider.getTableMeta(resultType).getColumnMetas();
//        } else {
//            columnMetas = TableProvider.getViewMeta(resultType).getViewColumnMetas();
//        }
//        Map<String, FieldMeta> fieldNameMap = columnMetas.stream().collect(Collectors.toMap(cm -> cm.getField().getName(), cm -> cm));
//        Map<String, FieldMeta> columnNameMap = columnMetas.stream().collect(Collectors.toMap(cm -> cm.getColumnName(), cm -> cm));
//        // 处理结果集，完成枚举映射
//        List<Object> resultList = new ArrayList<>();
//        ResultSetMetaData metaData = resultSet.getMetaData();
//        int columnCount = metaData.getColumnCount();
//        while (resultSet.next()) {
//            Object instance = ReflectUtils.instance(resultType);
//            resultList.add(instance);
//            for (int i = 1; i <= columnCount; i++) {
//                String columnName = metaData.getColumnLabel(i);
//                Object value = resultSet.getObject(i);
//                FieldMeta fieldMeta = Optional.ofNullable(fieldNameMap.get(columnName)).orElse(columnNameMap.get(columnName));
//                //不关心查询了不存在的列
//                if (fieldMeta == null) {
//                    continue;
//                }
//                Object v = ConverterUtils.convertToEntityAttribute(fieldMeta, fieldMeta.getField().getType(), value);
//                ReflectUtils.setFieldValue(instance, fieldMeta.getField(), v);
//            }
//        }
//        return resultList;
//    }
//
//    @Override
//    public Object plugin(Object target) {
//        return Plugin.wrap(target, this);
//    }
//
//    @Override
//    public void setProperties(Properties properties) {
//        // 可以通过配置文件传递参数（如果需要）
//    }
//}
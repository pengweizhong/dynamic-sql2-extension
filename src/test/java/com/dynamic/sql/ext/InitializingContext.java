package com.dynamic.sql.ext;

import com.dynamic.sql.context.SqlContextHelper;
import com.dynamic.sql.context.properties.SchemaProperties;
import com.dynamic.sql.context.properties.SqlContextProperties;
import com.dynamic.sql.core.SqlContext;
import com.dynamic.sql.datasource.DataSourceProvider;
import com.dynamic.sql.datasource.connection.ConnectionHolder;
import com.dynamic.sql.datasource.connection.SimpleConnectionHandle;
import com.dynamic.sql.ext.plugins.conversion.MybatisAdaptObjectWrapperFactory;
import com.dynamic.sql.ext.plugins.conversion.impl.FetchJsonObjectConverter;
import com.dynamic.sql.ext.plugins.pagination.MybatisPageInterceptorPlugin;
import com.dynamic.sql.plugins.exception.DefaultSqlErrorHint;
import com.dynamic.sql.plugins.exception.ExceptionPlugin;
import com.dynamic.sql.plugins.pagination.PageInterceptorPlugin;
import com.dynamic.sql.utils.ConverterUtils;
import com.google.gson.JsonObject;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.InputStream;

public class InitializingContext {
    protected static SqlSession sqlSession;
    protected static SqlContext sqlContext;
    private static final Logger log = LoggerFactory.getLogger(InitializingContext.class);

    @BeforeAll
    public static void setup() throws Exception {
        if (sqlContext == null) {
            log.info("--------------------- SqlContext 初始化 ---------------------");
            SqlContextProperties sqlContextProperties = SqlContextProperties.defaultSqlContextProperties();
            sqlContextProperties.setScanTablePackage("com.dynamic.sql");
            sqlContextProperties.setScanDatabasePackage("com.dynamic.sql");
            //提供Mapper代理，但是与Mybatis不兼容，因此推荐使用SqlContext
            sqlContextProperties.setScanMapperPackage("com.dynamic.sql");
            SchemaProperties schemaProperties = new SchemaProperties();
            //本地数据源名称
            schemaProperties.setDataSourceName("dataSource");
            //设置全局默认数据源
            schemaProperties.setGlobalDefault(true);
            schemaProperties.setUseSchemaInQuery(false);
            //可以直接指定SQL方言
            //schemaProperties.setSqlDialect(SqlDialect.ORACLE);
            //指定特定的版本号，不同的版本号语法可能不同
            //schemaProperties.setDatabaseProductVersion("11.0.0.1");
            schemaProperties.setUseAsInQuery(true);
            //打印SQL
            SchemaProperties.PrintSqlProperties printSqlProperties = new SchemaProperties.PrintSqlProperties();
            printSqlProperties.setPrintSql(true);
            printSqlProperties.setPrintDataSourceName(true);
            schemaProperties.setPrintSqlProperties(printSqlProperties);
            sqlContextProperties.addSchemaProperties(schemaProperties);
            //内置分页
            sqlContextProperties.addInterceptor(new PageInterceptorPlugin());
            sqlContextProperties.addInterceptor(new ExceptionPlugin(new DefaultSqlErrorHint()));
            //内置JDBC连接
            ConnectionHolder.setConnectionHandle(new SimpleConnectionHandle());
            ConverterUtils.putFetchResultConverter(JsonObject.class, new FetchJsonObjectConverter());
            sqlContext = SqlContextHelper.createSqlContext(sqlContextProperties);
        }
        if (sqlSession == null) {
            log.info("--------------------- sqlSession 初始化 ---------------------");
            // 1. 配置数据源
            DataSource dataSource = DataSourceProvider.getDefaultDataSourceMeta().getDataSource();
            // 2. 加载 MyBatis 配置
            InputStream configStream = Resources.getResourceAsStream("mybatis-config.xml");
            XMLConfigBuilder configBuilder = new XMLConfigBuilder(configStream);
            Configuration configuration = configBuilder.parse();
            // 3. 设置数据源和环境
            Environment environment = new Environment("dev", new JdbcTransactionFactory(), dataSource);
            configuration.setEnvironment(environment);
//            configuration.setObjectWrapperFactory(new CustomWrapperFactory());
            configuration.setObjectWrapperFactory(new MybatisAdaptObjectWrapperFactory());
            //configuration.setMapUnderscoreToCamelCase(true);
            configuration.addInterceptor(new MybatisPageInterceptorPlugin());
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
            sqlSession = sqlSessionFactory.openSession();
        }
    }

    @AfterAll
    public static void teardown() throws Exception {
        sqlSession.close();
    }
}

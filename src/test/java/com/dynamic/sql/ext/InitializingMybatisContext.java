package com.dynamic.sql.ext;

import com.dynamic.sql.ext.datasource.MysqlDataSource;
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

import javax.sql.DataSource;
import java.io.InputStream;

public class InitializingMybatisContext {
    protected static SqlSession sqlSession;

    @BeforeAll
    public static void setup() throws Exception {
        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        // ✅ 1. 配置数据源
        DataSource dataSource = mysqlDataSource.getDataSource();
        // ✅ 2. 加载 MyBatis 配置
        InputStream configStream = Resources.getResourceAsStream("mybatis-config.xml");
        XMLConfigBuilder configBuilder = new XMLConfigBuilder(configStream);
        Configuration configuration = configBuilder.parse();
        // ✅ 3. 设置数据源和环境
        Environment environment = new Environment("dev", new JdbcTransactionFactory(), dataSource);
        configuration.setEnvironment(environment);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        sqlSession = sqlSessionFactory.openSession();
    }

    @AfterAll
    public static void teardown() throws Exception {
        sqlSession.close();
    }
}

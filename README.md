# 如何使用？
引入项目 
```xml
<dependency>
    <groupId>com.dynamic-sql</groupId>
    <artifactId>dynamic-sql2-extension</artifactId>
    <version>0.1.4</version>
</dependency>
```
配置环境
> 目前主要配置：MybatisAdaptObjectWrapperFactory、MybatisPageInterceptorPlugin

```java

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
//添加dynamic-sql2包装映射
configuration.setObjectWrapperFactory(new MybatisAdaptObjectWrapperFactory());
//添加分页插件
configuration.addInterceptor(new MybatisPageInterceptorPlugin());
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
sqlSession = sqlSessionFactory.openSession();

```
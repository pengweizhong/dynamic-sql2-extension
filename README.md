# 如何使用？
引入项目 
```xml
<dependency>
    <groupId>com.dynamic-sql</groupId>
    <artifactId>dynamic-sql2-extension</artifactId>
    <version>0.1.4</version>
</dependency>
```
在项目中注入Bean

```java
@Configuration
public class MyBatisPageConfig {
    @Bean
    public MybatisPageInterceptorPlugin mybatisPageInterceptorPlugin() {
        return new MybatisPageInterceptorPlugin();
    }
}

```
package com.dynamic.sql.ext.plugins.conversion;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

import java.util.Collection;
import java.util.Map;

public class CustomWrapperFactory implements ObjectWrapperFactory {
    //TODO 需要兼容全部的类型
    @Override
    public boolean hasWrapperFor(Object object) {
        // 只对用户自定义的实体类处理（不是 Map、Collection、内置类）
        return isJavaBean(object);
    }

    //每次映射结果都会触发，然后创建一个Wrapper对象
    @Override
    public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
        return new MybatisAttributeBeanWrapper(metaObject, object);
    }

    private boolean isJavaBean(Object obj) {
        if (obj instanceof Map || obj instanceof Collection) return false;
        Package pkg = obj.getClass().getPackage();
        // 排除 JDK 内置类
        return pkg != null && !pkg.getName().startsWith("java.");
    }
}
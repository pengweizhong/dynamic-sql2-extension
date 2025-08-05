package com.dynamic.sql.ext.plugins.conversion;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

import java.util.Collection;
import java.util.Map;

public class MyStrictObjectWrapperFactory implements ObjectWrapperFactory {
    @Override
    public boolean hasWrapperFor(Object object) {
        // 所有非 map 的对象都用我们的 wrapper
        return isJavaBean(object);
    }

    @Override
    public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
        return new MyStrictColumnWrapper(metaObject, object);
    }


    private boolean isJavaBean(Object obj) {
        if (obj instanceof Map || obj instanceof Collection) return false;
        Package pkg = obj.getClass().getPackage();
        // 排除 JDK 内置类
        return pkg != null && !pkg.getName().startsWith("java.");
    }
}

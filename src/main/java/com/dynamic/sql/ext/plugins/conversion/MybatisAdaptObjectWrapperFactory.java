/*
 * Copyright (c) 2024 PengWeizhong. All Rights Reserved.
 *
 * This source code is licensed under the MIT License.
 * You may obtain a copy of the License at:
 * https://opensource.org/licenses/MIT
 *
 * See the LICENSE file in the project root for more information.
 */
package com.dynamic.sql.ext.plugins.conversion;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;

import java.util.Collection;
import java.util.Map;

public class MybatisAdaptObjectWrapperFactory implements ObjectWrapperFactory {
    @Override
    public boolean hasWrapperFor(Object object) {
        return isJavaBean(object);
    }

    @Override
    public ObjectWrapper getWrapperFor(MetaObject metaObject, Object object) {
        return new MybatisAdaptColumnWrapper(metaObject, object);
    }


    private boolean isJavaBean(Object obj) {
        if (obj instanceof Map || obj instanceof Collection) return false;
        Package pkg = obj.getClass().getPackage();
        // 排除 JDK 内置类
        return pkg != null && !pkg.getName().startsWith("java.");
    }
}

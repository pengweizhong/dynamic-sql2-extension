package com.dynamic.sql.ext.plugins.conversion;

import com.dynamic.sql.anno.Table;
import com.dynamic.sql.table.FieldMeta;
import com.dynamic.sql.table.TableProvider;
import com.dynamic.sql.utils.ConverterUtils;
import com.dynamic.sql.utils.ReflectUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MybatisAdaptColumnWrapper implements ObjectWrapper {
    private static final Logger log = LoggerFactory.getLogger(MybatisAdaptColumnWrapper.class);
    private final Object instance;
    private final Map<String, FieldMeta> fieldNameMap;
    private final Map<String, FieldMeta> columnNameMap;

    public MybatisAdaptColumnWrapper(MetaObject metaObject, Object object) {
        this.instance = object;
        Object targetObject = metaObject.getOriginalObject();
        List<FieldMeta> columnMetas;
        Class<?> resultType = targetObject.getClass();
        if (resultType.isAnnotationPresent(Table.class)) {
            columnMetas = TableProvider.getTableMeta(resultType).getColumnMetas();
        } else {
            columnMetas = TableProvider.getViewMeta(resultType).getViewColumnMetas();
        }
        fieldNameMap = columnMetas.stream().collect(Collectors.toMap(cm -> cm.getField().getName(), cm -> cm));
        columnNameMap = columnMetas.stream().collect(Collectors.toMap(FieldMeta::getColumnName, cm -> cm));
    }

    @Override
    public void set(PropertyTokenizer prop, Object value) {
        String columnName = prop.getName();
//        System.out.println("---------------> set: columnName=" + columnName + "， value=" + value);
        FieldMeta fieldMeta = Optional.ofNullable(fieldNameMap.get(columnName)).orElse(columnNameMap.get(columnName));
        //不关心查询了不存在的列
        if (fieldMeta == null) {
            log.trace("查询了未使用到的列：{}", columnName);
            return;
        }
        Object newValue = ConverterUtils.convertToEntityAttribute(fieldMeta, fieldMeta.getField().getType(), value);
        ReflectUtils.setFieldValue(instance, fieldMeta.getField(), newValue);
    }

    @Override
    public String findProperty(String name, boolean useCamelCaseMapping) {
//        System.out.println("findProperty");
        return name;
    }

    @Override
    public String[] getGetterNames() {
//        System.out.println("getGetterNames");
        return new String[0];
    }

    @Override
    public String[] getSetterNames() {
//        System.out.println("getSetterNames");
        return new String[0];
    }

    @Override
    public Class<?> getSetterType(String name) {
//        System.out.println("getSetterType");
        return Object.class;
    }

    @Override
    public Class<?> getGetterType(String name) {
//        System.out.println("getGetterType");
        return Object.class;
    }

    @Override
    public boolean hasSetter(String name) {
//        System.out.println("hasSetter");
        return true;
    }

    @Override
    public boolean hasGetter(String name) {
//        System.out.println("hasGetter");
        return false;
    }

    @Override
    public Object get(PropertyTokenizer prop) {
//        System.out.println("get");
        return null;
    }

    @Override
    public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
//        System.out.println("instantiatePropertyValue");
        return null;
    }

    @Override
    public boolean isCollection() {
//        System.out.println("isCollection");
        return false;
    }

    @Override
    public void add(Object element) {
//        System.out.println("add");
    }

    @Override
    public <E> void addAll(List<E> elementList) {
//        System.out.println("addAll");
    }
}

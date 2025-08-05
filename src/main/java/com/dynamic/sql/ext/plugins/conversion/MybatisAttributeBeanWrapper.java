//package com.dynamic.sql.ext.plugins.conversion;
//
//import com.dynamic.sql.anno.Table;
//import com.dynamic.sql.table.FieldMeta;
//import com.dynamic.sql.table.TableProvider;
//import com.dynamic.sql.utils.ConverterUtils;
//import org.apache.ibatis.reflection.MetaObject;
//import org.apache.ibatis.reflection.property.PropertyTokenizer;
//import org.apache.ibatis.reflection.wrapper.BeanWrapper;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.stream.Collectors;
//
//public class MybatisAttributeBeanWrapper extends BeanWrapper {
//    private final Map<String, FieldMeta> fieldNameMap;
//    private final Map<String, FieldMeta> columnNameMap;
//
//    public MybatisAttributeBeanWrapper(MetaObject metaObject, Object object) {
//        super(metaObject, object);
//        Object targetObject = metaObject.getOriginalObject();
//        List<FieldMeta> columnMetas;
//        Class<?> resultType = targetObject.getClass();
//        if (resultType.isAnnotationPresent(Table.class)) {
//            columnMetas = TableProvider.getTableMeta(resultType).getColumnMetas();
//        } else {
//            columnMetas = TableProvider.getViewMeta(resultType).getViewColumnMetas();
//        }
//        fieldNameMap = columnMetas.stream().collect(Collectors.toMap(cm -> cm.getField().getName(), cm -> cm));
//        columnNameMap = columnMetas.stream().collect(Collectors.toMap(FieldMeta::getColumnName, cm -> cm));
//    }
//
//    @Override
//    public void set(PropertyTokenizer prop, Object value) {
//        String columnName = prop.getName();
//        System.out.println("---------------> set: columnName=" + columnName + "， value=" + value);
//        FieldMeta fieldMeta = Optional.ofNullable(fieldNameMap.get(columnName)).orElse(columnNameMap.get(columnName));
//        //不关心查询了不存在的列
//        if (fieldMeta == null) {
//            super.set(prop, value);
//            return;
//        }
//        Object newValue = ConverterUtils.convertToEntityAttribute(fieldMeta, fieldMeta.getField().getType(), value);
////        ReflectUtils.setFieldValue(instance, fieldMeta.getField(), v);
//        //使用默认的赋值吧
//        super.set(prop, newValue);
//    }
//}

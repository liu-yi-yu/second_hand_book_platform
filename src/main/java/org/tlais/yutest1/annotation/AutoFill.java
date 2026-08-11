package org.tlais.yutest1.annotation;

import org.tlais.yutest1.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * 自定义注解，用于标识需要自动填充的属性
 */
@Target(ElementType.METHOD)/// 表示该注解用于方法上
@Retention(RetentionPolicy.RUNTIME)// 表示该注解在运行时保留
public @interface AutoFill {
    // 用于指定填充数据的操作类型
    //opertionType是类型，value()是属性，不是方法
    //@AutoFill(value = OperationType.INSERT)
    OperationType value();
}

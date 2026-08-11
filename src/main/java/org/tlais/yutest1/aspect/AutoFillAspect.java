package org.tlais.yutest1.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.tlais.yutest1.annotation.AutoFill;
import org.tlais.yutest1.constant.AutoFillConstant;
import org.tlais.yutest1.context.BaseContext;
import org.tlais.yutest1.enumeration.OperationType;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
 * 自定义切面，实现自动填充功能
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    // 定义切点
    @Pointcut("@annotation(org.tlais.yutest1.annotation.AutoFill))&&execution(* org.tlais.yutest1.mapper.*.*(..))")
    public void autoFillPointcut() {}

    @Before("autoFillPointcut()")
    public void before(JoinPoint joinPoint) throws Throwable {
        String name = joinPoint.getSignature().getName();
        log.info("开始进行数据填充{}", name);

        //获取到当前被拦截的方法上的数据库操作类型
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType value = autoFill.value();
        log.info("数据库操作类型{}", value);

        //获取到方法参数(约定：实体对象放在第一个)
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) return;
        Object object = args[0];

        // 根据对应的数据库操作类型，为对应的属性赋值
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //Long currentId = BaseContext.getCurrentId();
        if (value == OperationType.INSERT) {
            Method setCreateTime = object.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, String.class);
//            Method setCreateUser = object.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
//            Method setUpdateUser = object.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
            Method setUpdateTime = object.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, String.class);

            setCreateTime.invoke(object, now);
            setUpdateTime.invoke(object, now);
//            setCreateUser.invoke(object, currentId);
//            setUpdateUser.invoke(object, currentId);
        }
        else if (value == OperationType.UPDATE) {
            Method setUpdateTime = object.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
            //Method setUpdateUser = object.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

            setUpdateTime.invoke(object, LocalDateTime.now());
            //setUpdateUser.invoke(object, currentId);
        }

    }
}

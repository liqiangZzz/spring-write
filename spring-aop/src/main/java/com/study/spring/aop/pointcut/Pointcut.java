package com.study.spring.aop.pointcut;

import java.lang.reflect.Method;

/**
 * @ClassName Pointcut
 * @Description 切入点
 * @Author liqiang
 * @Date 2025/9/28 15:39
 */
public interface Pointcut {


    /**
     * 匹配类
     */
    boolean matchClass(Class<?> targetClass);

    /**
     * 匹配方法
     */
    boolean matchMethod(Method method, Class<?> targetClass);
}

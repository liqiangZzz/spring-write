package com.study.spring.aop.advice;

import java.lang.reflect.Method;

/**
 * @InterfaceName AfterReturningAdvice
 * @Description 后置返回通知
 * @Author liqiang
 * @Date 2025-09-28 15:27
 */
public interface AfterReturningAdvice extends Advice{


    /**
     * 后置返回通知
     * @param returnValue 返回值
     * @param method  被增强的方法
     * @param args 方法参数
     * @param target 方法的所属对象
     * @throws Throwable
     */
    void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable;
}

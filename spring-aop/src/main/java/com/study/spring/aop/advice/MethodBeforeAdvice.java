package com.study.spring.aop.advice;

import java.lang.reflect.Method;

/**
 * @InterfaceName MethodBeforeAdvice
 * @Description 前置增强
 * @Author liqiang
 * @Date 2025-09-28 15:28
 */
public interface MethodBeforeAdvice extends Advice {


    /**
     * 前置增强
     * @param method 目标方法(被增强方法)
     * @param args 方法参数
     * @param target 被增强的目标对象
     * @throws Throwable
     */
    void before(Method method, Object[] args, Object target) throws Throwable;
}

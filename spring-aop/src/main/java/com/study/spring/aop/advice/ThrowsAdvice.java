package com.study.spring.aop.advice;

import java.lang.reflect.Method;

/**
 * @InterfaceName ThrowsAdvice
 * @Description 异常通知
 * @Author liqiang
 * @Date 2025-09-28 15:29
 */
public interface ThrowsAdvice extends Advice{


    /**
     * 异常通知
     * @param method   被增强的方法
     * @param args     方法参数
     * @param target   方法所属对象
     * @param ex       方法抛出的异常
     * @throws Throwable
     */
    void afterThrowing(Method method, Object[] args, Object target, Exception ex) throws Throwable;
}

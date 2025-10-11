package com.study.spring.aop.advice;

import java.lang.reflect.Method;

/**
 * @InterfaceName AfterAdvice
 * @Description 最终通知
 * @Author liqiang
 * @Date 2025-09-28 15:27
 */
public interface AfterAdvice extends Advice {


    /**
     * 最终通知
     *
     * @param returnValue 方法返回值
     * @param method      目标方法（被增强的方法）
     * @param args        方法参数
     * @param target      方法的所属对象
     * @throws Throwable
     */
    void after(Object returnValue, Method method, Object[] args, Object target) throws Throwable;
}

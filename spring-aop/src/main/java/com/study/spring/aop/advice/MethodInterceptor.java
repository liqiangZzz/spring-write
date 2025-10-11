package com.study.spring.aop.advice;

import java.lang.reflect.Method;

/**
 * @InterfaceName MethodInterceptor
 * @Description 环绕通知
 * @Author liqiang
 * @Date 2025-09-28 15:29
 */
public interface MethodInterceptor extends Advice{


    /**
     * 对方法进行环绕（前置、后置）增强、异常处理增强，方法实现中需调用目标方法。
     * @param method   被增强的方法
     * @param args     方法参数
     * @param target   方法所属对象
     * @return 方法返回值
     * @throws Throwable
     */
    Object invoke(Method method, Object[] args, Object target) throws Throwable;
}

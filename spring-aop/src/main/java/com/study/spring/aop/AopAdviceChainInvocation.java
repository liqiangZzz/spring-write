package com.study.spring.aop;

import com.study.spring.aop.advice.*;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @ClassName AopAdviceChainInvocation
 * @Description  AopAdviceChainInvocation 类用于实现 AOP 拦截器链的调用逻辑。
 *   该类通过责任链模式依次执行各种类型的通知（advice），包括前置通知、环绕通知、后置返回通知、最终通知和异常通知。
 *   在所有通知执行完成后，最终调用目标方法。
 * @Author liqiang
 * @Date 2025/9/29 11:18
 */
public class AopAdviceChainInvocation {

    // 静态获取 invoke 方法的反射对象，用于在环绕通知中传递调用链
    private static Method invokeMethod;


    static {
        try {
            invokeMethod = AopAdviceChainInvocation.class.getDeclaredMethod("invoke", null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    // 代理对象
    private Object proxy;
    // 目标对象
    private Object target;
    // 被代理的方法
    private Method method;
    // 方法参数
    private Object[] args;
    // 拦截器链（通知列表）
    private List<Object> advices;

    public AopAdviceChainInvocation(Object proxy, Object target, Method method, Object[] args, List<Object> advices) {
        super();
        this.proxy = proxy;
        this.target = target;
        this.method = method;
        this.args = args;
        this.advices = advices;
    }

    // AOP 链调用 责任链执行记录索引号
    private int i = 0;


    /**
     * 执行拦截器链中的增强逻辑，按照顺序调用各种类型的通知（advice）
     * 该方法通过递归方式遍历拦截器链，根据不同的通知类型执行相应的增强逻辑
     *
     * @return 方法执行的返回值
     * @throws Throwable 方法执行过程中可能抛出的异常
     */
    public Object invoke() throws Throwable {
        // 当前索引小于通知列表大小时，继续执行通知逻辑
        if (i < this.advices.size()) {
            // 获取当前通知并递增索引
            Object advice = this.advices.get(i++);
            // 前置通知处理
            if (advice instanceof MethodBeforeAdvice) {
                // 执行前置增强
                ((MethodBeforeAdvice) advice).before(method, args, target);
                // 环绕通知处理
            } else if (advice instanceof MethodInterceptor) {
                // 执行环绕增强和异常处理增强。注意这里给入的method 和 对象 是invoke方法和链对象
                return ((MethodInterceptor) advice).invoke(invokeMethod, null, this);
                // 后置返回通知处理
            } else if (advice instanceof AfterReturningAdvice) {
                // 当是AfterReturning后置返回通知增强时，先得得到结果，再执行后置增强逻辑
                Object returnValue = this.invoke();
                ((AfterReturningAdvice) advice).afterReturning(returnValue, method, args, target);
                return returnValue;
                // 最终通知处理
            } else if (advice instanceof AfterAdvice) {
                // 当是最终通知
                Object returnValue = null;
                try {
                    returnValue = this.invoke();
                } finally {
                    ((AfterAdvice) advice).after(returnValue, method, args, target);
                }
                return returnValue;
                // 异常通知处理
            } else if (advice instanceof ThrowsAdvice) {
                try {
                    return this.invoke();
                } catch (Exception e) {
                    ((ThrowsAdvice) advice).afterThrowing(method, args, target, e);
                    throw e;
                }
            }
        } else {
            // 执行目标方法
            return method.invoke(target, args);
        }

        return this.invoke();
    }
}

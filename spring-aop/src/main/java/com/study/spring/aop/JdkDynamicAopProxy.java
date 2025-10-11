package com.study.spring.aop;

import com.study.spring.aop.advisor.Advisor;
import com.study.spring.aop.beans.factory.BeanFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @ClassName JdkDynamicAopProxy
 * @Description
 * @Author liqiang
 * @Date 2025/9/29 10:41
 */
@Slf4j
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {

    // 被代理的Bean名称
    private String beanName;

    // 被代理的Bean实例
    private Object target;

    // 匹配的Advisor列表
    private List<Advisor> matchAdvisors;

    // BeanFactory
    private BeanFactory beanFactory;

    public JdkDynamicAopProxy(String beanName, Object target, List<Advisor> matchAdvisors, BeanFactory beanFactory) {
        super();
        this.beanName = beanName;
        this.target = target;
        this.matchAdvisors = matchAdvisors;
        this.beanFactory = beanFactory;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return AopProxyUtils.applyAdvices(target, method, args, matchAdvisors, proxy, beanFactory);
    }

    @Override
    public Object getProxy() {
        return this.getProxy(target.getClass().getClassLoader());
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        if (log.isDebugEnabled()) {
            log.debug("为{}创建代理。", target);
        }
        return Proxy.newProxyInstance(classLoader, target.getClass().getInterfaces(), this);
    }


}

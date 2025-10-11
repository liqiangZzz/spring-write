package com.study.spring.aop.factory;

import com.study.spring.aop.AopProxy;
import com.study.spring.aop.advisor.Advisor;
import com.study.spring.aop.beans.factory.BeanFactory;

import java.util.List;

/**
 * @InterfaceName AopProxyFactory
 * @Description AOP代理工厂
 * @Author liqiang
 * @Date 2025-09-29 10:36
 */
public interface AopProxyFactory {

    AopProxy createAopProxy(Object bean, String beanName, List<Advisor> matchAdvisors, BeanFactory beanFactory)
            throws Throwable;

    /**
     * 获得默认的AopProxyFactory实例
     *
     * @return AopProxyFactory
     */
    static AopProxyFactory getDefaultAopProxyFactory() {
        return new DefaultAopProxyFactory();
    }
}

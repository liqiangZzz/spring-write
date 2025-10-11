package com.study.spring.aop.factory;

import com.study.spring.aop.AopProxy;
import com.study.spring.aop.CglibDynamicAopProxy;
import com.study.spring.aop.JdkDynamicAopProxy;
import com.study.spring.aop.advisor.Advisor;
import com.study.spring.aop.beans.factory.BeanFactory;

import java.util.List;

/**
 * @ClassName DefaultAopProxyFactory
 * @Description 默认的AOP代理工厂
 * @Author liqiang
 * @Date 2025/9/29 10:38
 */
public class DefaultAopProxyFactory implements AopProxyFactory{
    @Override
    public AopProxy createAopProxy(Object bean, String beanName, List<Advisor> matchAdvisors, BeanFactory beanFactory) throws Throwable {
        //判断使用JDK动态代理还是CGLIB动态代理
        if (shouldUseJDKDynamicProxy(bean, beanName)) {
            return new JdkDynamicAopProxy(beanName, bean, matchAdvisors, beanFactory);
        } else {
            return new CglibDynamicAopProxy(beanName, bean, matchAdvisors, beanFactory);
        }
    }


    private boolean shouldUseJDKDynamicProxy(Object bean, String beanName) {
        // 如何判断？
        // 这样可以吗：有实现接口就用JDK,没有就用cglib？
        // 请同学们实现
        return false;
    }

}

package com.study.spring.aop;

import com.study.spring.aop.advisor.Advisor;
import com.study.spring.aop.beans.BeanDefinition;
import com.study.spring.aop.beans.factory.BeanFactory;
import com.study.spring.aop.beans.factory.DefaultBeanFactory;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @ClassName CglibDynamicAopProxy
 * @Description
 * @Author liqiang
 * @Date 2025/9/29 10:46
 */
@Slf4j
public class CglibDynamicAopProxy implements AopProxy, MethodInterceptor {

    // CGLIB
    private static Enhancer enhancer = new Enhancer();

    // 被代理的Bean名称
    private String beanName;

    // 被代理的Bean实例
    private Object target;

    // 匹配的Advisor
    private List<Advisor> matchAdvisors;

    // BeanFactory
    private BeanFactory beanFactory;

    public CglibDynamicAopProxy(String beanName, Object target, List<Advisor> matchAdvisors, BeanFactory beanFactory) {
        super();
        this.beanName = beanName;
        this.target = target;
        this.matchAdvisors = matchAdvisors;
        this.beanFactory = beanFactory;
    }



    @Override
    public Object getProxy() {
        return this.getProxy(target.getClass().getClassLoader());
    }

    @Override
    public Object getProxy(ClassLoader classLoader) {
        if (log.isDebugEnabled()) {
            log.debug("为{}创建cglib代理。", target);
        }
        Class<?> superClass = this.target.getClass();
        enhancer.setSuperclass(superClass);
        enhancer.setInterfaces(this.getClass().getInterfaces());
        enhancer.setCallback(this);

        Constructor< ?> constructor = null;
        try {
            constructor = superClass.getConstructor();
        } catch (NoSuchMethodException e) {
        }
        if (constructor != null) {
            return enhancer.create();
        } else {
            BeanDefinition bd = ((DefaultBeanFactory) beanFactory).getBeanDefinition(beanName);
            return enhancer.create(bd.getConstructor().getParameterTypes(), bd.getConstructorArgumentRealValues());
        }
    }


    /**
     * 拦截代理对象的方法调用，应用AOP通知
     *
     * @param proxy 被代理的对象
     * @param method 被调用的方法
     * @param args 方法参数数组
     * @param methodProxy 方法代理对象
     * @return 方法执行结果
     * @throws Throwable 方法执行过程中抛出的异常
     */
    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        return AopProxyUtils.applyAdvices(target, method, args, matchAdvisors, proxy, beanFactory);
    }
}

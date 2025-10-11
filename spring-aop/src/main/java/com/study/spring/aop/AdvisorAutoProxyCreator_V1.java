package com.study.spring.aop;


import com.study.spring.aop.beans.BeanPostProcessor;

/**
 * @ClassName AdvisorAutoProxyCreator_V1
 * @Description
 * @Author liqiang
 * @Date 2025/9/28 16:37
 */
public class AdvisorAutoProxyCreator_V1 implements BeanPostProcessor {


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Throwable {
        /*
         * 逻辑：在初始化之后，
         * 1. 判断bean是否需要增强
         * 2. 创建代理对象实现增强
         *
         */
        return bean;
    }
}

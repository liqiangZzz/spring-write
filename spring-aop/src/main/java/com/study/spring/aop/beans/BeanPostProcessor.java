package com.study.spring.aop.beans;

/**
 * @ClassName BeanPostProcessor
 * @Description
 * @Author liqiang
 * @Date 2025/9/28 16:17
 */
public interface BeanPostProcessor {

    /**
     * 初始化之前
     *
     * @param bean     初始化之前的对象
     * @param beanName 对象名
     * @return
     * @throws Exception
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) throws Throwable {
        return bean;
    }

    /**
     * 初始化之后
     *
     * @param bean     初始化之后的对象
     * @param beanName 对象名
     * @return
     * @throws Exception
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) throws Throwable {
        return bean;
    }

}

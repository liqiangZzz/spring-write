package com.study.spring.aop.beans.registry;


import com.study.spring.aop.beans.BeanDefinition;
import com.study.spring.aop.exception.BeanDefinitionRegistryException;

/**
 * @InterfaceName BeanDefinitionRegistry
 * @Description BeanDefinition注册接口
 * @Author liqiang
 * @Date 2025-09-18 16:14
 */
public interface BeanDefinitionRegistry extends AliasRegistry{

    /**
     * 注册BeanDefinition
     */
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionRegistryException;


    /**
     * 获取BeanDefinition
     */
    BeanDefinition getBeanDefinition(String beanName);


    /**
     * 判断是否包含BeanDefinition
     */
    boolean containsBeanDefinition(String beanName);
}

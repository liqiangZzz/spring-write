package com.study.spring.registry;

import com.spring.beans.BeanDefinition;
import com.spring.exception.BeanDefinitionRegistryException;

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
     * 注册BeanDefinition
     */
    void registerType() throws BeanDefinitionRegistryException, NoSuchMethodException;


    /**
     * 获取BeanDefinition
     */
    BeanDefinition getBeanDefinition(String beanName);


    /**
     * 判断是否包含BeanDefinition
     */
    boolean containsBeanDefinition(String beanName);
}

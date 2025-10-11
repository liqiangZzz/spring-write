package com.study.spring.aop.beans.factory;


import com.study.spring.aop.beans.BeanPostProcessor;

import java.util.List;
import java.util.Map;

/**
 * @ClassName BeanFactory
 * @Description Bean工厂接口
 * @Author liqiang
 * @Date 2025/9/18 16:13
 */
public interface BeanFactory {

    Object getBean(String beanName) throws Throwable;

    <T> T getBean(Class<T> type) throws Throwable;

    <T> Map<String,T> getBeansOfType(Class<T> type)throws Throwable;

    Class<?> getType(String beanName) throws NoSuchMethodException;

    void registerBeanPostProcessor(BeanPostProcessor bpp);

    <T> List<T> getBeansOfTypeList(Class<T> type) throws Throwable;
}

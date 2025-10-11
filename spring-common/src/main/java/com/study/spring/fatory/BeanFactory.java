package com.study.spring.fatory;

import java.util.Map;

/**
 * @ClassName BeanFactory
 * @Description Bean工厂接口
 * @Author liqiang
 * @Date 2025/9/18 16:13
 */
public interface BeanFactory {

    Object getBean(String beanName) throws Exception ;

    <T> T getBean(Class<T> type) throws Exception;

    <T> Map<String,T> getBeanOfType(Class<T> type) throws Exception;

    Class<?> getType(String beanName) throws NoSuchMethodException;
}

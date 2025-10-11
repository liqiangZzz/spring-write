package com.study.spring.aop.beans;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @InterfaceName BeanDefinition
 * @Description bean 定义接口
 * @Author liqiang
 * @Date 2025-09-18 15:36
 */
public interface BeanDefinition {

    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    /**
     * 获取bean的class
     */
    Class<?> getBeanClass();

    /**
     * 获取bean的scope
     */
    String getScope();

    /**
     * 判断是否是单例
     */
    boolean isSingleton();

    /**
     * 判断是否是原型
     */
    boolean isPrototype();


    /**
     * 获取bean的工厂bean名称
     */
    String getFactoryBeanName();

    /**
     * 获取bean的工厂方法名称
     */
    String getFactoryMethodName();

    /**
     * 获取bean的初始化方法名称
     */
    String getInitMethodName();

    /**
     * 获取bean的销毁方法名称
     */
    String getDestroyMethodName();

    /**
     *  判断是否是主对象
     */
    boolean isPrimary();


    /**
     * 验证bean定义的合法性
     */
    default boolean validate(){
        // 没定义class,工厂bean或工厂方法没指定，则不合法。
        if (this.getBeanClass() == null) {
            if (StringUtils.isBlank(getFactoryBeanName()) || StringUtils.isBlank(getFactoryMethodName())) {
                return false;
            }
        }

        // 定义了类，又定义工厂bean，不合法
        if (this.getBeanClass() != null && StringUtils.isNotBlank(getFactoryBeanName())) {
            return false;
        }

        return true;
    }


    /**
     * 获得构造参数定义 <br>
     * add in V2
     */
    List<?> getConstructorArgumentValues();

    /**
     * 属性依赖<br>
     * add in V2
     *
     * @return
     */
    List<PropertyValue> getPropertyValues();

    /* 下面的四个方法是供beanFactory中使用的 */

    public Constructor<?> getConstructor();

    public void setConstructor(Constructor<?> constructor);

    public Method getFactoryMethod();

    public void setFactoryMethod(Method factoryMethod);

    public Object[] getConstructorArgumentRealValues();

    public void setConstructorArgumentRealValues(Object[] values);
}

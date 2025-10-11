package com.study.spring.context;


import com.study.spring.aop.beans.BeanPostProcessor;
import com.study.spring.aop.beans.factory.PreBuildBeanFactory;

import java.util.List;
import java.util.Map;

/**
 * @ClassName AbstractApplicationContext
 * @Description 抽象应用上下文
 * @Author liqiang
 * @Date 2025/10/10 14:25
 */
public class AbstractApplicationContext implements ApplicationContext {


    protected PreBuildBeanFactory beanFactory;

    public AbstractApplicationContext() {
        super();
        this.beanFactory = new PreBuildBeanFactory();
    }

    /**
     * 刷新容器
     */
    protected void refresh() throws Throwable {
        // 1、 注册bean的类型
        beanFactory.registerTypeMap();
        // 2、 注册bean的后置处理器
        doRegisterBeanPostProcessor();
        // 3、 预实例化单例
        beanFactory.preInstantiateSingletons();
    }


    /**
     *  注册BeanPostProcessor
     */
    private void doRegisterBeanPostProcessor() throws Throwable {
        // 从BeanFactory中得到所有用户配置的BeanPostProcessor类型的Bean实例，注册到BeanFactory
        List<BeanPostProcessor> beanPostProcessors = beanFactory.getBeansOfTypeList(BeanPostProcessor.class);
        if (beanPostProcessors != null) {
            beanPostProcessors.forEach(beanFactory::registerBeanPostProcessor);
        }
    }


    @Override
    public Object getBean(String beanName) throws Throwable {
        return beanFactory.getBean(beanName);
    }

    @Override
    public <T> T getBean(Class<T> type) throws Throwable {
        return beanFactory.getBean(type);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws Throwable {
        return beanFactory.getBeansOfType(type);
    }

    @Override
    public Class<?> getType(String beanName) throws NoSuchMethodException {
        return beanFactory.getType(beanName);
    }

    @Override
    public void registerBeanPostProcessor(BeanPostProcessor bpp) {
        beanFactory.registerBeanPostProcessor(bpp);
    }

    @Override
    public <T> List<T> getBeansOfTypeList(Class<T> type) throws Throwable {
        return beanFactory.getBeansOfTypeList(type);
    }
}

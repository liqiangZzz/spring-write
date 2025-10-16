package com.spring.ioc.factory;

import com.study.spring.beans.BeanDefinition;
import com.study.spring.exception.BeanDefinitionRegistryException;
import com.study.spring.fatory.BeanFactory;
import com.study.spring.registry.BeanDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName DefaultBeanFactory2_back
 * @Description 默认的Bean工厂 销毁单例Bean
 * @Author liqiang
 * @Date 2025/9/18 16:16
 */
@Slf4j
public class DefaultBeanFactory2_back implements BeanFactory, BeanDefinitionRegistry, Closeable {

    protected Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

    private Map<String, Object> singletonBeanMap = new ConcurrentHashMap<>(256);

    private Map<Class<?>, Set<String>> typeMap = new ConcurrentHashMap<>(256);


    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionRegistryException {
        Objects.requireNonNull(beanName, "注册bean需要给入beanName");
        Objects.requireNonNull(beanDefinition, "注册bean需要给入beanDefinition");

        //校验备案是否合法
        if (!beanDefinition.validate()) {
            throw new BeanDefinitionRegistryException("名字为[" + beanName + "] 的bean定义不合法：" + beanDefinition);
        }

        /*Spring中默认是不可覆盖（抛异常）,可通过参数 spring.main.allow-bean-definition-overriding: true 来允许覆盖*/
        if (this.containsBeanDefinition(beanName)) {
            throw new BeanDefinitionRegistryException("名字为[" + beanName + "] 的bean定义已存在:" + this.getBeanDefinition(beanName));
        }

        this.beanDefinitionMap.put(beanName, beanDefinition);
    }

    @Override
    public void registerType() throws BeanDefinitionRegistryException, NoSuchMethodException {
        for (String beanName : this.beanDefinitionMap.keySet()) {
            Class<?> type = this.getType(beanName);
            //映射本类
            this.registerTypeMap(beanName, type);
            //父类
            this.registerSuperClassTypeMap(beanName, type);
            //接口
            this.registerInterfaceTypeMap(beanName, type);
        }
    }


    /**
     * 注册类型映射
     */
    private void registerTypeMap(String beanName, Class<?> type) {
        Set<String> names2type = this.typeMap.computeIfAbsent(type, k -> new HashSet<>());
        names2type.add(beanName);
    }

    /**
     * 注册父类映射
     */
    private void registerSuperClassTypeMap(String beanName, Class<?> type) {
        Class<?> superClass = type.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            this.registerTypeMap(beanName, superClass);
            //递归找父类
            this.registerSuperClassTypeMap(beanName, superClass);
            //找父类实现的接口注册
            this.registerInterfaceTypeMap(beanName, superClass);
        }

    }

    /**
     * 注册接口映射
     */
    private void registerInterfaceTypeMap(String beanName, Class<?> type) {
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            //注册接口
            this.registerTypeMap(beanName, anInterface);
            //递归找父接口
            this.registerInterfaceTypeMap(beanName, anInterface);
        }
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanDefinitionMap.get(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return this.beanDefinitionMap.containsKey(beanName);
    }


    @Override
    public Object getBean(String beanName) throws Exception {
        return this.doGetBean(beanName);
    }

    /**
     * 获取bean
     */
    private Object doGetBean(String beanName) throws Exception {
        Objects.requireNonNull(beanName, "beanName不能为空");

        BeanDefinition beanDefinition = this.getBeanDefinition(beanName);
        Objects.requireNonNull(beanDefinition, "beanDefinition不能为空");

        Object instance;

        //如果等于 null
        if (beanDefinition.isSingleton()) {
            synchronized (this.singletonBeanMap) {
                instance = singletonBeanMap.get(beanName);
                // 第二次检查
                if (instance == null) {
                    instance = doCreateInstance(beanDefinition);
                    singletonBeanMap.put(beanName, instance);
                }
                // volatile
            }
        } else {
            instance = doCreateInstance(beanDefinition);
        }

        return instance;
    }

    /**
     * 实例化对象
     */
    private Object doCreateInstance(BeanDefinition beanDefinition) throws Exception {
        Class<?> type = beanDefinition.getBeanClass();
        Object instance = null;
        if (type != null) {
            if (StringUtils.isBlank(beanDefinition.getFactoryBeanName())) {
                // 构造方法方式来构造对象
                instance = this.createInstanceByConstructor(beanDefinition);
            } else {
                // 工厂bean方式来构造对象
                instance = this.createInstanceByStaticFactoryMethod(beanDefinition);
            }
        } else {
            // 工厂bean方式来构造对象
            instance = this.createInstanceByFactoryBean(beanDefinition);
        }

        // 执行初始化方法
        this.doInit(beanDefinition, instance);
        return instance;
    }


    /**
     * 通过构造方法来创建实例
     */
    private Object createInstanceByConstructor(BeanDefinition beanDefinition) throws InstantiationException, IllegalAccessException {
        try {
            return beanDefinition.getBeanClass().newInstance();
        } catch (SecurityException e1) {
            log.error("创建bean的实例异常,beanDefinition：" + beanDefinition, e1);
            throw e1;
        }
    }

    /**
     * 通过静态工厂方法来创建实例
     */
    private Object createInstanceByStaticFactoryMethod(BeanDefinition beanDefinition) throws Exception {
        Class<?> type = beanDefinition.getBeanClass();
        Method method = type.getMethod(beanDefinition.getFactoryMethodName(), null);
        return method.invoke(type, null);

    }

    /**
     * 通过工厂bean来创建实例
     */
    private Object createInstanceByFactoryBean(BeanDefinition beanDefinition) throws Exception {
        //调用doGetBean()获取工厂Bean实例
        Object factoryBean = this.doGetBean(beanDefinition.getFactoryBeanName());
        // 通过反射获取工厂Bean中指定的工厂方法
        Method m = factoryBean.getClass().getMethod(beanDefinition.getFactoryMethodName(), null);
        //执行该工厂方法创建并返回Bean实例
        return m.invoke(factoryBean, null);
    }


    /**
     * 初始化
     */
    private void doInit(BeanDefinition beanDefinition, Object instance) throws Exception {
        // 执行初始化方法
        if (StringUtils.isNotBlank(beanDefinition.getInitMethodName())) {
            Method m = instance.getClass().getMethod(beanDefinition.getInitMethodName(), null);
            m.invoke(instance, null);
        }
    }


    @Override
    public <T> T getBean(Class<T> type) {
        return null;
    }

    @Override
    public <T> Map<String, T> getBeanOfType(Class<T> type) {
        return Collections.emptyMap();
    }

    @Override
    public Class<?> getType(String beanName) throws NoSuchMethodException {
        BeanDefinition beanDefinition = this.getBeanDefinition(beanName);
        Class<?> type = beanDefinition.getBeanClass();
        if (type != null) {
            if (StringUtils.isBlank(beanDefinition.getFactoryMethodName())) {
                // 构造方法来构造对象的，Type就是beanClass,不需做什么。
            } else {
                // 静态工厂方法方式的，反射获得Method,再获取Method的返回值类型
                type = type.getDeclaredMethod(beanDefinition.getFactoryMethodName(), null).getReturnType();
            }
            //如果未定义 beanClass，说明是工厂 Bean 方式创建
        } else {
            // 先递归获取工厂 Bean 的类型。
            type = this.getType(beanDefinition.getFactoryBeanName());
            // 再通过反射获取工厂方法的返回值类型。
            type = type.getDeclaredMethod(beanDefinition.getFactoryMethodName(), null).getReturnType();
        }
        return type;
    }

    /**
     * 销毁方法
     */
    @Override
    public void close() throws IOException {

        // 执行单例实例的销毁方法
        for (Map.Entry<String, BeanDefinition> entry : this.beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();

            // 单例且定义销毁方法
            if (beanDefinition.isSingleton() && StringUtils.isNoneBlank(beanDefinition.getDestroyMethodName())) {
                Object instance = this.singletonBeanMap.get(beanName);
                try {
                    Method method = instance.getClass().getMethod(beanDefinition.getDestroyMethodName(), null);
                    method.invoke(instance, null);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                         | InvocationTargetException e1) {
                    log.error("执行bean[" + beanName + "] " + beanDefinition + " 的 销毁方法异常！", e1);
                }
            }

        }
    }
}

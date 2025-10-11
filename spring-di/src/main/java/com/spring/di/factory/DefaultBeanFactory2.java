package com.spring.di.factory;

import com.spring.beans.BeanDefinition;
import com.spring.beans.BeanReference;
import com.spring.exception.BeanDefinitionRegistryException;
import com.spring.fatory.BeanFactory;
import com.spring.registry.BeanDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName DefaultBeanFactory2
 * @Description 增加了构造参数依赖注入的： 参数真实值获取实现，构造方法方式的构造方法的判定实现
 * @Author liqiang
 * @Date 2025/9/18 16:16
 */
@Slf4j
public class DefaultBeanFactory2 implements BeanFactory, BeanDefinitionRegistry, Closeable {

    // Bean 定义映射表：Bean 名称 -> BeanDefinition
    protected Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

    // 单例 Bean 缓存：Bean 名称 -> Bean 实例（仅缓存单例 Bean）
    private Map<String, Object> singletonBeanMap = new ConcurrentHashMap<>(256);

    // 类型映射表：Class 类型 -> 对应的 Bean 名称集合
    private Map<Class<?>, Set<String>> typeMap = new ConcurrentHashMap<>(256);

    // 别名映射表：别名 -> 原始名称
    protected final Map<String, String> aliasMap = new ConcurrentHashMap<>(256);


    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionRegistryException {
        Objects.requireNonNull(beanName, "注册bean需要给入beanName");
        Objects.requireNonNull(beanDefinition, "注册bean需要给入beanDefinition");

        //娇艳备案是否合法
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
    private Object doGetBean(String name) throws Exception {
        Objects.requireNonNull(name, "beanName不能为空");

        // 解析别名，获取真实的 Bean 名称
        String beanName = resolveBeanName(name);

        Object instance = singletonBeanMap.get(beanName);

        if (instance != null) {
            return instance;
        }


        BeanDefinition beanDefinition = this.getBeanDefinition(beanName);
        Objects.requireNonNull(beanDefinition, "beanDefinition不能为空");


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
    private Object createInstanceByConstructor(BeanDefinition beanDefinition) throws Exception {
        // 构造参数依赖注入，这里需要做些什么？

        /*
        1 得到真正的参数值，因为
        List<?> constructorArgumentValues = bd.getConstructorArgumentValues();
        constructorArgumentValues 中可能有 BeanReference
        */
        Object[] args = getConstructorArgumentValues(beanDefinition);
        // 2 判定该调用哪个构造方法来创建实例
        return this.determineConstructor(beanDefinition, args).newInstance(args);

    }

    private Object[] getConstructorArgumentValues(BeanDefinition beanDefinition) throws Exception {
        // 获取Bean定义中的构造函数参数值列表
        List<?> constructorArgumentValues = beanDefinition.getConstructorArgumentValues();
        if (CollectionUtils.isEmpty(constructorArgumentValues)) {
            return null;
        }
        // 将参数值列表转换为数组
        Object[] values = new Object[constructorArgumentValues.size()];
        int i = 0;
        for (Object originalValue : constructorArgumentValues) {
            values[i++] = getOneArgumentRealValue(originalValue);
        }
        return values;
    }


    private Object getOneArgumentRealValue(Object originalValue) throws Exception {
        //获取真正的参数值，主要是处理BeanReference ，得到真的的Bean实例
        Object realValue = null;
        if (originalValue != null) {
            if (originalValue instanceof BeanReference) {
                // 处理Bean引用：根据名称或类型获取真实的Bean实例
                BeanReference beanReference = (BeanReference) originalValue;
                if (StringUtils.isNotBlank(beanReference.getBeanName())) {
                    realValue = this.getBean(beanReference.getBeanName());
                } else {
                    realValue = this.getBean(beanReference.getType());
                }
            } else if (originalValue instanceof Object[]) {
                // 处理对象数组：递归解析数组中每一个元素的真实值
                Object[] originalArray = (Object[]) originalValue;
                Object[] realArray = new Object[originalArray.length];
                for (int i = 0; i < originalArray.length; i++) {
                    realArray[i] = getOneArgumentRealValue(originalArray[i]);
                }
                realValue = realArray;
            } else if (originalValue instanceof Collection) {
                // 处理集合类型（List/Set）：保持原有集合类型特性并递归解析内部元素
                Collection<?> originalCollection = (Collection<?>) originalValue;
                Collection<Object> realCollection;
                if (originalCollection instanceof List) {
                    realCollection = new ArrayList<>(originalCollection.size());
                } else if (originalCollection instanceof Set) {
                    realCollection = new HashSet<>(originalCollection.size());
                } else {
                    realCollection = new ArrayList<>(originalCollection.size());
                }
                for (Object item : originalCollection) {
                    realCollection.add(getOneArgumentRealValue(item));
                }
                realValue = realCollection;
            } else if (originalValue instanceof Map) {
                // 处理映射表：递归解析键与值的真实值
                Map<?, ?> originalMap = (Map<?, ?>) originalValue;
                Map<Object, Object> realMap = new HashMap<>(originalMap.size());
                for (Map.Entry<?, ?> entry : originalMap.entrySet()) {
                    Object realKey = getOneArgumentRealValue(entry.getKey());
                    Object realValueEntry = getOneArgumentRealValue(entry.getValue());
                    realMap.put(realKey, realValueEntry);
                }
                realValue = realMap;
            } else {
                realValue = originalValue;
            }
        }
        return realValue;
    }


    private Constructor<?> determineConstructor(BeanDefinition bd, Object[] args) throws Exception {
         /*
          判定构造方法的逻辑应是怎样的？
             1 先根据参数的类型进行精确匹配查找，如未找到，则进行第2步查找；
             2 获得所有的构造方法，遍历，通过参数数量过滤，再比对形参类型与实参类型。
         */

        //没有参数，则用无参构造方法
        if (args == null) {
            return bd.getBeanClass().getConstructor(null);
        }

        Constructor<?> ct = null;

        //1、根据参数类型进行精确匹配查找
        Class[] paramTypes = new Class[args.length];
        int j = 0;
        for (Class paramType : paramTypes) {
            paramTypes[j++] = paramType.getClass();
        }

        try {
            ct = bd.getBeanClass().getConstructor(paramTypes);
        } catch (Exception e) {
            // 这个异常不需要处理
        }

        if (ct == null) {
            //2、获得所有的构造方法，遍历，通过参数数量过滤，再比对形参类型与实参类型。
            //  判断逻辑：先判断参数数量，再依次比对形参类型与实参类型
            outer:
            for (Constructor<?> c : bd.getBeanClass().getConstructors()) {
                //通过参数数量过滤
                if (paramTypes.length == args.length) {
                    //再依次比对形参类型与实参类型是否匹配
                    for (int i = 0; i < paramTypes.length; i++) {
                        if (!paramTypes[i].isAssignableFrom(args[i].getClass())) {
                            //参数类型不可赋值（不匹配），跳到外层循环，继续下一个
                            continue outer;
                        }
                    }
                    ct = c;
                    break outer;
                }
            }
        }
        if (ct != null) {
            return ct;
        } else {
            throw new Exception("不存在对应的构造方法！" + bd);
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
            // 添加以下代码来绕过访问检查
            m.setAccessible(true);
            m.invoke(instance, null);
        }
    }


    @Override
    public <T> T getBean(Class<T> type) throws Exception {
   /*
		逻辑：
		1 获得其对应的所有的BeanDefinition
		2 如果只有一个，直接获取bean实例返回，否则
		3 遍历找出Primary的
		4 如果primary没有，或大于1个，抛出异常
		5 返回Primary的实例
		 */
        Set<String> beanNames = this.typeMap.get(type);

        if (beanNames == null || beanNames.isEmpty()) {
            return null;
        }

        // 如果只有一个Bean，直接返回
        if (beanNames.size() == 1) {
            return (T) this.getBean(beanNames.iterator().next());
        }

        //找Primary
        // 收集所有primary bean的名称
        List<String> primaryBeanNames = new ArrayList<>();
        List<String> allBeanNames = new ArrayList<>();

        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = this.getBeanDefinition(beanName);
            allBeanNames.add(beanName);

            if (beanDefinition != null && beanDefinition.isPrimary()) {
                primaryBeanNames.add(beanName);
            }
        }

        // 根据primary bean的数量处理
        if (primaryBeanNames.isEmpty()) {
            String mess = type + " 类型的Bean存在多个" + allBeanNames + " 但无法确定Primary";
            log.error(mess);
            throw new Exception(mess);
        } else if (primaryBeanNames.size() == 1) {
            return (T) this.getBean(primaryBeanNames.get(0));
        } else {
            String mess = type + " 类型的Bean存在多个Primary Bean: " + primaryBeanNames;
            log.error(mess);
            throw new Exception(mess);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getBeanOfType(Class<T> type) throws Exception {
        Map<String, T> result = new HashMap<>();
        Set<String> beanNames = typeMap.get(type);
        if (beanNames != null) {
            for (String beanName : beanNames) {
                result.put(beanName, (T) getBean(beanName));
            }
        }
        return result;
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
                    method.setAccessible(true);
                    method.invoke(instance, null);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                         IllegalArgumentException
                         | InvocationTargetException e1) {
                    log.error("执行bean[" + beanName + "] " + beanDefinition + " 的 销毁方法异常！", e1);
                }
            }

        }
    }

    @Override
    public void registerAlias(String name, String alias) {
        // 参数校验：检查name和alias是否为null
        if (name == null || alias == null) {
            throw new IllegalArgumentException("Name and alias cannot be null");
        }
        // 参数校验：检查name和alias是否相同
        if (name.equals(alias)) {
            throw new IllegalArgumentException("Name and alias cannot be the same");
        }
        // 检查要注册别名的bean是否存在
        if (!beanDefinitionMap.containsKey(name)) {
            throw new RuntimeException("No bean named '" + name + "' is defined");
        }
        // 将别名和对应的原始名称存入别名映射表
        aliasMap.put(alias, name);
    }

    @Override
    public void removeAlias(String alias) {
        aliasMap.remove(alias);
    }

    @Override
    public boolean isAlias(String name) {
        return aliasMap.containsKey(name);
    }

    @Override
    public String getOriginalName(String name) {
        return aliasMap.get(name);
    }


    /**
     * 根据给定的名称获取所有对应的别名
     *
     * @param name 需要查找别名的名称
     * @return 包含所有别名的字符串数组，如果没有找到则返回空数组
     */
    @Override
    public String[] getAliases(String name) {
        // 从别名映射中筛选出值等于指定名称的条目，提取键作为别名并转换为数组
        return aliasMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(name))
                .map(Map.Entry::getKey)
                .toArray(String[]::new);
    }

    /**
     * 解析 Bean 名称（处理别名）
     */
    private String resolveBeanName(String name) {
        String originalName = getOriginalName(name);
        return originalName != null ? originalName : name;
    }


}

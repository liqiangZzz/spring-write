package com.spring.di.factory;

import com.spring.beans.BeanDefinition;
import com.spring.beans.BeanReference;
import com.spring.beans.PropertyValue;
import com.spring.exception.BeanDefinitionRegistryException;
import com.spring.fatory.BeanFactory;
import com.spring.registry.BeanDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName DefaultBeanFactory
 * @Description DefaultBeanFactory2 中增加了构造参数依赖注入的： 参数真实值获取实现，构造方法方式的构造方法的判定实现
 *              DefaultBeanFactory3 中增加 缓存原型bean的构造方法、工厂方法，增加了 静态工厂方法、工厂bean工厂方法的参数依赖实现
 *              DefaultBeanFactory4 加入构造循环依赖检测
 *              DefaultBeanFactory5  增加属性依赖实现
 * @Author liqiang
 * @Date 2025/9/18 16:16
 */
@Slf4j
public class DefaultBeanFactory implements BeanFactory, BeanDefinitionRegistry, Closeable {

    // Bean 定义映射表：Bean 名称 -> BeanDefinition
    protected Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

    // 单例 Bean 缓存：Bean 名称 -> Bean 实例（仅缓存单例 Bean）
    private Map<String, Object> singletonBeanMap = new ConcurrentHashMap<>(256);

    // 类型映射表：Class 类型 -> 对应的 Bean 名称集合
    private Map<Class<?>, Set<String>> typeMap = new ConcurrentHashMap<>(256);

    // 别名映射表：别名 -> 原始名称
    protected final Map<String, String> aliasMap = new ConcurrentHashMap<>(256);

    // 正在创建的 Bean 集合
    private ThreadLocal<Set<String>> buildingBeansRecordor = new ThreadLocal<>();

    // 提前曝光的 Bean 缓存：Bean 名称 -> Bean 实例（仅缓存单例 Bean）
    private ThreadLocal<Map<String, Object>> earlyExposeBuildingBeans = new ThreadLocal<>();


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

        instance = this.getFromEarlyExposeBuildingBeans(beanName);
        if (instance != null) { //这是属性依赖时的循环引用，返回提前暴露的实例
            return instance;
        }


        BeanDefinition beanDefinition = this.getBeanDefinition(beanName);
        Objects.requireNonNull(beanDefinition, "beanDefinition不能为空");

        // 检测循环依赖
        Set<String> buildingBeans = this.buildingBeansRecordor.get();
        if (buildingBeans == null) {
            buildingBeans = new HashSet<>();
            this.buildingBeansRecordor.set(buildingBeans);
        }

        // 检测循环依赖
        if (buildingBeans.contains(beanName)) {
            throw new Exception(beanName + " 循环依赖！" + buildingBeans);
        }

        // 记录正在创建的Bean
        buildingBeans.add(beanName);

        //如果等于 null
        if (beanDefinition.isSingleton()) {
            synchronized (this.singletonBeanMap) {
                instance = singletonBeanMap.get(beanName);
                // 第二次检查
                if (instance == null) {
                    instance = doCreateInstance(beanName, beanDefinition);
                    singletonBeanMap.put(beanName, instance);
                }
                // volatile
            }
        } else {
            instance = doCreateInstance(beanName, beanDefinition);
        }

        // 创建好实例后，移除创建中记录
        buildingBeans.remove(beanName);
        return instance;
    }

    private Object getFromEarlyExposeBuildingBeans(String beanName) {
        Map<String, Object> earlyExposeBuildingBeansMap = earlyExposeBuildingBeans.get();
        return earlyExposeBuildingBeansMap == null ? null : earlyExposeBuildingBeansMap.get(beanName);
    }

    /**
     * 实例化对象
     */
    private Object doCreateInstance(String beanName, BeanDefinition beanDefinition) throws Exception {
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
        //提前暴露正在创建的Bean
        this.doEarlyExposeBuildingBeans(beanName, instance);

        // 给入属性依赖
        this.setPropertyDIValues(beanDefinition, instance);

        // 移除正在创建的Bean
        this.removeEarlyExposeBuildingBeans(beanName, instance);

        // 执行初始化方法
        this.doInit(beanDefinition, instance);

        return instance;
    }


    /**
     * 提前暴露正在创建的Bean
     */
    private void doEarlyExposeBuildingBeans(String beanName, Object instance) {
        // 从线程本地变量中获取提前暴露的Bean映射表
        Map<String, Object> earlyExposeBuildingBeansMap = earlyExposeBuildingBeans.get();
        // 如果映射表为空，则创建新的HashMap并设置到线程本地变量中
        if (earlyExposeBuildingBeansMap == null) {
            earlyExposeBuildingBeansMap = new HashMap<>();
            earlyExposeBuildingBeans.set(earlyExposeBuildingBeansMap);
        }
        // 将Bean名称和实例存入映射表中
        earlyExposeBuildingBeansMap.put(beanName, instance);
    }

    /**
     * 为Bean实例设置属性依赖注入值
     *
     * @param beanDefinition Bean定义信息，包含属性值配置
     * @param instance       Bean实例对象，需要被注入属性值
     * @throws Exception 当反射操作失败时抛出异常
     */
    private void setPropertyDIValues(BeanDefinition beanDefinition, Object instance) throws Exception {
        // 如果没有属性值配置，则直接返回
        if (CollectionUtils.isEmpty(beanDefinition.getPropertyValues())) {
            return;
        }
        // 遍历所有属性值配置，依次进行依赖注入
        for (PropertyValue pv : beanDefinition.getPropertyValues()) {
            // 跳过属性名为空的配置
            if (StringUtils.isBlank(pv.getName())) {
                continue;
            }
            Class<?> clazz = instance.getClass();
            // 获取属性对应的字段对象
            Field p = clazz.getDeclaredField(pv.getName());
            //暴力访问  private
            p.setAccessible(true);
            // 设置属性值，支持依赖注入
            p.set(instance, this.getOneArgumentRealValue(pv.getValue()));

        }
    }

    /**
     * 移除正在创建的Bean
     */
    private void removeEarlyExposeBuildingBeans(String beanName, Object instance) {
        earlyExposeBuildingBeans.get().remove(beanName);
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
        // 和Spring源码保持一致对原型进行缓存避免多次重复查找，
        // 对于原型bean,从第二次开始获取bean实例时，可直接获得第一次缓存的构造方法。
        ct = bd.getConstructor();
        if (ct != null) {
            return ct;
        }

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
            // 对于原型bean,可以缓存找到的构造方法，方便下次构造实例对象。在BeanDefinition中获取设置所用构造方法的方法。
            // 同时在上面增加从beanDefinition中获取的逻辑。
            if (bd.isPrototype()) {
                bd.setConstructor(ct);
            }
            return ct;
        } else {
            throw new Exception("不存在对应的构造方法！" + bd);
        }
    }

    /**
     * 通过静态工厂方法来创建实例
     */
    private Object createInstanceByStaticFactoryMethod(BeanDefinition beanDefinition) throws Exception {
        // 获取构造函数参数值
        Object[] argumentValues = this.getConstructorArgumentValues(beanDefinition);
        // 获取bean的类型信息
        Class<?> beanClass = beanDefinition.getBeanClass();
        // 确定要调用的工厂方法
        Method method = this.determineFactoryMethod(beanDefinition, argumentValues, beanClass);
        // 调用静态工厂方法创建实例
        return method.invoke(beanClass, argumentValues);
    }

    /**
     * 通过FactoryBean创建Bean实例
     */
    private Object createInstanceByFactoryBean(BeanDefinition beanDefinition) throws Exception {

        // 获取构造函数参数值
        Object[] constructorArgumentValues = this.getConstructorArgumentValues(beanDefinition);
        // 确定要调用的工厂方法
        Method method = this.determineFactoryMethod(beanDefinition, constructorArgumentValues, this.getType(beanDefinition.getFactoryBeanName()));
        //执行该工厂方法创建并返回Bean实例
        Object factoryBean = this.doGetBean(beanDefinition.getFactoryBeanName());
        return method.invoke(factoryBean, constructorArgumentValues);
    }

    /**
     * 根据给定的BeanDefinition、参数列表和目标类型，确定用于创建Bean实例的工厂方法。
     * 判定逻辑与构造方法的判定逻辑类似：
     * 1. 先根据实参的类型进行精确匹配查找，如未找到，则进行第2步查找；
     * 2. 获得所有方法，遍历，通过方法名、参数数量过滤，再比对形参类型与实参类型。
     *
     * @param bd   BeanDefinition对象，包含Bean的定义信息，如工厂方法名等
     * @param args 实际传入的参数列表，用于匹配方法参数
     * @param type 目标类的Class对象，用于获取方法信息
     * @return 匹配到的工厂方法（Method对象）
     * @throws Exception 如果没有找到合适的工厂方法，则抛出异常
     */
    private Method determineFactoryMethod(BeanDefinition bd, Object[] args, Class<?> type) throws Exception {
         /*判定工厂方法的逻辑同构造方法的判定逻辑
        1 先根据实参的类型进行精确匹配查找，如未找到，则进行第2步查找；
        2 获得所有方法，遍历，通过方法名、参数数量过滤，再比对形参类型与实参类型。
        * */
        String methodName = bd.getFactoryMethodName();

        if (args == null) {
            return type.getMethod(methodName, null);
        }

        Method method = null;
        //对于原型bean，从第二次开始火哦去bean实例时，可以直接获得第一次缓存的构造方法。
        method = bd.getFactoryMethod();
        if (method != null) {
            return method;
        }

        //1、根据参数类型进行精确匹配查找
        Class<?>[] paramTypes = new Class[args.length];
        int j = 0;
        for (Class paramType : paramTypes) {
            paramTypes[j++] = paramType.getClass();
        }

        try {
            method = type.getMethod(methodName, paramTypes);
        } catch (Exception e) {
            // 这个异常不需要处理
        }

        //同名的工厂方法可能很多，需要通过参数去确认具体是哪个工厂方法
        if (method == null) {

            // 没有精确参数类型匹配的，则遍历匹配所有的方法
            // 2 获得所有方法，遍历，通过方法名、参数数量过滤，再比对形参类型与实参类型。

            outer:
            for (Method m : type.getMethods()) {
                if (m.getName().equals(methodName)) {
                    continue;
                }
                Class<?>[] paramterTypes = method.getParameterTypes();
                if (paramterTypes.length == args.length) {
                    for (int i = 0; i < paramterTypes.length; i++) {
                        if (!paramterTypes[i].isAssignableFrom(args[i].getClass())) {
                            continue outer;
                        }
                    }

                    method = m;
                    break outer;
                }
            }
        }

        if (method != null) {
            // 对于原型bean,可以缓存找到的方法，方便下次构造实例对象。在BeanDefinfition中获取设置所用方法的方法。
            // 同时在上面增加从beanDefinition中获取的逻辑。
            if (bd.isPrototype()) {
                bd.setFactoryMethod(method);
            }
            return method;
        } else {
            throw new Exception("不存在对应的构造方法！" + bd);
        }
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

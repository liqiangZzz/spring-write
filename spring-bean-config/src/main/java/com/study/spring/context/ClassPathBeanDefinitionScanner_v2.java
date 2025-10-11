package com.study.spring.context;

import com.study.spring.aop.beans.BeanReference;
import com.study.spring.aop.beans.GenericBeanDefinition;
import com.study.spring.aop.beans.PropertyValue;
import com.study.spring.aop.beans.registry.BeanDefinitionRegistry;
import com.study.spring.context.annotation.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @ClassName ClassPathBeanDefinitionScanner_v2
 * @Description
 * @Author liqiang
 * @Date 2025/10/10 15:00
 */
public class ClassPathBeanDefinitionScanner_v2 {


    private BeanDefinitionRegistry registry;

    //注意：当在开发时执行单元测试，测试类是另一个类目录，这里可能会导致类名截取不对。部署环境时没问题。
    private int classPathAbsLength = new File(ClassPathBeanDefinitionScanner_v2.class.getResource("/").getPath()).getAbsolutePath().length();

    public ClassPathBeanDefinitionScanner_v2(BeanDefinitionRegistry registry) {
        super();
        this.registry = registry;
    }


    /**
     * 扫描指定包路径下的类文件，解析注解并注册Bean定义
     *
     * @param basePackages 需要扫描的基础包路径数组，可变参数形式传入
     */
    public void scan(String... basePackages) {
        if (basePackages != null && basePackages.length > 0) {
            for (String basePackage : basePackages) {
                /*
				 1 递归扫描包目录下的.class文件
				 2 组合包路径+class文件名 得到全限定类名
				 3 ClassLoad.load("类名") 得到 Class 对象
				 4 解析Class上的注解，获得Bean定义信息，注册Bean定义
				 */

                //1 递归扫描包目录下的.class文件
                Set<File> classFiles = this.doScan(basePackage);
                //2 得到Class对象，并解析注解、注册Bean定义
                this.readAndRegisterBeanDefinition(classFiles);
            }
        }
    }


    /**
     * 扫描指定包下的所有类文件
     *
     * @param basePackage 基础包名，如"com.example.service"
     * @return 返回找到的所有class文件集合
     */
    private Set<File> doScan(String basePackage) {
        // 扫描包下的类
        // 将包名转为路径名
        String basePackagePath = "/" + StringUtils.replace(basePackage, ".", "/");
        //得到包对应的目录
        File rootDir = new File(this.getClass().getResource(basePackagePath).getPath());

        //存放找到的类文件集合
        Set<File> classFiles = new HashSet<>();
        // 调用doRetrieveMatchingFiles来扫描class文件
        this.doRetrieveMatchingFiles(rootDir, classFiles);

        return classFiles;
    }

    /**
     * 递归检索匹配的文件
     * 该函数会遍历指定根目录下的所有文件和子目录，查找以.class结尾的文件并添加到集合中
     *
     * @param rootDir    根目录，从此目录开始递归搜索文件
     * @param classFiles 用于存储找到的.class文件的集合
     */
    private void doRetrieveMatchingFiles(File rootDir, Set<File> classFiles) {
        // 遍历当前目录下的所有文件和子目录
        for (File file : rootDir.listFiles()) {
            // 如果是可读的目录，则递归调用继续搜索
            if (file.isDirectory() && file.canRead()) {
                this.doRetrieveMatchingFiles(file, classFiles);
            }
            // 如果文件名以.class结尾，则将其添加到结果集合中
            if (file.getName().endsWith(".class")) {
                classFiles.add(file);
            }
        }
    }


    /**
     * 读取类文件并注册Bean定义
     * 该方法会遍历传入的类文件集合，加载标记了@Component注解的类，
     * 并为其创建相应的Bean定义，同时处理构造方法和方法上的注解
     *
     * @param classFiles 类文件集合，包含需要扫描和注册的类文件
     */
    private void readAndRegisterBeanDefinition(Set<File> classFiles) {
        for (File classFile : classFiles) {
            String className = getClassNameFromFile(classFile);

            try {
                // 加载类
                Class<?> clazz = this.getClass().getClassLoader().loadClass(className);

                Component component = clazz.getAnnotation(Component.class);
                //  标注了@Component注解
                if (component != null) {
                    String beanName = component.value();
                    if (StringUtils.isBlank(beanName)) {
                        beanName = this.generateBeanName(clazz);
                    }
                    GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                    beanDefinition.setBeanClass(clazz);

                    //处理Scope
                    Scope scope = clazz.getAnnotation(Scope.class);
                    if (scope != null) {
                        beanDefinition.setScope(scope.value());
                    }
                    //处理Primary
                    Primary primary = clazz.getAnnotation(Primary.class);
                    if (primary != null) {
                        beanDefinition.setPrimary(true);
                    }

                    // 处理构造方法，在构造方法上找@Autowired注解，如有，将这个构造方法set到bd;
                    this.handleConstructor(clazz, beanDefinition);

                    //处理方法上的注解（找出初始化、销毁、工厂方法）
                    this.handleMethod(clazz, beanDefinition, beanName);

                    // 处理属性依赖
                    this.handlePropertyDi(clazz, beanDefinition);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * 从类文件中获取类名
     *
     * @param classFile 类文件对象
     * @return 返回对应的类名，使用点号分隔的完整类名
     */
    private String getClassNameFromFile(File classFile) {
        // 获取类文件的绝对路径
        String absPath = classFile.getAbsolutePath();
        // 从绝对路径中提取类名部分，并将文件分隔符替换为点号
        String name = absPath.substring(classPathAbsLength + 1, absPath.indexOf('.'));
        return StringUtils.replace(name, File.separator, ".");
    }

    /**
     * 根据类名生成对应的bean名称
     *
     * @param clazz 需要生成bean名称的类
     * @return 生成的bean名称，规则为类名首字母小写
     */
    private String generateBeanName(Class<?> clazz) {
        //应用名称生成规则生成beanName:  类名首字母小写
        String className = clazz.getName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }


    /**
     * 处理类的构造方法，查找带有@Autowired注解的构造方法并设置到BeanDefinition中
     *
     * @param clazz          需要处理的类
     * @param beanDefinition Bean定义对象，用于存储构造方法信息
     */
    private void handleConstructor(Class<?> clazz, GenericBeanDefinition beanDefinition) {
        // 获得所有构造方法，在构造方法上找@Autowired注解，如有，将这个构造方法set到bd
        Constructor<?>[] constructors = clazz.getConstructors();
        // 遍历所有构造方法，查找带有@Autowired注解的构造方法
        if (constructors != null || constructors.length > 0) {
            for (Constructor<?> constructor : constructors) {
                Autowired autowired = constructor.getAnnotation(Autowired.class);
                if (autowired != null) {
                    // 设置带有@Autowired注解的构造方法
                    beanDefinition.setConstructor(constructor);
                    //构造参数依赖处理
                    beanDefinition.setConstructorArgumentValues(this.handleMethodParameters(constructor.getParameters()));
                    break;
                }
            }
        }
    }

    /**
     * 处理方法参数，解析参数上的注解并创建相应的依赖对象
     *
     * @param parameters 方法参数数组
     * @return 包含参数值或Bean引用的列表
     */
    private List<?> handleMethodParameters(Parameter[] parameters) {
        //遍历获取参数上的注解，及创建构造参数依赖
        List<Object> argValues = new ArrayList<>();
        for (Parameter parameter : parameters) {
            //找@Value注解
            Value v = parameter.getAnnotation(Value.class);
            if (v != null) {
                argValues.add(v.value());
                continue;
            }
            //找@Qualifier
            Qualifier q = parameter.getAnnotation(Qualifier.class);
            if (q != null) {
                argValues.add(new BeanReference(q.value()));
            } else {
                argValues.add(new BeanReference(parameter.getType()));
            }
        }
        return argValues;
    }

    /**
     * 处理类中的方法，查找并设置初始化方法、销毁方法和工厂方法
     *
     * @param clazz          bean的类对象
     * @param beanDefinition bean定义对象
     * @param beanName       bean名称
     */
    private void handleMethod(Class<?> clazz, GenericBeanDefinition beanDefinition, String beanName) {
        //遍历方法找初始化、销毁、工厂方法注解
        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                beanDefinition.setInitMethodName(method.getName());
            } else if (method.isAnnotationPresent(PreDestroy.class)) {
                beanDefinition.setDestroyMethodName(method.getName());
            } else if (method.isAnnotationPresent(Bean.class)) {
                this.handlerFactoryMethod(method, clazz, beanName);
            }
        }
    }


    /**
     * 处理工厂方法，解析方法上的注解并注册Bean定义
     *
     * @param method   工厂方法对象
     * @param clazz    包含工厂方法的类
     * @param beanName Bean名称
     */
    private void handlerFactoryMethod(Method method, Class<?> clazz, String beanName) {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        //静态工厂方法
        if (Modifier.isStatic(method.getModifiers())) {
            bd.setBeanClass(clazz);
        } else { //成员工厂方法，指定工厂Bean名
            bd.setFactoryBeanName(beanName);
        }

        bd.setFactoryMethod(method);
        bd.setFactoryMethodName(method.getName());

        // 处理Scope
        Scope scope = method.getAnnotation(Scope.class);
        if (scope != null) {
            bd.setScope(scope.value());
        }
        // 处理Primary
        Primary primary = method.getAnnotation(Primary.class);
        if (primary != null) {
            bd.setPrimary(true);
        }

        Bean bean = method.getAnnotation(Bean.class);
        //beanName的由来，获取beanName
        String xbeanName = bean.name();
        if (StringUtils.isBlank(xbeanName)) {
            xbeanName = method.getName();
        }

        //初始化方法、销毁方法设置
        if (StringUtils.isNotBlank(bean.initMethod())) {
            bd.setInitMethodName(bean.initMethod());
        }

        if (StringUtils.isNotBlank(bean.destroyMethod())) {
            bd.setDestroyMethodName(bean.destroyMethod());
        }

        //参数依赖处理
        bd.setConstructorArgumentValues(this.handleMethodParameters(method.getParameters()));
        // 注册Bean定义
        this.registry.registerBeanDefinition(xbeanName, bd);
    }

    /**
     * 处理类的属性依赖注入
     * 该方法扫描类中的所有字段，查找带有@Autowired注解的字段，并为其创建相应的Bean引用依赖
     *
     * @param clazz          需要处理的类对象
     * @param beanDefinition 对应的Bean定义对象，用于存储属性值信息
     */
    private void handlePropertyDi(Class<?> clazz, GenericBeanDefinition beanDefinition) {
        // 创建属性值列表并设置到Bean定义中
        List<PropertyValue> propertyValues = new ArrayList<>();
        beanDefinition.setPropertyValues(propertyValues);

        // 遍历类中所有声明的字段
        for (Field field : clazz.getDeclaredFields()) {
            // 检查字段是否带有Autowired注解
            if (field.isAnnotationPresent(Autowired.class)) {
                BeanReference beanReference = null;
                // 获取字段上的Qualifier注解
                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                if (qualifier != null) {
                    // 如果存在Qualifier注解，使用指定的Bean名称创建引用
                    beanReference = new BeanReference(qualifier.value());
                } else {
                    // 如果不存在Qualifier注解，使用字段类型创建引用
                    beanReference = new BeanReference(field.getType());
                }
                // 将属性名和Bean引用添加到属性值列表中
                propertyValues.add(new PropertyValue(field.getName(), beanReference));
            }
        }
    }
}

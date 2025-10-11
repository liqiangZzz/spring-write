package com.study.spring.context;


import com.study.spring.aop.beans.registry.BeanDefinitionRegistry;

/**
 * @ClassName AnnotationApplicationContext
 * @Description 注解驱动的ApplicationContext实现类
 * @Author liqiang
 * @Date 2025/10/10 14:48
 */
public class AnnotationApplicationContext extends AbstractApplicationContext {

    public AnnotationApplicationContext(String... basePackages) throws Throwable {
        super();
        // 找到所有的被 @Componment 修饰的Java类的BeanDefinition
        new ClassPathBeanDefinitionScanner((BeanDefinitionRegistry) this.beanFactory).scan(basePackages);
        super.refresh();
    }



}

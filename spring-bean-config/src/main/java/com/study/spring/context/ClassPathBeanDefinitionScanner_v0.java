package com.study.spring.context;

import com.study.spring.aop.beans.registry.BeanDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName ClassPathBeanDefinitionScanner_v0
 * @Description 扫描类路径下的Bean定义
 * @Author liqiang
 * @Date 2025/10/10 14:48
 */
@Slf4j
public class ClassPathBeanDefinitionScanner_v0 {


    private BeanDefinitionRegistry registry;

    public ClassPathBeanDefinitionScanner_v0(BeanDefinitionRegistry registry) {
        super();
        this.registry = registry;
    }


    public void scan(String... basePackages) {
        if (basePackages != null && basePackages.length > 0) {
            for (String basePackage : basePackages) {
                /*
				 1 递归扫描包目录下的.class文件
				 2 组合包路径+class文件名 得到全限定类名
				 3 ClassLoad.load("类名") 得到 Class 对象
				 4 解析Class上的注解，获得Bean定义信息，注册Bean定义
				 */
            }
        }
    }
}

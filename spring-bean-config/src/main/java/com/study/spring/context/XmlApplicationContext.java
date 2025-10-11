package com.study.spring.context;

import org.springframework.beans.factory.support.BeanDefinitionReader;

/**
 * @ClassName XmlApplicationContext
 * @Description
 * @Author liqiang
 * @Date 2025/10/10 15:39
 */
public class XmlApplicationContext  extends AbstractApplicationContext{

    private BeanDefinitionReader reader;

    public XmlApplicationContext(String... location) throws Throwable {
        super();
        // 加载解析注配置 生成BeanDefinition 并注册BeanFactory
        super.refresh();
    }
}

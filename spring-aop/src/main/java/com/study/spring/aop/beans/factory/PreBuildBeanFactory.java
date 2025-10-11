package com.study.spring.aop.beans.factory;

import com.study.spring.aop.beans.BeanDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @ClassName PreBuildBeanFactory
 * @Description
 * @Author liqiang
 * @Date 2025/9/25 10:19
 */
@Slf4j
public class PreBuildBeanFactory extends DefaultBeanFactory {

    public void preInstantiateSingletons() throws Throwable {
        synchronized (this.beanDefinitionMap) {
            for (Map.Entry<String, BeanDefinition> entry : this.beanDefinitionMap.entrySet()) {
                String key = entry.getKey();
                BeanDefinition value = entry.getValue();
                if (value.isSingleton()) {
                    this.getBean(key);
                }
            }
        }
    }
}

package com.spring.ioc.factory;

import com.spring.beans.BeanDefinition;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @ClassName PreBuildBeanFactory
 * @Description
 * @Author liqiang
 * @Date 2025/9/25 10:19
 */
@Slf4j
public class PreBuildBeanFactory extends DefaultBeanFactory4 {

    /**
     * 预实例化所有单例Bean
     * 该方法会遍历所有的BeanDefinition，对于标记为单例的Bean进行预先实例化
     *
     * @throws Exception 如果在Bean实例化过程中发生错误
     */
    public void preInstantiateSingletons() throws Exception {
        // 同步访问beanDefinitionMap以保证线程安全
        synchronized (this.beanDefinitionMap) {
            // 遍历所有BeanDefinition，实例化标记为单例的Bean
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

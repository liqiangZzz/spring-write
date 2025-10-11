package com.spring.ioc;

import com.spring.ioc.factory.PreBuildBeanFactory;
import com.study.spring.beans.GenericBeanDefinition;

import java.util.Arrays;

/**
 * @ClassName IOCContainerExample
 * @Description
 * @Author liqiang
 * @Date 2025/9/25 13:36
 */
public class IOCContainerExample {

    public static void main(String[] args) {

        PreBuildBeanFactory beanFactory = new PreBuildBeanFactory();

        try {
            GenericBeanDefinition userServiceDef = new GenericBeanDefinition();
            userServiceDef.setBeanClass(UserService.class);
            userServiceDef.setScope("singleton");
            userServiceDef.setInitMethodName("init");
            userServiceDef.setDestroyMethodName("destroy");
            beanFactory.registerBeanDefinition("userService", userServiceDef);
            // 注册别名
            beanFactory.registerAlias("userService", "userServiceImpl");
            beanFactory.registerAlias("userService", "myUserService");


            GenericBeanDefinition orderServiceDef = new GenericBeanDefinition();
            orderServiceDef.setBeanClass(OrderService.class);
            orderServiceDef.setScope("prototype");
            beanFactory.registerBeanDefinition("orderService", orderServiceDef);

            GenericBeanDefinition configDef = new GenericBeanDefinition();
            configDef.setBeanClass(AppConfig.class);
            configDef.setScope("singleton");
            configDef.setFactoryMethodName("createConfig");
            beanFactory.registerBeanDefinition("appConfig", configDef);

            // 预实例化单例 Bean

            beanFactory.preInstantiateSingletons();
            // 测试单例 Bean
            UserService userService1 = (UserService) beanFactory.getBean("userService");
            UserService userService2 = (UserService) beanFactory.getBean("userServiceImpl");
            System.out.println("UserService   Singleton beans are same: " + (userService1 == userService2));


            OrderService orderService = (OrderService) beanFactory.getBean("orderService");
            OrderService orderService2 = (OrderService) beanFactory.getBean("orderService");
            System.out.println("OrderService prototype beans are same: " + (orderService == orderService2));

            // 测试工厂方法创建的 Bean
            AppConfig config = (AppConfig) beanFactory.getBean("appConfig");
            config.showConfig();


            // 测试别名查询
            String[] aliases = beanFactory.getAliases("userService");
            System.out.println("Aliases for userService: " + Arrays.toString(aliases));

            System.out.println("All beans created successfully!");
            beanFactory.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


// ========== 测试用的 Bean 类 ==========

class UserService {
    public void init() {
        System.out.println("UserService initialized");
    }

    public void destroy() {
        System.out.println("UserService destroyed");
    }

    public void getUser() {
        System.out.println("Getting user...");
    }
}

class OrderService {
    public void getOrder() {
        System.out.println("Getting order...");
    }
}

class AppConfig {
    public static AppConfig createConfig() {
        System.out.println("Creating AppConfig via factory method");
        return new AppConfig();
    }

    public void showConfig() {
        System.out.println("AppConfig loaded successfully");
    }
}
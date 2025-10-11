package com.study.spring.exception;

/**
 * @ClassName BeanDefinitionRegistryException
 * @Description
 * @Author liqiang
 * @Date 2025/9/18 16:12
 */
public class BeanDefinitionRegistryException extends RuntimeException {

    private static final long serialVersionUID = 6056374114834139330L;

    public BeanDefinitionRegistryException(String mess) {
        super(mess);
    }

    public BeanDefinitionRegistryException(String mess, Throwable e) {
        super(mess, e);
    }
}

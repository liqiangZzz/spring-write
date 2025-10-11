package com.study.spring.context;

import com.study.spring.context.AnnotationApplicationContext;

/**
 * @ClassName AnnotationApplicationContextTest
 * @Description
 * @Author liqiang
 * @Date 2025/10/10 15:40
 */
public class AnnotationApplicationContextTest {

    public static void main(String[] args) {
        String[] basePackages = {"com.study.spring.bean"};
        try {
            AnnotationApplicationContext applicationContext = new AnnotationApplicationContext(basePackages);
            // 根据修复后的generateBeanName方法的逻辑，ABean类的默认bean名称应该是"aBean"
            System.out.println(applicationContext.getBean("aBean"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
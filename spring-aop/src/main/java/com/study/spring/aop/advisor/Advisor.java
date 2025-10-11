package com.study.spring.aop.advisor;

/**
 * @InterfaceName Advisor
 * @Description 通知者
 * @Author liqiang
 * @Date 2025-09-28 15:54
 */
public interface Advisor {

    String getAdviceBeanName();

    String getExpression();
}

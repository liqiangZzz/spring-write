package com.study.spring.aop.advisor;

import com.study.spring.aop.pointcut.AspectJExpressionPointcut;
import com.study.spring.aop.pointcut.Pointcut;

/**
 * @ClassName AspectJPointcutAdvisor
 * @Description
 * @Author liqiang
 * @Date 2025/9/28 15:57
 */
public class AspectJPointcutAdvisor implements PointcutAdvisor {
    private String adviceBeanName;

    private String expression;

    private AspectJExpressionPointcut pointcut;

    /**
     * 构造函数，初始化AspectJ切点顾问
     * @param adviceBeanName 通知bean的名称
     * @param expression AspectJ表达式字符串
     */
    public AspectJPointcutAdvisor(String adviceBeanName, String expression) {
        super();
        this.adviceBeanName = adviceBeanName;
        this.expression = expression;
        this.pointcut = new AspectJExpressionPointcut(this.expression);
    }

    /**
     * 返回配置的AspectJ表达式切点对象
     */
    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }


    /**
     * 获取通知bean名称
     */
    @Override
    public String getAdviceBeanName() {
        return this.adviceBeanName;
    }


    /**
     * 获取AspectJ表达式
     */
    @Override
    public String getExpression() {
        return this.expression;
    }
}

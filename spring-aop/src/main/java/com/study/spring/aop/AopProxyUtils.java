package com.study.spring.aop;

import com.study.spring.aop.advisor.Advisor;
import com.study.spring.aop.advisor.PointcutAdvisor;
import com.study.spring.aop.beans.factory.BeanFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName AopProxyUtils
 * @Description
 * @Author liqiang
 * @Date 2025/9/29 10:45
 */
public class AopProxyUtils {

    public static Object applyAdvices(Object target, Method method, Object[] args, List<Advisor> matchAdvisors,
                                      Object proxy, BeanFactory beanFactory) throws Throwable {

        // 1、获取要对当前方法进行增强的advice
        List<Object> advices = AopProxyUtils.getShouldApplyAdvices(target.getClass(), method, matchAdvisors, beanFactory);
        //2、如果有增强的advice，责任链增强执行
        if (CollectionUtils.isEmpty(advices)) {
            return method.invoke(target, args);
        } else {
            // 责任链式执行增强
            AopAdviceChainInvocation chain = new AopAdviceChainInvocation(proxy, target, method, args, advices);
            return chain.invoke();
        }


    }


    /**
     * 获取应该应用到指定方法上的通知列表
     *
     * @param beanClass     目标bean的类对象
     * @param method        目标方法
     * @param matchAdvisors 匹配的顾问列表
     * @param beanFactory   bean工厂，用于获取通知bean实例
     * @return 应该应用到该方法上的通知列表，如果没有匹配的通知则返回null
     * @throws Throwable 获取bean过程中可能抛出的异常
     */
    private static List<Object> getShouldApplyAdvices(Class<?> beanClass, Method method, List<Advisor> matchAdvisors, BeanFactory beanFactory) throws Throwable {
        // 如果没有匹配的顾问，则直接返回null
        if (CollectionUtils.isEmpty(matchAdvisors)) {
            return null;
        }
        List<Object> advices = new ArrayList<>();
        // 遍历所有匹配的顾问，筛选出适用于当前方法的通知
        for (Advisor advisor : matchAdvisors) {
            if (advisor instanceof PointcutAdvisor) {
                PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
                // 判断切点是否匹配当前方法
                if (pointcutAdvisor.getPointcut().matchMethod(method, beanClass)) {
                    advices.add(beanFactory.getBean(pointcutAdvisor.getAdviceBeanName()));
                }
            }
        }
        return advices;
    }
}

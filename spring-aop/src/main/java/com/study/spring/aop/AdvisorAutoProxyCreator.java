package com.study.spring.aop;

import com.study.spring.aop.advice.Advice;
import com.study.spring.aop.advisor.Advisor;
import com.study.spring.aop.advisor.PointcutAdvisor;
import com.study.spring.aop.beans.BeanPostProcessor;
import com.study.spring.aop.beans.aware.BeanFactoryAware;
import com.study.spring.aop.beans.factory.BeanFactory;
import com.study.spring.aop.factory.AopProxyFactory;
import com.study.spring.aop.pointcut.Pointcut;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * @ClassName AdvisorAutoProxyCreator
 * @Description
 * @Author liqiang
 * @Date 2025/9/28 16:35
 */
public class AdvisorAutoProxyCreator implements BeanPostProcessor, BeanFactoryAware {


    private BeanFactory beanFactory;
    //所有的Advisors
    private List<Advisor> advisors;
    //标识是否获取过了所有的Advisors
    private volatile boolean gettedAllAdvisors = false;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Throwable {

        //不能对Advisor 和 Advice 类型的bean作处理
        if(bean instanceof  Advisor || bean instanceof Advice){
            return bean;
        }
		/*逻辑
		1 判断Bean是否需要增强
		2 创建代理来实现增强
		*/

        //1 判断Bean是否需要增强
        List<Advisor> matchAdvisors = getMatchedAdvisors(bean);

        // 2如有切面切中，创建代理来实现增强
        if (CollectionUtils.isNotEmpty(matchAdvisors)) {
            bean = this.createProxy(bean, beanName, matchAdvisors);
        }

        return bean;
    }

    /**
     * 获取与指定Bean匹配的Advisor切面列表
     *
     * @param bean 需要匹配的Bean对象
     * @return 与该Bean匹配的Advisor切面列表
     * @throws Throwable 如果在获取过程中发生异常
     */
    private List<Advisor> getMatchedAdvisors(Object bean) throws Throwable {
        //第一次执行该方法，先从BeanFactory中得到用户配置的所有切面Advisor
        if (!gettedAllAdvisors) {
            synchronized (this) {
                if (!gettedAllAdvisors) {
                    advisors = this.beanFactory.getBeansOfTypeList(Advisor.class);
                    gettedAllAdvisors = true;
                }
            }
        }

        //如果没有配置切面
        if (CollectionUtils.isEmpty(this.advisors)) {
            return Collections.emptyList();
        }

        //有配置切面
        // 得到Bean的类、所有的方法
        Class<?> beanClass = bean.getClass();
        List<Method> allMethods = this.getAllMethodForClass(beanClass);

        // 存放匹配的Advisor的list
        List<Advisor> matchAdvisors = new ArrayList<>();
        // 遍历Advisor来找匹配的
        for (Advisor ad : this.advisors) {
            if (ad instanceof PointcutAdvisor) {
                if (isPointcutMatchBean((PointcutAdvisor) ad, beanClass, allMethods)) {
                    matchAdvisors.add(ad);
                }
            }
        }
        return matchAdvisors;
    }



    /**
     * 获取指定类及其所有接口中声明的所有方法
     *
     * @param beanClass 需要获取方法的类
     * @return 包含该类及其实现接口的所有方法的列表
     */
    private List<Method> getAllMethodForClass(Class<?> beanClass) {
        //注意需要获得本类以及所实现的接口的方法
        List<Method> allMethods = new LinkedList<>();
        // 获取该类实现的所有接口
        Set<Class<?>> classes = new LinkedHashSet<>(ClassUtils.getAllInterfacesForClassAsSet(beanClass));
        // 将当前类本身也加入到需要处理的类集合中
        classes.add(beanClass);
        // 遍历所有相关的类（包括当前类和接口），收集它们声明的所有方法
        for (Class<?> clazz : classes) {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
            allMethods.addAll(Arrays.asList(methods));
        }

        return allMethods;
    }

    /**
     * 判断切点是否匹配指定的Bean类和方法
     *
     * @param pa PointcutAdvisor对象，包含切点信息
     * @param beanClass Bean的类对象
     * @param methods Bean类中的方法列表
     * @return 如果切点匹配该Bean类或其任意方法则返回true，否则返回false
     */
    private boolean isPointcutMatchBean(PointcutAdvisor pa, Class<?> beanClass, List<Method> methods) {
        Pointcut p = pa.getPointcut();

        // 首先判断类是否匹配
        if (!p.matchClass(beanClass)) {
            return false;
        }

        // 再判断是否有方法匹配
        for (Method method : methods) {
            if (p.matchMethod(method, beanClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 创建代理对象
     *
     * @param bean 原始bean对象
     * @param beanName bean的名称
     * @param matchAdvisors 匹配的切面顾问列表
     * @return 代理对象
     * @throws Throwable 创建代理过程中可能抛出的异常
     */
    private Object createProxy(Object bean, String beanName, List<Advisor> matchAdvisors) throws Throwable {
        // 通过AopProxyFactory工厂去完成选择、和创建代理对象的工作。
        return AopProxyFactory.getDefaultAopProxyFactory().createAopProxy(bean, beanName, matchAdvisors, beanFactory)
                .getProxy();
    }

    @Override
    public void setBeanFactory(BeanFactory bf) {
        this.beanFactory = bf;
    }
}

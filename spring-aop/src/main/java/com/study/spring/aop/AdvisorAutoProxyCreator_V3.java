package com.study.spring.aop;

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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @ClassName AdvisorAutoProxyCreator_V2
 * @Description 增加代理实现逻辑
 * @Author liqiang
 * @Date 2025/9/28 16:41
 */
public class AdvisorAutoProxyCreator_V3 implements BeanPostProcessor, BeanFactoryAware {


    private BeanFactory beanFactory;

    /**
     * 所有的Advisor
     */

    private List<Advisor> advisors;

    /**
     * 标识是否获取过了所有的Advisors
     */
    private volatile boolean getAllAdvisors = false;

    @Override
    public void setBeanFactory(BeanFactory bf) {
        this.beanFactory = bf;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Throwable {
        /*
         * 逻辑：在初始化之后，
         * 1. 判断bean是否需要增强
         * 2. 创建代理对象实现增强
         */

        // 获取所有的Advisor,判断Bean是否需要增强
        List<Advisor> matchAdvisors = getMatchedAdvisors(bean);

        //
        if (CollectionUtils.isNotEmpty(matchAdvisors)) {
            bean = this.createProxy(bean, beanName, matchAdvisors);
        }
        return bean;
    }


    /**
     * 获取与指定Bean匹配的Advisor列表
     *
     * @param bean 需要匹配的Bean实例
     * @return 与该Bean匹配的Advisor列表，如果没有匹配的则返回null
     * @throws Throwable 在获取Bean过程中可能抛出的异常
     */

    private List<Advisor> getMatchedAdvisors(Object bean) throws Throwable {

        // 延迟初始化所有Advisor
        if (!getAllAdvisors) {
            // 获取所有的Advisor
            synchronized (this) {
                if (!getAllAdvisors) {
                    advisors = beanFactory.getBeansOfTypeList(Advisor.class);
                    getAllAdvisors = true;
                }
            }
        }

        //如果没有配置切面
        if (CollectionUtils.isEmpty(advisors)) {
            return null;
        }

        //有配置切面，得到Bean的类、所有的方法
        Class<?> beanClass = bean.getClass();
        List<Method> allMethods = this.getAllMethodForClass(beanClass);

        // 存放匹配Advisor的list
        List<Advisor> matchAdvisors = new LinkedList<>();
        for (Advisor advisor : this.advisors) {
            if (advisor instanceof PointcutAdvisor) {
                if (isPointcutMatchBean((PointcutAdvisor) advisor, beanClass, allMethods)) {
                    matchAdvisors.add(advisor);
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
        // 获取该类实现的所有接口集合，并添加当前类本身
        Set<Class<?>> classes = new LinkedHashSet<>(ClassUtils.getAllInterfacesForClassAsSet(beanClass));
        classes.add(beanClass);
        // 遍历所有类（包括当前类和接口），收集所有声明的方法
        for (Class<?> clazz : classes) {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
            for (Method m : methods) {
                allMethods.add(m);
            }
        }

        return allMethods;
    }


    /**
     * 判断切点是否匹配指定的Bean类和方法
     *
     * @param advisor    切点通知器，包含切点信息
     * @param beanClass  Bean的类对象
     * @param allMethods Bean类中的所有方法列表
     * @return 如果切点匹配该Bean的类或任意方法则返回true，否则返回false
     */
    private boolean isPointcutMatchBean(PointcutAdvisor advisor, Class<?> beanClass, List<Method> allMethods) {
        Pointcut p = advisor.getPointcut();

        // 首先判断类是否匹配
        if (!p.matchClass(beanClass)) {
            return false;
        }

        // 再判断是否有方法匹配
        for (Method method : allMethods) {
            if (p.matchMethod(method, beanClass)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 创建代理对象
     */
    private Object createProxy(Object bean, String beanName, List<Advisor> matchAdvisors) throws Throwable {
// 通过AopProxyFactory工厂去完成选择、和创建代理对象的工作。
        return AopProxyFactory.getDefaultAopProxyFactory().createAopProxy(bean, beanName, matchAdvisors, beanFactory).getProxy();
    }
}

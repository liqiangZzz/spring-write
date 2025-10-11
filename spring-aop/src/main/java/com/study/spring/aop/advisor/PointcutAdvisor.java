package com.study.spring.aop.advisor;

import com.study.spring.aop.pointcut.Pointcut;

/**
 * @InterfaceName PointcutAdvisor
 * @Description 用于表示基于切点的通知器。该接口定义了获取切点对象的方法，切点用于确定通知应该应用到哪些连接点
 * @Author liqiang
 * @Date 2025-09-28 15:54
 */
public interface PointcutAdvisor extends Advisor {

    /**
     * 获取当前通知器关联的切点对象。
     * 切点用于定义通知应该在哪些连接点上生效，包含了匹配规则和过滤条件。
     *
     * @return Pointcut 切点对象，用于确定通知的应用范围
     */
    Pointcut getPointcut();


}

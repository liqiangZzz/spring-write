package com.study.spring.aop.beans.aware;

import com.study.spring.aop.beans.factory.BeanFactory;

/**
 * @ClassName BeanFactoryAware
 * @Description
 * @Author liqiang
 * @Date 2025/9/28 16:18
 */
public interface BeanFactoryAware extends Aware {

	void setBeanFactory(BeanFactory bf);
}

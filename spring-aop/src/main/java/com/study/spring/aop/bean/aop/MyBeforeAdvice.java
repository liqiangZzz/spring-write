package com.study.spring.aop.bean.aop;

import com.study.spring.aop.advice.MethodBeforeAdvice;

import java.lang.reflect.Method;

public class MyBeforeAdvice implements MethodBeforeAdvice {

	@Override
	public void before(Method method, Object[] args, Object target) throws Throwable {
		System.out.println(this + " 对 " + target + " 进行了前置增强！");
	}

}

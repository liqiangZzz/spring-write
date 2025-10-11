package com.study.spring.aop.bean;

public interface Driver {

	void start();

	default void stop() {

	}
}

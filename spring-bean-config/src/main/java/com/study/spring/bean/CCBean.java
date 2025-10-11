package com.study.spring.bean;

import com.study.spring.aop.bean.Driver;

public class CCBean extends CBean implements Driver {

	public CCBean(String name) {
		super(name);
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

}

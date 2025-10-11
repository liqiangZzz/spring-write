package com.study.spring.aop.bean;

import com.study.spring.aop.beans.factory.DefaultBeanFactory;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.ShadowMatch;
import org.aspectj.weaver.tools.TypePatternMatcher;

import java.lang.reflect.Method;

public class AspectJTest {
	public static void main(String[] args) throws Exception {
		//创建支持所有基本类型并使用上下文类加载器解析的PointcutParser
		PointcutParser pp = PointcutParser
				.getPointcutParserSupportingAllPrimitivesAndUsingContextClassloaderForResolution();
		//解析类型模式"com.study.spring.."和切点表达式"execution( com.study.spring.samples.Driver.start*(..))"
		TypePatternMatcher tpm = pp.parseTypePattern("com.study.spring..*");
		PointcutExpression pe = pp.parsePointcutExpression("execution(* com.study.spring.samples.Driver.start*(..))");

		//检查CCBean类的getName方法是否匹配切点表达式
		Class<?> cl = CCBean.class;
		Method aMethod = cl.getMethod("getName", null);
		ShadowMatch sm = pe.matchesMethodExecution(aMethod);
		System.out.println(sm.alwaysMatches());


		//判断切点表达式是否可能匹配CCBean和DefaultBeanFactory类中的连接点
		System.out.println(pe.couldMatchJoinPointsInType(cl));
		System.out.println(pe.couldMatchJoinPointsInType(DefaultBeanFactory.class));


		//打印CCBean类的所有方法名
		for (Method m : cl.getMethods()) {
			System.out.println(m.getName());
		}
	}
}

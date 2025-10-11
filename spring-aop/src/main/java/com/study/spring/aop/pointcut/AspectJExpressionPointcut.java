package com.study.spring.aop.pointcut;

import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.ShadowMatch;

import java.lang.reflect.Method;

/**
 * @ClassName AspectJExpressionPointcut
 * @Description 切点表达式
 * @Author liqiang
 * @Date 2025/9/28 15:41
 */
public class AspectJExpressionPointcut implements Pointcut {

    private static PointcutParser pointcutParser = PointcutParser.getPointcutParserSupportingAllPrimitivesAndUsingContextClassloaderForResolution();

    private String expression;

    private PointcutExpression pointcutExpression;


    public AspectJExpressionPointcut(String expression) {
        super();
        this.expression = expression;
        pointcutExpression = pointcutParser.parsePointcutExpression(expression);
    }

    @Override
    public boolean matchClass(Class<?> targetClass) {
        return pointcutExpression.couldMatchJoinPointsInType(targetClass);
    }

    @Override
    public boolean matchMethod(Method method, Class<?> targetClass) {
        ShadowMatch shadowMatch = pointcutExpression.matchesMethodExecution(method);
        return shadowMatch.alwaysMatches();
    }

    public String getExpression() {
        return expression;
    }
}

package v;

import com.study.spring.aop.bean.ABean;
import com.study.spring.aop.bean.ABeanFactory;
import com.study.spring.aop.bean.CBean;
import com.study.spring.aop.bean.CCBean;
import com.study.spring.aop.beans.BeanDefinition;
import com.study.spring.aop.beans.BeanReference;
import com.study.spring.aop.beans.GenericBeanDefinition;
import com.study.spring.aop.beans.factory.DefaultBeanFactory;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DefaultBeanFactoryTest {

    static DefaultBeanFactory bf = new DefaultBeanFactory();

    @Test
    public void testRegister() {

        GenericBeanDefinition bd = new GenericBeanDefinition();

        bd.setBeanClass(ABean.class);
        bd.setScope(BeanDefinition.SCOPE_SINGLETON);
        bd.setInitMethodName("init");
        bd.setDestroyMethodName("destroy");
        List<Object> args = new ArrayList<>();
        // ABean 构造方法参数 第一个参数的值
        args.add("ABean");

        // ABean 构造方法参数 第二个参数的值
        GenericBeanDefinition Cbd = new GenericBeanDefinition();
        Cbd.setBeanClass(CCBean.class);
        List<Object> args2 = new ArrayList<>();
        args2.add("CCBean");
        Cbd.setScope(BeanDefinition.SCOPE_SINGLETON);
        Cbd.setConstructorArgumentValues(args2);
        bf.registerBeanDefinition("ccbean", Cbd);

        args.add(new BeanReference("ccbean"));
        bd.setConstructorArgumentValues(args);
        System.out.println();
        bf.registerBeanDefinition("aBean", bd);

    }

    @Test
    public void testRegisterStaticFactoryMethod() {
        GenericBeanDefinition cbd = new GenericBeanDefinition();
        cbd.setBeanClass(CBean.class);
        List<Object> args2 = new ArrayList<>();
        args2.add("cBean");
        cbd.setScope(BeanDefinition.SCOPE_SINGLETON);
        cbd.setConstructorArgumentValues(args2);
        bf.registerBeanDefinition("cbean", cbd);


        // 配置通过静态工厂方法创建的Bean
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(ABeanFactory.class);
        bd.setFactoryMethodName("getABean");


        // 为静态工厂方法准备正确的参数
        List<Object> args = new ArrayList<>();
        args.add("abean");
        args.add(new BeanReference("cbean"));
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("staticAbean", bd);

        bd.setFactoryBeanName("staticAbean");

    }

    @Test
    public void testRegisterFactoryMethod() {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(ABeanFactory.class);
        bf.registerBeanDefinition("factory", bd);

        bd = new GenericBeanDefinition();
        bd.setFactoryBeanName("factory");
        bd.setFactoryMethodName("getABean2");
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);


        List<Object> args = new ArrayList<>();
        // ABean 构造方法参数 第一个参数的值
        args.add("ABean");

        // ABean 构造方法参数 第二个参数的值
        GenericBeanDefinition cbd = new GenericBeanDefinition();
        cbd.setBeanClass(CBean.class);
        List<Object> args2 = new ArrayList<>();
        args2.add("cBean");
        cbd.setScope(BeanDefinition.SCOPE_SINGLETON);
        cbd.setConstructorArgumentValues(args2);
        bf.registerBeanDefinition("cbean", cbd);
        args.add(new BeanReference("cbean"));

        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("factoryAbean", bd);
    }

    @AfterClass
    public static void testGetBean() throws Throwable {

//        System.out.println("构造方法方式------------");
//        for (int i = 0; i < 3; i++) {
//            ABean ab = (ABean) bf.getBean("aBean");
//            ab.doSomthing();
//        }

//        System.out.println("静态工厂方法方式------------");
//        for (int i = 0; i < 3; i++) {
//            ABean ab = (ABean) bf.getBean("staticAbean");
//            ab.doSomthing();
//        }

		System.out.println("工厂方法方式------------");
		for (int i = 0; i < 3; i++) {
			ABean ab = (ABean) bf.getBean("factoryAbean");
			ab.doSomthing();
		}

		bf.close();
    }
}

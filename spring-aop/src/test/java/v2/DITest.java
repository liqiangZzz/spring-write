package v2;

import com.study.spring.aop.bean.ABean;
import com.study.spring.aop.bean.ABeanFactory;
import com.study.spring.aop.bean.CBean;
import com.study.spring.aop.bean.CCBean;
import com.study.spring.aop.beans.BeanReference;
import com.study.spring.aop.beans.GenericBeanDefinition;
import com.study.spring.aop.beans.factory.PreBuildBeanFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DITest {
    static PreBuildBeanFactory bf = new PreBuildBeanFactory();

    @Test
    public void testConstructorDI() throws Throwable {

        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(ABean.class);
        List<Object> args = new ArrayList<>();
        args.add("abean");
        args.add(new BeanReference("cbean"));
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("abean", bd);

        bd = new GenericBeanDefinition();
        bd.setBeanClass(CBean.class);
        args = new ArrayList<>();
        args.add("cbean");
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("cbean", bd);

        bf.preInstantiateSingletons();

        ABean abean = (ABean) bf.getBean("abean");
        abean.doSomthing();

        CBean cBean = (CBean) bf.getBean("cbean");
        System.out.println(cBean);
    }

    @Test
    public void testStaticFactoryMethodDI() throws Throwable {
        // 先注册CBean
        GenericBeanDefinition cbd = new GenericBeanDefinition();
        cbd.setBeanClass(CBean.class);
        List<Object> args2 = new ArrayList<>();
        args2.add("cbean");
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
        bf.registerBeanDefinition("abean", bd);

        bd.setFactoryBeanName("abean02");
        bf.preInstantiateSingletons();

        ABean abean = (ABean)bf.getBean("abean");
        abean.doSomthing();
    }

    @Test
    public void testFactoryMethodDI() throws Throwable {

        GenericBeanDefinition bd = new GenericBeanDefinition();
        List<Object> args = new ArrayList<>();
        args.add("cbean");
        bd.setBeanClass(CBean.class);
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("cbean", bd);

        bd = new GenericBeanDefinition();
        bd.setBeanClass(ABeanFactory.class);
        bf.registerBeanDefinition("abeanFactory", bd);

        bd = new GenericBeanDefinition();
        bd.setFactoryBeanName("abeanFactory");
        bd.setFactoryMethodName("getABean2");
        args = new ArrayList<>();
        args.add("abean");
        args.add(new BeanReference("cbean"));
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("abean", bd);


        bf.preInstantiateSingletons();

        ABean abean = (ABean) bf.getBean("abean");

        abean.doSomthing();
    }

    @Test
    public void testChildTypeDI() throws Throwable {

        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(ABean.class);
        List<Object> args = new ArrayList<>();
        args.add("abean");
        args.add(new BeanReference("ccbean"));
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("abean", bd);

        bd = new GenericBeanDefinition();
        bd.setBeanClass(CCBean.class);
        args = new ArrayList<>();
        args.add("Ccbean");
        bd.setConstructorArgumentValues(args);
        bf.registerBeanDefinition("ccbean", bd);

        bf.preInstantiateSingletons();

        ABean abean = (ABean) bf.getBean("abean");

        abean.doSomthing();
    }
}

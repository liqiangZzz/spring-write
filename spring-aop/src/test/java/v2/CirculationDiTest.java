package v2;

import com.study.spring.aop.bean.DBean;
import com.study.spring.aop.bean.EBean;
import com.study.spring.aop.beans.BeanReference;
import com.study.spring.aop.beans.GenericBeanDefinition;
import com.study.spring.aop.beans.factory.PreBuildBeanFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
/**
 * @ClassName CirculationDiTest
 * @Description 循环依赖测试
 * @Author liqiang
 * @Date 2025/9/28 16:18
 */
public class CirculationDiTest {

	static PreBuildBeanFactory bf = new PreBuildBeanFactory();

	@Test
	public void testCirculationDI() throws Throwable {
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(DBean.class);
		List<Object> args = new ArrayList<>();
		args.add("张三");

		args.add(new BeanReference("ebean"));
		bd.setConstructorArgumentValues(args);
		bf.registerBeanDefinition("dbean", bd);

		bd = new GenericBeanDefinition();
		bd.setBeanClass(EBean.class);
		args = new ArrayList<>();
		args.add(new BeanReference("dbean"));
		bd.setConstructorArgumentValues(args);
		bf.registerBeanDefinition("ebean", bd);

		bf.preInstantiateSingletons();
	}
}

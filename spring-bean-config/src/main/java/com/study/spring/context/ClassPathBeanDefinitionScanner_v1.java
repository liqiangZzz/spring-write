package com.study.spring.context;

import com.study.spring.aop.beans.registry.BeanDefinitionRegistry;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @ClassName ClassPathBeanDefinitionScanner_v1
 * @Description  扫描类路径下的Bean定义
 * @Author liqiang
 * @Date 2025/10/10 14:52
 */
public class ClassPathBeanDefinitionScanner_v1 {


    private BeanDefinitionRegistry registry;

    public ClassPathBeanDefinitionScanner_v1(BeanDefinitionRegistry registry) {
        super();
        this.registry = registry;
    }


    public void scan(String... basePackages) {
        if (basePackages != null && basePackages.length > 0) {
            for (String basePackage : basePackages) {
                /*
				 1 递归扫描包目录下的.class文件
				 2 组合包路径+class文件名 得到全限定类名
				 3 ClassLoad.load("类名") 得到 Class 对象
				 4 解析Class上的注解，获得Bean定义信息，注册Bean定义
				 */

                //1 递归扫描包目录下的.class文件
                Set<File> classFiles =  this.doScan(basePackage);
                //2 得到Class对象，并解析注解、注册Bean定义
                this.readAndRegisterBeanDefinition(classFiles);
            }
        }
    }



    private Set<File> doScan(String basePackage) {
        // 扫描包下的类
        // 将包名转为路径名
        String basePackagePath = "/" + StringUtils.replace(basePackage, ".", "/");
        //得到包对应的目录
        File rootDir = new File(this.getClass().getResource(basePackagePath).getPath());

        //存放找到的类文件集合
        Set<File> classFiles = new HashSet<>();
        // 调用doRetrieveMatchingFiles来扫描class文件
        this.doRetrieveMatchingFiles(rootDir, classFiles);

        return classFiles;
    }

    private void doRetrieveMatchingFiles(File rootDir, Set<File> classFiles) {
        for (File file : rootDir.listFiles()) {
            if(file.isDirectory() && file.canRead()){
                this.doRetrieveMatchingFiles(file, classFiles);
            }
            if (file.getName().endsWith(".class")){
                classFiles.add(file);
            }
        }
    }



    private void readAndRegisterBeanDefinition(Set<File> classFiles) {
    }
}

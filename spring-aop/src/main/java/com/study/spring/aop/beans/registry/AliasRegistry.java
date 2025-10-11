package com.study.spring.aop.beans.registry;

/**
 * @InterfaceName AliasRegistry
 * @Description 别名注册接口 - 管理 Bean 的别名
 * @Author liqiang
 * @Date 2025-09-25 10:37
 */
public interface AliasRegistry {

    /** 注册别名 */
    default void registerAlias(String name, String alias){

    }

    /** 移除别名 */
    default void removeAlias(String alias){

    }

    /** 检查名称是否为别名 */
    default boolean isAlias(String name){
        return false;
    }

    /** 根据别名获取原始名称 */
    default String getOriginalName(String name){
        return name;
    }


    /** 获取指定 Bean 的所有别名 */
    default String[] getAliases(String name){
        return new String[0];
    }

}

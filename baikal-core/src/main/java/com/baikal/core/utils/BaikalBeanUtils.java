package com.baikal.core.utils;

/**
 * @author kowalski
 * BaikalBean工具类
 * 组装叶子节点在初始化时使用的Bean
 */
public final class BaikalBeanUtils {

  private BaikalBeanUtils() {
  }

  private static BaikalBeanFactory factory;

  public static void autowireBean(Object existingBean) {
    factory.autowireBean(existingBean);
  }

  public static boolean containsBean(String name) {
    if (factory == null) {
      return false;
    }
    return factory.containsBean(name);
  }

  public static void setFactory(BaikalBeanFactory factory) {
    BaikalBeanUtils.factory = factory;
  }

  public interface BaikalBeanFactory {
    /**
     * 注入Bean
     *
     * @param existingBean
     */
    void autowireBean(Object existingBean);

    /**
     * 检查是否有此Bean
     *
     * @param name
     * @return
     */
    boolean containsBean(String name);
  }
}

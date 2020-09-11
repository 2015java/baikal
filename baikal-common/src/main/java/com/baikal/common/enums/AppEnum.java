package com.baikal.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kowalski
 * 业务App
 */
public enum AppEnum {
  /**
   * 测试项目
   */
  TEST(1, "Test");

  private final int app;

  private final String name;

  AppEnum(int app, String name) {
    this.app = app;
    this.name = name;
  }

  public int getApp() {
    return app;
  }

  public String getName() {
    return name;
  }

  private static final Map<Integer, AppEnum> MAP = new HashMap<>();

  static {
    for (AppEnum enums : AppEnum.values()) {
      MAP.put(enums.getApp(), enums);
    }
  }

  public static Map<Integer, AppEnum> getMAP() {
    return MAP;
  }

  public static AppEnum getEnum(int app) {
    return MAP.get(app);
  }
}

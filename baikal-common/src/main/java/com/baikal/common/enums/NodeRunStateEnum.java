package com.baikal.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kowalski
 * 节点执行结果
 * REJECT OUTTIME NONE 都不干预执行转向
 */
public enum NodeRunStateEnum {

  /**
   * false
   */
  FALSE((byte) 0),
  /**
   * true
   */
  TRUE((byte) 1),
  /**
   * 不参与节点运行结果
   */
  NONE((byte) 2);

  private final byte state;

  NodeRunStateEnum(byte state) {
    this.state = state;
  }

  public byte getState() {
    return state;
  }

  private static final Map<Byte, NodeRunStateEnum> MAP = new HashMap<>();

  static {
    for (NodeRunStateEnum enums : NodeRunStateEnum.values()) {
      MAP.put(enums.getState(), enums);
    }
  }

  public static NodeRunStateEnum getEnum(byte state) {
    return MAP.get(state);
  }
}

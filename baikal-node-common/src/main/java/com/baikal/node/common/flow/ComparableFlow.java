package com.baikal.node.common.flow;

import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.roam.BaseLeafRoamFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kowalski
 * 比大小比相等
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ComparableFlow extends BaseLeafRoamFlow {

  private Object key;

  private Object score;
  /**
   * 1判大(默认)
   * 0判等
   * -1判小
   */
  private int code = 1;

  /**
   * 叶子节点流程处理
   *
   * @param roam 包裹
   * @return 返回
   */
  @Override
  protected boolean doRoamFlow(BaikalRoam roam) {
    Comparable<Object> keyValue = roam.getUnion(key);
    if (keyValue == null && code != 0) {
      return false;
    }
    Comparable<Object> value = roam.getUnion(score);
    if (value == null) {
      return code == 0 && keyValue == null;
    }
    if (keyValue == null) {
      return false;
    }
    return keyValue.compareTo(value) == code;
  }
}

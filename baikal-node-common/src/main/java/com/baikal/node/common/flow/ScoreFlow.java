package com.baikal.node.common.flow;

import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.roam.BaseLeafRoamFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author kowalski
 * 比较BigDecimal数据大小
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScoreFlow extends BaseLeafRoamFlow {

  private Object key;

  private Object score;

  /**
   * 叶子节点流程处理
   *
   * @param roam
   * @return
   */
  @Override
  protected boolean doRoamFlow(BaikalRoam roam) {
    BigDecimal keyValue = roam.getUnion(key);
    if(keyValue == null){
      return false;
    }
    BigDecimal value = roam.getUnion(score);
    if(value == null){
      return false;
    }
    return keyValue.compareTo(value) > 0;
  }
}

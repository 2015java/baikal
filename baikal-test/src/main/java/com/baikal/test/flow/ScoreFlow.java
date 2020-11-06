package com.baikal.test.flow;

import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.roam.BaseLeafRoamFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kowalski
 * 取出roam中的值比较大小
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScoreFlow extends BaseLeafRoamFlow {

  private double score;

  private String key;

  /**
   * 叶子节点流程处理
   *
   * @param roam
   * @return
   */
  @Override
  protected boolean doRoamFlow(BaikalRoam roam) {
    Object value = roam.getMulti(key);
    if(value == null){
      return false;
    }
    double valueScore = Double.parseDouble(value.toString());
    return !(valueScore < score);
  }
}

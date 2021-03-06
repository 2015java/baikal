package com.baikal.node.common.flow;

import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.roam.BaseLeafRoamFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;

/**
 * @author kowalski
 * 判断key对应的值是否在set中
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContainsFlow extends BaseLeafRoamFlow {

  private Object key;
  /***
   * Collection
   * eg:Set[1,2]
   */
  private Object collection;

  /**
   * 叶子节点流程处理
   *
   * @param roam
   * @return
   */
  @Override
  protected boolean doRoamFlow(BaikalRoam roam) {
    Collection<Object> sets = roam.getUnion(collection);
    if (sets == null || sets.isEmpty()) {
      return false;
    }
    return sets.contains(roam.getUnion(key));
  }
}

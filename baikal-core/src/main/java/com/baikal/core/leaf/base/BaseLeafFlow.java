package com.baikal.core.leaf.base;

import com.baikal.common.enums.NodeRunStateEnum;
import com.baikal.core.base.BaseLeaf;
import com.baikal.core.context.BaikalContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kowalski
 * Flow 叶子节点
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafFlow extends BaseLeaf {

  /**
   * process leaf flow
   */
  @Override
  protected NodeRunStateEnum doLeaf(BaikalContext cxt) {
    if (doFlow(cxt)) {
      return NodeRunStateEnum.TRUE;
    }
    return NodeRunStateEnum.FALSE;
  }

  /**
   * process leaf flow
   *
   * @param cxt
   * @return
   */
  protected abstract boolean doFlow(BaikalContext cxt);
}

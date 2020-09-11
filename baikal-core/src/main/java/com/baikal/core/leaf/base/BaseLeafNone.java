package com.baikal.core.leaf.base;

import com.baikal.common.enums.NodeRunStateEnum;
import com.baikal.core.base.BaseLeaf;
import com.baikal.core.context.BaikalContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kowalski
 * None 叶子节点
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafNone extends BaseLeaf {

  /**
   * process leaf none
   */
  @Override
  protected NodeRunStateEnum doLeaf(BaikalContext cxt) {
    doNone(cxt);
    return NodeRunStateEnum.NONE;
  }

  /**
   * process leaf none
   *
   * @param cxt
   * @return
   */
  protected abstract void doNone(BaikalContext cxt);
}

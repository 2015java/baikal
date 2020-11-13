package com.baikal.core.leaf.base;

import com.baikal.common.enums.NodeRunStateEnum;
import com.baikal.core.base.BaseLeaf;
import com.baikal.core.context.BaikalContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 * Result 叶子节点
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafResult extends BaseLeaf {

  /**
   * process leaf result
   */
  @Override
  protected NodeRunStateEnum doLeaf(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    if (this.doResult(cxt)) {
      return NodeRunStateEnum.TRUE;
    }
    return NodeRunStateEnum.FALSE;
  }

  /**
   * process leaf result
   *
   * @param cxt
   * @return
   */
  protected abstract boolean doResult(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException;
}

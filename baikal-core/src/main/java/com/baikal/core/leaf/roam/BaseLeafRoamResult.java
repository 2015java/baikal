package com.baikal.core.leaf.roam;

import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.base.BaseLeafResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafRoamResult extends BaseLeafResult {

  @Override
  protected boolean doResult(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    return doRoamResult(cxt.getPack().getRoam());
  }

  /**
   * process leaf result with roam
   *
   * @param roam
   * @return
   */
  protected abstract boolean doRoamResult(BaikalRoam roam) throws InvocationTargetException, IllegalAccessException;
}

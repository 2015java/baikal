package com.baikal.core.leaf.roam;

import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.base.BaseLeafFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafRoamFlow extends BaseLeafFlow {

  @Override
  protected boolean doFlow(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    return doRoamFlow(cxt.getPack().getRoam());
  }

  /**
   * process leaf flow with roam
   *
   * @param roam
   * @return
   */
  protected abstract boolean doRoamFlow(BaikalRoam roam) throws InvocationTargetException, IllegalAccessException;
}

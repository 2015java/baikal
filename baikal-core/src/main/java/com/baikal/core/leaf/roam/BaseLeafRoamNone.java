package com.baikal.core.leaf.roam;

import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.base.BaseLeafNone;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafRoamNone extends BaseLeafNone {

  @Override
  protected void doNone(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    doRoamNone(cxt.getPack().getRoam());
  }

  /**
   * process leaf none with roam
   *
   * @param roam
   * @return
   */
  protected abstract void doRoamNone(BaikalRoam roam) throws InvocationTargetException, IllegalAccessException;
}

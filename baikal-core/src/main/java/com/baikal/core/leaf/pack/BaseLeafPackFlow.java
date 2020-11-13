package com.baikal.core.leaf.pack;

import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalPack;
import com.baikal.core.leaf.base.BaseLeafFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafPackFlow extends BaseLeafFlow {

  @Override
  protected boolean doFlow(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    return doPackFlow(cxt.getPack());
  }

  /**
   * process leaf flow with pack
   *
   * @param pack
   * @return
   */
  protected abstract boolean doPackFlow(BaikalPack pack) throws InvocationTargetException, IllegalAccessException;
}

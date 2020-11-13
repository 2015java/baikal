package com.baikal.core.leaf.pack;

import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalPack;
import com.baikal.core.leaf.base.BaseLeafResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafPackResult extends BaseLeafResult {

  @Override
  protected boolean doResult(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    return doPackResult(cxt.getPack());
  }

  /**
   * process leaf result with pack
   *
   * @param pack
   * @return
   */
  protected abstract boolean doPackResult(BaikalPack pack) throws InvocationTargetException, IllegalAccessException;
}

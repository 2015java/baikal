package com.baikal.core.leaf.pack;

import com.baikal.core.leaf.base.BaseLeafResult;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalPack;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafPackResult extends BaseLeafResult {

  @Override
  protected boolean doResult(BaikalContext cxt) {
    return doPackResult(cxt.getPack());
  }

  /**
   * process leaf result with pack
   *
   * @param pack
   * @return
   */
  protected abstract boolean doPackResult(BaikalPack pack);
}

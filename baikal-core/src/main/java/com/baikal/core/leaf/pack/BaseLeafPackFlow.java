package com.baikal.core.leaf.pack;

import com.baikal.core.leaf.base.BaseLeafFlow;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalPack;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafPackFlow extends BaseLeafFlow {

  @Override
  protected boolean doFlow(BaikalContext cxt) {
    return doPackFlow(cxt.getPack());
  }

  /**
   * process leaf flow with pack
   *
   * @param pack
   * @return
   */
  protected abstract boolean doPackFlow(BaikalPack pack);
}

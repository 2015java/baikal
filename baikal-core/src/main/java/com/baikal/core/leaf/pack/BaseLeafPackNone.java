package com.baikal.core.leaf.pack;

import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalPack;
import com.baikal.core.leaf.base.BaseLeafNone;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeafPackNone extends BaseLeafNone {

  @Override
  protected void doNone(BaikalContext cxt) {
    doPackNone(cxt.getPack());
  }

  /**
   * process leaf none with pack
   *
   * @param pack
   * @return
   */
  protected abstract void doPackNone(BaikalPack pack);
}

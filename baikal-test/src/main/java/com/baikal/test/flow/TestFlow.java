package com.baikal.test.flow;

import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.roam.BaseLeafRoamFlow;

/**
 * @author kowalski
 */
public class TestFlow extends BaseLeafRoamFlow {

  /**
   * process leaf flow with roam
   *
   * @param roam
   * @return
   */
  @Override
  protected boolean doRoamFlow(BaikalRoam roam) {
    return false;
  }
}

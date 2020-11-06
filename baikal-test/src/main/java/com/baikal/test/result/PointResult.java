package com.baikal.test.result;

import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.roam.BaseLeafRoamResult;
import com.baikal.test.service.SendService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.annotation.Resource;

@Data
@EqualsAndHashCode(callSuper = true)
public class PointResult extends BaseLeafRoamResult {

  @Resource
  private SendService sendService;

  private String key;

  private double value;

  @Override
  protected boolean doRoamResult(BaikalRoam roam) {
    Integer uid = roam.getMulti(key);
    if (uid == null || value <= 0) {
      return false;
    }

    return sendService.sendPoint(uid, value);
  }
}

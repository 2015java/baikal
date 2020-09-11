package com.baikal.test.flow;

import com.alibaba.fastjson.JSONArray;
import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.roam.BaseLeafRoamFlow;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SetFlow extends BaseLeafRoamFlow {

  private Object set;

  private Object key;

  /**
   * 叶子节点流程处理
   *
   * @param roam
   * @return
   */
  @Override
  protected boolean doRoamFlow(BaikalRoam roam) {
    if(set == null){
      return false;
    }
    Set<Object> sets = roam.getUnion(set);
    if(CollectionUtils.isEmpty(sets)){
      return false;
    }
    return sets.contains(roam.getUnion(key));
  }

  public void setSet(Object set) {
    if(set instanceof JSONArray){
      this.set = new HashSet<>((Collection<?>) set);
    }else {
      this.set = set;
    }
  }
}

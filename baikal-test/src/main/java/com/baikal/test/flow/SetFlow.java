package com.baikal.test.flow;

import com.baikal.core.annotation.BaikalParam;
import com.baikal.test.service.SendService;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Set;

/**
 * @author kowalski
 * 取出roam中的值比较大小
 */
@Data
public final class SetFlow {

  private Set<Integer> set;

  @Resource
  private SendService sendService;

  private static boolean compare(@BaikalParam("one") Integer one, @BaikalParam("two") Integer two) {
    return one > two;
  }

  /**
   * 叶子节点流程处理
   *
   * @return
   */
  public boolean contains(@BaikalParam("uid") Integer uid) {
    if (CollectionUtils.isEmpty(set)) {
      return false;
    }
    sendService.sendPoint(uid, 10);
    return set.contains(uid);
  }
}

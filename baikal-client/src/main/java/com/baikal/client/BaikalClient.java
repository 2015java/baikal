package com.baikal.client;

import com.baikal.core.BaikalDispatcher;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalPack;
import com.baikal.core.context.BaikalRoam;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kowalski
 */
public final class BaikalClient {

  private BaikalClient() {
  }

  /**
   * 不关心结果-handler异步执行
   *
   * @param pack
   */
  public static void process(BaikalPack pack) {
    BaikalDispatcher.asyncDispatcher(pack);
  }

  /**
   * 需要执行后的单个Roam
   *
   * @param pack
   * @return
   */
  public static BaikalRoam processSingleRoam(BaikalPack pack) {
    BaikalContext cxt = processSingleCxt(pack);
    if (cxt != null && cxt.getPack() != null) {
      return cxt.getPack().getRoam();
    }
    return null;
  }

  /**
   * 需要执行后的Roam列表
   *
   * @param pack
   * @return
   */
  public static List<BaikalRoam> processRoam(BaikalPack pack) {
    List<BaikalContext> cxts = BaikalDispatcher.syncDispatcher(pack);
    if (CollectionUtils.isEmpty(cxts)) {
      return null;
    }
    return cxts.stream().map(cxt -> cxt.getPack().getRoam())
        .collect(Collectors.toCollection(() -> new ArrayList<>(cxts.size())));
  }

  /**
   * 需要执行后的单个cxt
   *
   * @param pack
   * @return
   */
  public static BaikalContext processSingleCxt(BaikalPack pack) {
    List<BaikalContext> cxts = processCxt(pack);
    if (CollectionUtils.isEmpty(cxts)) {
      return null;
    }
    return cxts.get(0);
  }

  /**
   * 需要执行后的cxt列表
   *
   * @param pack
   * @return
   */
  public static List<BaikalContext> processCxt(BaikalPack pack) {
    return BaikalDispatcher.syncDispatcher(pack);
  }
}

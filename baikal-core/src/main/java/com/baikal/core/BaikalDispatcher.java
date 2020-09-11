package com.baikal.core;

import com.alibaba.fastjson.JSON;
import com.baikal.core.cache.BaikalHandlerCache;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalPack;
import com.baikal.core.context.BaikalRoam;
import com.baikal.core.handler.BaikalHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author kowalski
 * Baikal分发器
 */
@Slf4j
public final class BaikalDispatcher {

  private BaikalDispatcher() {
  }

  public static List<BaikalContext> syncDispatcher(BaikalPack pack) {
    if (!checkPack(pack)) {
      return Collections.emptyList();
    }
    /**优先ID*/
    if (pack.getBaikalId() > 0) {
      BaikalHandler handler = BaikalHandlerCache.getHandlerById(pack.getBaikalId());
      if (handler == null) {
        log.debug("handler maybe expired baikalId:{}", pack.getBaikalId());
        return Collections.emptyList();
      }
      BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack);
      handler.handle(cxt);
      return Collections.singletonList(cxt);
    }
    /**其次是按scene区分的一组*/
    Map<Long, BaikalHandler> handlerMap = BaikalHandlerCache.getHandlersByScene(pack.getScene());
    if (handlerMap == null || handlerMap.isEmpty()) {
      log.debug("handlers maybe all expired scene:{}", pack.getScene());
      return Collections.emptyList();
    }

    List<BaikalContext> cxtList = new LinkedList<>();
    if(handlerMap.size() == 1){
      /**处理的handler只有一个 直接处理*/
      BaikalHandler handler= handlerMap.values().iterator().next();
      BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack);
      handler.handle(cxt);
      cxtList.add(cxt);
      return cxtList;
    }
    /**处理的handler有多个 保障roam不冲突(注意浅拷贝影响)*/
    BaikalRoam roam = pack.getRoam();
    for (BaikalHandler handler : handlerMap.values()) {
      BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack.newPack(roam));
      handler.handle(cxt);
      cxtList.add(cxt);
    }
    return cxtList;
  }

  public static void asyncDispatcher(BaikalPack pack) {
    if (!checkPack(pack)) {
      return ;
    }
    /**优先ID*/
    if (pack.getBaikalId() > 0) {
      BaikalHandler handler = BaikalHandlerCache.getHandlerById(pack.getBaikalId());
      if (handler == null) {
        log.debug("handler maybe expired baikalId:{}", pack.getBaikalId());
        return ;
      }
      BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack);
      handler.handle(cxt);
      return ;
    }
    /**其次是按scene区分的一组*/
    Map<Long, BaikalHandler> handlerMap = BaikalHandlerCache.getHandlersByScene(pack.getScene());
    if (handlerMap == null || handlerMap.isEmpty()) {
      log.debug("handlers maybe all expired scene:{}", pack.getScene());
      return ;
    }

    if(handlerMap.size() == 1){
      /**处理的handler只有一个 直接处理*/
      BaikalHandler handler = handlerMap.values().iterator().next();
      BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack);
      handler.handle(cxt);
      return ;
    }
    /**处理的handler有多个 保障roam不冲突(注意浅拷贝影响)*/
    BaikalRoam roam = pack.getRoam();
    for (BaikalHandler handler : handlerMap.values()) {
      BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack.newPack(roam));
      handler.handle(cxt);
    }
  }

  private static boolean checkPack(BaikalPack pack) {
    if (pack == null) {
      log.error("invalid pack null");
      return false;
    }
    if (pack.getBaikalId() > 0) {
      return true;
    }
    if (pack.getScene() == null || pack.getScene().isEmpty()) {
      log.error("invalid pack none baikalId none scene:{}", JSON.toJSONString(pack));
      return false;
    }
    return true;
  }
}

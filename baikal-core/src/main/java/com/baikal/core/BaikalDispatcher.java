package com.baikal.core;

import com.alibaba.fastjson.JSON;
import com.baikal.common.enums.DebugEnum;
import com.baikal.common.exception.NodeException;
import com.baikal.core.base.BaseNode;
import com.baikal.core.cache.BaikalConfCache;
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
    /*优先ID*/
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
    /*其次是按scene区分的一组*/
    if(pack.getScene() != null && !pack.getScene().isEmpty()) {
      Map<Long, BaikalHandler> handlerMap = BaikalHandlerCache.getHandlersByScene(pack.getScene());
      if (handlerMap == null || handlerMap.isEmpty()) {
        log.debug("handlers maybe all expired scene:{}", pack.getScene());
        return Collections.emptyList();
      }

      List<BaikalContext> cxtList = new LinkedList<>();
      if (handlerMap.size() == 1) {
        /*处理的handler只有一个 直接处理*/
        BaikalHandler handler = handlerMap.values().iterator().next();
        BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack);
        handler.handle(cxt);
        cxtList.add(cxt);
        return cxtList;
      }
      /*处理的handler有多个 保障roam不冲突(注意浅拷贝影响)*/
      BaikalRoam roam = pack.getRoam();
      for (BaikalHandler handler : handlerMap.values()) {
        BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack.newPack(roam));
        handler.handle(cxt);
        cxtList.add(cxt);
      }
      return cxtList;
    }

    /*最后是按照confId的root*/
    long confId = pack.getConfId();
    BaikalContext cxt = new BaikalContext(confId, pack);
    if (DebugEnum.filter(DebugEnum.IN_PACK, pack.getDebug())) {
      log.info("handle confId:{} in pack:{}", pack.getConfId(), JSON.toJSONString(pack));
    }
    BaseNode root = BaikalConfCache.getConfById(confId);
    if (root != null) {
      try {
        root.process(cxt);
        if (DebugEnum.filter(DebugEnum.PROCESS, pack.getDebug())) {
          log.info("handle confId:{} process:{}", confId, cxt.getProcessInfo().toString());
        }
        if (DebugEnum.filter(DebugEnum.OUT_PACK, pack.getDebug())) {
          log.info("handle confId:{} out pack:{}", confId, JSON.toJSONString(pack));
        } else {
          if (DebugEnum.filter(DebugEnum.OUT_ROAM, pack.getDebug())) {
            log.info("handle confId:{} out roam:{}", confId, JSON.toJSONString(pack.getRoam()));
          }
        }
      } catch (NodeException ne) {
        log.error("error occur in node confId:{} node:{} cxt:{}", confId, cxt.getCurrentId(), JSON.toJSONString(cxt), ne);
      } catch (Exception e) {
        log.error("error occur confId:{} node:{} cxt:{}", confId, cxt.getCurrentId(), JSON.toJSONString(cxt), e);
      }
    } else {
      log.error("root not exist please check! confId:{}", confId);
    }
    return Collections.singletonList(cxt);
  }

  public static void asyncDispatcher(BaikalPack pack) {
    if (!checkPack(pack)) {
      return ;
    }
    /*优先ID*/
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
    /*其次是按scene区分的一组*/
    if(pack.getScene() != null && !pack.getScene().isEmpty()) {
      Map<Long, BaikalHandler> handlerMap = BaikalHandlerCache.getHandlersByScene(pack.getScene());
      if (handlerMap == null || handlerMap.isEmpty()) {
        log.debug("handlers maybe all expired scene:{}", pack.getScene());
        return;
      }

      if (handlerMap.size() == 1) {
        /*处理的handler只有一个 直接处理*/
        BaikalHandler handler = handlerMap.values().iterator().next();
        BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack);
        handler.handle(cxt);
        return;
      }
      /*处理的handler有多个 保障roam不冲突(注意浅拷贝影响)*/
      BaikalRoam roam = pack.getRoam();
      for (BaikalHandler handler : handlerMap.values()) {
        BaikalContext cxt = new BaikalContext(handler.findBaikalId(), pack.newPack(roam));
        handler.handle(cxt);
      }
    }
    /*最后是按照confId的root*/
    long confId = pack.getConfId();
    BaikalContext cxt = new BaikalContext(confId, pack);
    if (DebugEnum.filter(DebugEnum.IN_PACK, pack.getDebug())) {
      log.info("handle confId:{} in pack:{}", pack.getConfId(), JSON.toJSONString(pack));
    }
    BaseNode root = BaikalConfCache.getConfById(confId);
    if (root != null) {
      try {
        root.process(cxt);
        if (DebugEnum.filter(DebugEnum.PROCESS, pack.getDebug())) {
          log.info("handle confId:{} process:{}", confId, cxt.getProcessInfo().toString());
        }
        if (DebugEnum.filter(DebugEnum.OUT_PACK, pack.getDebug())) {
          log.info("handle confId:{} out pack:{}", confId, JSON.toJSONString(pack));
        } else {
          if (DebugEnum.filter(DebugEnum.OUT_ROAM, pack.getDebug())) {
            log.info("handle confId:{} out roam:{}", confId, JSON.toJSONString(pack.getRoam()));
          }
        }
      } catch (NodeException ne) {
        log.error("error occur in node confId:{} node:{} cxt:{}", confId, cxt.getCurrentId(), JSON.toJSONString(cxt), ne);
      } catch (Exception e) {
        log.error("error occur confId:{} node:{} cxt:{}", confId, cxt.getCurrentId(), JSON.toJSONString(cxt), e);
      }
    } else {
      log.error("root not exist please check! confId:{}", confId);
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
    if (pack.getScene() != null && !pack.getScene().isEmpty()) {
      return true;
    }
    if(pack.getConfId() > 0){
      return true;
    }
    log.error("invalid pack none baikalId none scene none confId:{}", JSON.toJSONString(pack));
    return false;
  }
}

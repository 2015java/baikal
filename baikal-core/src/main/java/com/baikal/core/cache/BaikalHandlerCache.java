package com.baikal.core.cache;

import com.alibaba.fastjson.JSON;
import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.common.model.BaikalBaseDto;
import com.baikal.core.base.BaseNode;
import com.baikal.core.handler.BaikalHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kowalski
 * base配置缓存
 * base配置的实例化,层级构建,更新
 */
@Slf4j
public final class BaikalHandlerCache {

  /**
   * key baikalId value handler
   */
  private static final Map<Long, BaikalHandler> idHandlerMap = new ConcurrentHashMap<>();
  /**
   * key1 scene key2 baikalId
   */
  private static final Map<String, Map<Long, BaikalHandler>> sceneHandlersMap = new ConcurrentHashMap<>();
  /**
   * key1 confId key2 baikalId
   */
  private static final Map<Long, Map<Long, BaikalHandler>> confIdHandlersMap = new ConcurrentHashMap<>();

  private static final String REGEX_COMMA = ",";

  public static BaikalHandler getHandlerById(Long baikalId) {
    return idHandlerMap.get(baikalId);
  }

  public static Map<Long, BaikalHandler> getIdHandlerMap() {
    return idHandlerMap;
  }

  public static Map<Long, BaikalHandler> getHandlersByScene(String scene) {
    return sceneHandlersMap.get(scene);
  }

  public static List<String> insertOrUpdate(List<BaikalBaseDto> baikalBaseDtos) {
    List<String> errors = new ArrayList<>(baikalBaseDtos.size());
    for (BaikalBaseDto base : baikalBaseDtos) {
      BaikalHandler handler = new BaikalHandler();
      handler.setBaikalId(base.getId());
      handler.setTimeTypeEnum(TimeTypeEnum.getEnum(base.getTimeType()));
      handler.setStart(base.getStart() == null ? 0 : base.getStart());
      handler.setEnd(base.getEnd() == null ? 0 : base.getEnd());
      Long confId = base.getConfId();
      if (confId != null && confId > 0) {
        /*confId等于空的情况不考虑处理,没配confId的handler是没有意义的*/
        BaseNode root = BaikalConfCache.getConfById(confId);
        if (root == null) {
          String errorModeStr = JSON.toJSONString(base);
          errors.add("confId:" + confId + " not exist conf:" + errorModeStr);
          log.error("confId:{} not exist please check! conf:{}", confId, errorModeStr);
          continue;
        }
        Map<Long, BaikalHandler> handlerMap = confIdHandlersMap.get(confId);
        if (handlerMap == null) {
          handlerMap = new ConcurrentHashMap<>();
          confIdHandlersMap.put(confId, handlerMap);
        }
        handlerMap.put(handler.findBaikalId(), handler);
        handler.setRoot(root);
      }
      handler.setDebug(base.getDebug() == null ? 0 : base.getDebug());
      if (base.getScenes() != null && !base.getScenes().isEmpty()) {
        handler.setScenes(new HashSet<>(Arrays.asList(base.getScenes().split(REGEX_COMMA))));
      } else {
        handler.setScenes(Collections.emptySet());
      }
      onlineOrUpdateHandler(handler);
    }
    return errors;
  }

  public static void updateHandlerRoot(BaseNode updateConfNode) {
    Map<Long, BaikalHandler> handlerMap = confIdHandlersMap.get(updateConfNode.getBaikalNodeId());
    if (handlerMap != null) {
      for (BaikalHandler handler : handlerMap.values()) {
        handler.setRoot(updateConfNode);
      }
    }
  }

  public static void delete(List<Long> ids) {
    for (Long id : ids) {
      BaikalHandler removeHandler = idHandlerMap.get(id);
      if (removeHandler != null && removeHandler.getRoot() != null) {
        confIdHandlersMap.remove(removeHandler.getRoot().getBaikalNodeId());
      }
      offlineHandler(removeHandler);
    }
  }

  public static void onlineOrUpdateHandler(BaikalHandler handler) {
    BaikalHandler originHandler = null;
    if (handler.findBaikalId() > 0) {
      originHandler = idHandlerMap.get(handler.findBaikalId());
      idHandlerMap.put(handler.findBaikalId(), handler);
    }
    /*原有handler的新handler不存在的scene*/
    if (originHandler != null && originHandler.getScenes() != null && !originHandler.getScenes().isEmpty()) {
      if (handler.getScenes() == null || handler.getScenes().isEmpty()) {
        for (String scene : originHandler.getScenes()) {
          Map<Long, BaikalHandler> handlerMap = sceneHandlersMap.get(scene);
          if (handlerMap != null && !handlerMap.isEmpty()) {
            handlerMap.remove(originHandler.findBaikalId());
          }
          if (handlerMap == null || handlerMap.isEmpty()) {
            sceneHandlersMap.remove(scene);
          }
        }
        return;
      }
      for (String scene : originHandler.getScenes()) {
        if (!handler.getScenes().contains(scene)) {
          /*新的不存在以前的scene*/
          Map<Long, BaikalHandler> handlerMap = sceneHandlersMap.get(scene);
          if (handlerMap != null && !handlerMap.isEmpty()) {
            handlerMap.remove(originHandler.findBaikalId());
          }
          if (handlerMap == null || handlerMap.isEmpty()) {
            sceneHandlersMap.remove(scene);
          }
        }
      }
    }
    for (String scene : handler.getScenes()) {
      Map<Long, BaikalHandler> handlerMap = sceneHandlersMap.get(scene);
      if (handlerMap == null || handlerMap.isEmpty()) {
        handlerMap = new LinkedHashMap<>();
        sceneHandlersMap.put(scene, handlerMap);
      }
      handlerMap.put(handler.findBaikalId(), handler);
    }
  }

  private static void offlineHandler(BaikalHandler handler) {
    if (handler != null) {
      idHandlerMap.remove(handler.findBaikalId());
      for (String scene : handler.getScenes()) {
        Map<Long, BaikalHandler> handlerMap = sceneHandlersMap.get(scene);
        if (handlerMap != null && !handlerMap.isEmpty()) {
          handlerMap.remove(handler.findBaikalId());
        }
        if (handlerMap == null || handlerMap.isEmpty()) {
          sceneHandlersMap.remove(scene);
        }
      }
    }
  }

  private static void offlineHandler(Long id) {
    BaikalHandler handler = idHandlerMap.get(id);
    if (handler != null) {
      offlineHandler(handler);
    }
  }

  /**
   * 下线一个scene下的所有handler
   *
   * @param scene
   */
  private static void offlineHandler(String scene) {
    Map<Long, BaikalHandler> handlerMap = sceneHandlersMap.get(scene);
    if (handlerMap != null && !handlerMap.isEmpty()) {
      sceneHandlersMap.remove(scene);
    }
  }
}

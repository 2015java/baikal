package com.baikal.server.controller;

import com.alibaba.fastjson.JSON;
import com.baikal.common.constant.Constant;
import com.baikal.common.enums.AppEnum;
import com.baikal.dao.model.BaikalConf;
import com.baikal.server.model.WebResult;
import com.baikal.server.service.BaikalEditService;
import com.baikal.server.service.BaikalServerService;
import org.jetbrains.annotations.Contract;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author kowalski
 */

@RestController
public class BaikalController {

  private final BaikalServerService serverService;

  @Resource
  private BaikalEditService editService;

  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Resource
  private AmqpTemplate amqpTemplate;

  @Contract(pure = true)
  public BaikalController(BaikalServerService serverService) {
    this.serverService = serverService;
  }

  @RequestMapping(value = "/baikal/app/list", method = RequestMethod.GET)
  public WebResult getBaikalApp(@RequestParam(defaultValue = "1") Integer pageIndex,
                                @RequestParam(defaultValue = "100") Integer pageSize) {
    WebResult<List<BaikalApp>> result = new WebResult<>();
    Map<Integer, AppEnum> map = AppEnum.getMAP();
    List<BaikalApp> list = map.entrySet().stream()
        .map(entry -> new BaikalApp(entry.getKey(), entry.getValue().getName()))
        .collect(Collectors.toCollection(() -> new ArrayList<>(map.size())));
    result.setData(list);
    return result;
  }

  @RequestMapping(value = "/baikal/conf/list", method = RequestMethod.GET)
  public WebResult getBaikalConf(@RequestParam Integer app, @RequestParam(defaultValue = "1") Integer pageIndex,
                                 @RequestParam(defaultValue = "100") Integer pageSize) {
    return editService.getBase(app, pageIndex, pageSize);
  }

  @RequestMapping("/baikal/conf/detail")
  public WebResult getBaikalAppConf(@RequestParam Integer app, @RequestParam Long baikalId) {
    Object obj = amqpTemplate.convertSendAndReceive(Constant.getShowConfExchange(), String.valueOf(app),
        String.valueOf(baikalId));
    if (obj != null) {
      String json = (String) obj;
      if (!StringUtils.isEmpty(json)) {
        Map map = JSON.parseObject(json, Map.class);
        if (!CollectionUtils.isEmpty(map)) {
          Map handlerMap = (Map) map.get("handler");
          if (!CollectionUtils.isEmpty(handlerMap)) {
            Map rootMap = (Map) handlerMap.get("root");
            if (!CollectionUtils.isEmpty(rootMap)) {
              Set<Long> nodeIdSet = new HashSet<>();
              assemble(app, rootMap, nodeIdSet);
              return new WebResult<>(map);
            }
          }
        }
      }
    }
    return new WebResult<>();
  }

  @SuppressWarnings("unchecked")
  private List<Map> getChild(Map map) {
    return (List) map.get("children");
  }

  @SuppressWarnings("unchecked")
  private void assemble(Integer app, Map map, Set<Long> nodeIdSet) {
    if (map == null) {
      return;
    }
    Long nodeId = (Long) map.get("baikalNodeId");
    if (nodeId == null) {
      return;
    }
    Map foward = (Map) map.get("baikalForward");
    if (foward != null) {
      foward.put("nextId", nodeId);
    }
    assembleOther(app, map, nodeIdSet);
    assemble(app, foward, nodeIdSet);
    List<Map> children = getChild(map);
    if (CollectionUtils.isEmpty(children)) {
      return;
    }
    for (Map child : children) {
      child.put("parentId", nodeId);
      assemble(app, child, nodeIdSet);
    }
  }

  @SuppressWarnings("unchecked")
  private void assembleOther(Integer app, Map map, Set<Long> nodeIdSet) {
    Long nodeId = (Long) map.get("baikalNodeId");
    if (nodeId == null /*|| nodeIdSet.contains(baikalNodeId)*/) {
      return;
    }
    nodeIdSet.add(nodeId);
    Map showConf = new HashMap(map.size());
    Set<Map.Entry> entrySet = map.entrySet();
    List<Object> needRemoveKey = new ArrayList<>(map.size());
    for (Map.Entry entry : entrySet) {
      if (!("baikalForward".equals(entry.getKey()) || "children".equals(entry.getKey()) || "nextId".equals(entry.getKey()) || "parentId".equals(entry.getKey()))) {
        needRemoveKey.add(entry.getKey());
        if (!adjust(entry.getKey(), entry.getValue(), showConf)) {
          showConf.put(entry.getKey(), entry.getValue());
        }
      }
    }
    Object baikalForward = map.get("baikalForward");
    if (baikalForward != null) {
      map.remove("baikalForward");
      map.put("forward", baikalForward);
    }
    for (Object removeKey : needRemoveKey) {
      map.remove(removeKey);
    }
    map.put("showConf", showConf);
    BaikalConf baikalConf = serverService.getActiveConfById(app, nodeId);
    if (baikalConf != null) {
      if (!StringUtils.isEmpty(baikalConf.getName())) {
        showConf.put("nodeName", baikalConf.getName());
      }
      if (!StringUtils.isEmpty(baikalConf.getConfField())) {
        showConf.put("confField", baikalConf.getConfField());
      }
      if (!StringUtils.isEmpty(baikalConf.getConfName())) {
        showConf.put("confName", baikalConf.getId() + "-" + baikalConf.getConfName().substring(baikalConf.getConfName().lastIndexOf('.') + 1));
      }
      if (baikalConf.getType() != null) {
        showConf.put("nodeType", baikalConf.getType());
      }
      if (baikalConf.getStart() != null) {
        map.put("start", baikalConf.getStart());
      }
      if (baikalConf.getEnd() != null) {
        map.put("end", baikalConf.getEnd());
      }
      if (baikalConf.getTimeType() != null) {
        map.put("timeType", baikalConf.getTimeType());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private boolean adjust(Object key, Object value, Map showConf) {
    if ("baikalNodeId".equals(key)) {
      showConf.put("nodeId", value);
      return true;
    }
    if ("baikalNodeDebug".equals(key)) {
      showConf.put("debug", value);
      return true;
    }
    if ("baikalStart".equals(key)) {
      long time = Long.parseLong(value.toString());
      if (time != 0) {
        showConf.put("开始时间", sdf.format(new Date(time)));
      }
      return true;
    }
    if ("baikalEnd".equals(key)) {
      long time = Long.parseLong(value.toString());
      if (time != 0) {
        showConf.put("结束时间", sdf.format(new Date(time)));
      }
      return true;
    }
    if ("baikalInverse".equals(key)) {
      if (Boolean.parseBoolean(value.toString())) {
        showConf.put("inverse", value);
      }
      return true;
    }
    return false;
  }
}

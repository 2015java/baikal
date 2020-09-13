package com.baikal.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.baikal.common.constant.Constant;
import com.baikal.common.enums.NodeTypeEnum;
import com.baikal.common.enums.StatusEnum;
import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.common.model.BaikalBaseDto;
import com.baikal.common.model.BaikalConfDto;
import com.baikal.dao.mapper.BaikalBaseMapper;
import com.baikal.dao.mapper.BaikalConfMapper;
import com.baikal.dao.model.BaikalBase;
import com.baikal.dao.model.BaikalBaseExample;
import com.baikal.dao.model.BaikalConf;
import com.baikal.dao.model.BaikalConfExample;
import com.baikal.server.service.BaikalServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author kowalski
 * notice:
 * 1.数据库的更新为主更新
 * 2.baikal上线有最大提前一天上线(提前上线避免延迟)
 * 3.数据库手动下线与本地等待缓存到期激活冲突解决:本地缓存到期激活前检查是否被下线
 * 4.数据库手动更新延长上线时间与本地等待缓存到期激活冲突解决:增加与库中比对判断
 * 5.数据库手动更新已激活的与本地等待缓存到期激活冲突解决:以数据库更新为主
 * 6.client端启动init与更新消息推送内容冲突:加锁同步
 */
@Slf4j
@Service
public class BaikalServerServiceImpl implements BaikalServerService, InitializingBean {

  /**
   * 上次更新时间
   */
  private Date lastUpdateTime;

  private static final Set<Integer> appSet = new HashSet<>();
  /**
   * 提前一天上线(提前上线避免延迟与方便检查)
   */
  private static final long FOWARD_MILLS = 86400000L;

  private volatile long version;

  private static final Object LOCK = new Object();
  @Resource
  private BaikalBaseMapper baseMapper;

  @Resource
  private BaikalConfMapper confMapper;

  @Resource
  private AmqpTemplate amqpTemplate;
  /**
   * key:app value baseList
   */
  private final Map<Integer, Map<Long, BaikalBase>> baseWaitMap = new HashMap<>();
  /**
   * key:app value conf
   */
  private final Map<Integer, Map<Long, BaikalConf>> confWaitMap = new HashMap<>();

  /**
   * key:app value baseList
   */
  private final Map<Integer, Map<Long, BaikalBase>> baseActiveMap = new HashMap<>();
  /**
   * key:app value conf
   */
  private final Map<Integer, Map<Long, BaikalConf>> confActiveMap = new HashMap<>();

  private final Map<Integer, Map<Byte, Map<String, Integer>>> leafClassMap = new HashMap<>();

  @Override
  public Set<Integer> getAppSet() {
    return appSet;
  }

  @Override
  public List<BaikalBase> getBaseActive(Integer app) {
    Map<Long, BaikalBase> baseMap = baseActiveMap.get(app);
    if(CollectionUtils.isEmpty(baseMap)){
      return Collections.emptyList();
    }
    return new ArrayList<>(baseMap.values());
  }

  /**
   * 获取所有的activeBase-从库里
   *
   * @param app
   * @return
   */
  @Override
  public List<BaikalBase> getBaseFromDb(Integer app) {
    BaikalBaseExample example = new BaikalBaseExample();
    example.createCriteria().andAppEqualTo(app);
    baseMapper.selectByExample(example);
    //TODO
    return null;
  }

  /**
   * 根据confId获取配置信息
   * @param app
   * @param confId
   * @return
   */
  @Override
  public BaikalConf getActiveConfById(Integer app, Long confId) {
    Map<Long, BaikalConf> confMap = confActiveMap.get(app);
    if(!CollectionUtils.isEmpty(confMap)){
      return confMap.get(confId);
    }
    return null;
  }

  @Override
  public Map<String, Integer> getLeafClassMap(Integer app, Byte type) {
    Map<Byte, Map<String, Integer>> map = leafClassMap.get(app);
    if(map != null){
      return map.get(type);
    }
    return null;
  }

  @Override
  public void updateByEdit() {
    update();
  }

  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

  /**
   * 定时任务,距上次执行完成10s后执行
   */
  @Scheduled(fixedDelay = 20000)
  private void update() {
    Date now = new Date();
    log.info("=================now:{}   last:{}",sdf.format(now), sdf.format(lastUpdateTime));
    Map<Integer, Set<Long>> deleteConfMap = new HashMap<>(appSet.size());
    Map<Integer, Set<Long>> deleteBaseMap = new HashMap<>(appSet.size());

    Map<Integer, Map<Long, BaikalConf>> activeChangeConfMap = new HashMap<>(appSet.size());
    Map<Integer, Map<Long, BaikalBase>> activeChangeBaseMap = new HashMap<>(appSet.size());

    Map<Integer, Map<Long, BaikalConf>> waitConfMap = new HashMap<>(appSet.size());
    Map<Integer, Map<Long, BaikalBase>> waitBaseMap = new HashMap<>(appSet.size());
    Set<Long> dbWaitSet = new HashSet<>();
    /*先找数据库里的变化*/
    updateFromDbConf(deleteConfMap, activeChangeConfMap, waitConfMap, now, dbWaitSet);
    updateFromDbBase(deleteBaseMap, activeChangeBaseMap, waitBaseMap, now, dbWaitSet);
    /*原先active的check*/
    originActiveCheckConf(deleteConfMap, now);
    originActiveCheckBase(deleteBaseMap, now);
    /*原先的wait check 找active的*/
    originWaitCheckConf(activeChangeConfMap, now, deleteConfMap, dbWaitSet);
    originWaitCheckBase(activeChangeBaseMap, now, deleteBaseMap, dbWaitSet);
    /*更新本地缓存*/
    long updateVersion = updateLocal(deleteBaseMap, deleteConfMap, activeChangeBaseMap, waitBaseMap,
        activeChangeConfMap, waitConfMap);
    /*更新完毕 发送变更消息*/
    sendChange(deleteConfMap, deleteBaseMap, activeChangeConfMap, activeChangeBaseMap, updateVersion);
    /*更新时间*/
    lastUpdateTime = now;
  }

  private void updateFromDbConf(Map<Integer, Set<Long>> deleteConfMap,
      Map<Integer, Map<Long, BaikalConf>> activeChangeConfMap,
      Map<Integer, Map<Long, BaikalConf>> waitConfMap,
      Date now,
      Set<Long> dbWaitSet) {
    BaikalConfExample confExample = new BaikalConfExample();
    confExample.createCriteria().andUpdateAtGreaterThan(lastUpdateTime).andUpdateAtLessThanOrEqualTo(now);
    List<BaikalConf> confList = confMapper.selectByExample(confExample);
    log.info("===============confList:{}", JSON.toJSONString(confList));
    if (!CollectionUtils.isEmpty(confList)) {
      for (BaikalConf conf : confList) {
        appSet.add(conf.getApp());
        if (conf.getStatus() == StatusEnum.OFFLINE.getStatus()) {
          /*手动更新下线*/
          deleteConfMap.computeIfAbsent(conf.getApp(), k -> new HashSet<>()).add(conf.getId());
          continue;
        }
        if (passInvalidCheck(conf.getId(), conf.getTimeType(), conf.getStart(), conf.getEnd(), now)) {
          if (passActiveCheck(conf.getTimeType(), conf.getStart(), conf.getEnd(), now)) {
            activeChangeConfMap.computeIfAbsent(conf.getApp(), k -> new HashMap<>()).put(conf.getId(), conf);
          } else {
            dbWaitSet.add(conf.getId());
            waitConfMap.computeIfAbsent(conf.getApp(), k -> new HashMap<>()).put(conf.getId(), conf);
          }
        } else {
          /*手动更新时间下线*/
          deleteConfMap.computeIfAbsent(conf.getApp(), k -> new HashSet<>()).add(conf.getId());
        }
      }
    }
  }

  private void updateFromDbBase(Map<Integer, Set<Long>> deleteBaseMap,
      Map<Integer, Map<Long, BaikalBase>> activeChnageBaseMap,
      Map<Integer, Map<Long, BaikalBase>> waitBaseMap,
      Date now,
      Set<Long> dbWaitSet) {
    BaikalBaseExample baseExample = new BaikalBaseExample();
    baseExample.createCriteria().andUpdateAtGreaterThan(lastUpdateTime).andUpdateAtLessThanOrEqualTo(now);
    List<BaikalBase> baseList = baseMapper.selectByExample(baseExample);
    log.info("===============baseList:{}", JSON.toJSONString(baseList));
    if (!CollectionUtils.isEmpty(baseList)) {
      for (BaikalBase base : baseList) {
        appSet.add(base.getApp());
        if (base.getStatus() == StatusEnum.OFFLINE.getStatus()) {
          /*手动更新下线*/
          deleteBaseMap.computeIfAbsent(base.getApp(), k -> new HashSet<>()).add(base.getId());
          continue;
        }
        if (passInvalidCheck(base.getId(), base.getTimeType(), base.getStart(), base.getEnd(), now)) {
          if (passActiveCheck(base.getTimeType(), base.getStart(), base.getEnd(), now)) {
            activeChnageBaseMap.computeIfAbsent(base.getApp(), k -> new HashMap<>()).put(base.getId(), base);
          } else {
            dbWaitSet.add(base.getId());
            waitBaseMap.computeIfAbsent(base.getApp(), k -> new HashMap<>()).put(base.getId(), base);
          }
        } else {
          /*手动更新时间下线*/
          deleteBaseMap.computeIfAbsent(base.getApp(), k -> new HashSet<>()).add(base.getId());
        }
      }
    }
  }

  /**
   * 原先的wait check 找active的
   *
   * @param activeChangeConfMap
   * @param now
   */
  private void originWaitCheckConf(Map<Integer, Map<Long, BaikalConf>> activeChangeConfMap,
      Date now,
      Map<Integer, Set<Long>> confDeleteMap,
      Set<Long> dbWaitSet) {
    for (Map.Entry<Integer, Map<Long, BaikalConf>> entry : confWaitMap.entrySet()) {
      for (Map.Entry<Long, BaikalConf> confEntry : entry.getValue().entrySet()) {
        if (passActiveCheck(confEntry.getValue().getTimeType(), confEntry.getValue().getStart(),
            confEntry.getValue().getEnd(), now) && !deleteContainsCheck(confDeleteMap, entry.getKey(),
            confEntry.getKey()) && !dbWaitSet.contains(confEntry.getKey())) {
          /*wait的变active 如果已经有了 说明已经吸收了库里的变化 则不更改*/
          activeChangeConfMap.computeIfAbsent(confEntry.getValue().getApp(), k -> new HashMap<>())
              .putIfAbsent(confEntry.getValue().getId(), confEntry.getValue());
        }
      }
    }
  }

  /**
   * 原先的wait check 找active的
   *
   * @param activeChnageBaseMap
   * @param now
   */
  private void originWaitCheckBase(Map<Integer, Map<Long, BaikalBase>> activeChnageBaseMap,
      Date now,
      Map<Integer, Set<Long>> baseDeleteMap,
      Set<Long> dbWaitSet) {
    for (Map.Entry<Integer, Map<Long, BaikalBase>> entry : baseWaitMap.entrySet()) {
      for (Map.Entry<Long, BaikalBase> baseEntry : entry.getValue().entrySet()) {
        if (passActiveCheck(baseEntry.getValue().getTimeType(), baseEntry.getValue().getStart(),
            baseEntry.getValue().getEnd(), now) && !deleteContainsCheck(baseDeleteMap, entry.getKey(),
            baseEntry.getKey()) && !dbWaitSet.contains(baseEntry.getKey())) {
          /*wait的变active 如果已经有了 说明已经吸收了库里的变化 则不更改*/
          activeChnageBaseMap.computeIfAbsent(baseEntry.getValue().getApp(), k -> new HashMap<>())
              .putIfAbsent(baseEntry.getValue().getId(), baseEntry.getValue());
        }
      }
    }
  }

  private boolean deleteContainsCheck(Map<Integer, Set<Long>> deleteMap, Integer app, Long id) {
    Set<Long> deleteIdSet = deleteMap.get(app);
    if (deleteIdSet == null) {
      return false;
    }
    return deleteIdSet.contains(id);
  }

  /**
   * 原先active的check
   *
   * @param deleteConfMap
   * @param now
   */
  private void originActiveCheckConf(Map<Integer, Set<Long>> deleteConfMap, Date now) {
    for (Map.Entry<Integer, Map<Long, BaikalConf>> activeEnyry : confActiveMap.entrySet()) {
      for (Map.Entry<Long, BaikalConf> confEntry : activeEnyry.getValue().entrySet()) {
        if (!passActiveCheck(confEntry.getValue().getTimeType(), confEntry.getValue().getStart(),
            confEntry.getValue().getEnd(), now)) {
          /*原先的不再active*/
          deleteConfMap.computeIfAbsent(activeEnyry.getKey(), k -> new HashSet<>())
              .add(confEntry.getValue().getId());
        }
      }
    }
  }

  /**
   * 原先active的check
   *
   * @param deleteBaseMap
   * @param now
   */
  private void originActiveCheckBase(Map<Integer, Set<Long>> deleteBaseMap, Date now) {
    for (Map.Entry<Integer, Map<Long, BaikalBase>> activeEnyry : baseActiveMap.entrySet()) {
      for (Map.Entry<Long, BaikalBase> baseEntry : activeEnyry.getValue().entrySet()) {
        if (!passActiveCheck(baseEntry.getValue().getTimeType(), baseEntry.getValue().getStart(),
            baseEntry.getValue().getEnd(), now)) {
          /*原先的不再active*/
          deleteBaseMap.computeIfAbsent(activeEnyry.getKey(), k -> new HashSet<>()).add(baseEntry.getValue().getId());
        }
      }
    }
  }

  private void sendChange(Map<Integer, Set<Long>> deleteConfMap,
      Map<Integer, Set<Long>> deleteBaseMap,
      Map<Integer, Map<Long, BaikalConf>> activeChangeConfMap,
      Map<Integer, Map<Long, BaikalBase>> activeChnageBaseMap,
      long updateVersion) {
    for (Integer app : appSet) {
      Map<String, Object> updateMap = null;
      Map insertOrUpdateConfMap = activeChangeConfMap.get(app);
      if (!CollectionUtils.isEmpty(insertOrUpdateConfMap)) {
        updateMap = new HashMap<>(5);
        updateMap.put("insertOrUpdateConfs", insertOrUpdateConfMap.values());
      }
      Map insertOrUpdateBaseMap = activeChnageBaseMap.get(app);
      if (!CollectionUtils.isEmpty(insertOrUpdateBaseMap)) {
        if(updateMap == null){
          updateMap = new HashMap<>(4);
        }
        updateMap.put("insertOrUpdateBases", insertOrUpdateBaseMap.values());
      }
      Set deleteConfIds = deleteConfMap.get(app);
      if (!CollectionUtils.isEmpty(deleteConfMap)) {
        if(updateMap == null){
          updateMap = new HashMap<>(3);
        }
        updateMap.put("deleteConfIds", deleteConfIds);
      }
      Set deleteBases = deleteBaseMap.get(app);
      if (!CollectionUtils.isEmpty(deleteBases)) {
        if(updateMap == null){
          updateMap = new HashMap<>(2);
        }
        updateMap.put("deleteBaseIds", deleteBases);
      }
      /*有更新就推送消息*/
      if (updateMap != null) {
        updateMap.put("version", updateVersion);
        String message = JSON.toJSONString(updateMap);
        amqpTemplate.convertAndSend(Constant.getUpdateExchange(), Constant.getUpdateRoutetKey(app), message);
        log.info("baikal update app:{}, content:{}", app, message);
      }
    }
  }

  /**
   * 更新本地cache
   * 先处理删除,再处理插入与更新
   *
   * @param deleteBaseMap
   * @param deleteConfMap
   * @param activeChangeBaseMap
   * @param waitBaseMap
   * @param activeChangeConfMap
   * @param waitConfMap
   * @return 当前更新版本
   */
  private long updateLocal(Map<Integer, Set<Long>> deleteBaseMap,
      Map<Integer, Set<Long>> deleteConfMap,
      Map<Integer, Map<Long, BaikalBase>> activeChangeBaseMap,
      Map<Integer, Map<Long, BaikalBase>> waitBaseMap,
      Map<Integer, Map<Long, BaikalConf>> activeChangeConfMap,
      Map<Integer, Map<Long, BaikalConf>> waitConfMap) {
    synchronized (LOCK) {
      for (Map.Entry<Integer, Set<Long>> entry : deleteConfMap.entrySet()) {
        for (Long id : entry.getValue()) {
          Map tmpWaitMap = confWaitMap.get(entry.getKey());
          Map tmpActiveMap = confActiveMap.get(entry.getKey());
          if (tmpWaitMap != null) {
            confWaitMap.get(entry.getKey()).remove(id);
          }
          if (tmpActiveMap != null) {
            confActiveMap.get(entry.getKey()).remove(id);
          }
        }
      }
      for (Map.Entry<Integer, Set<Long>> entry : deleteBaseMap.entrySet()) {
        for (Long id : entry.getValue()) {
          Map tmpWaitMap = baseWaitMap.get(entry.getKey());
          Map tmpActiveMap = baseActiveMap.get(entry.getKey());
          if (tmpWaitMap != null) {
            baseWaitMap.get(entry.getKey()).remove(id);
          }
          if (tmpActiveMap != null) {
            baseActiveMap.get(entry.getKey()).remove(id);
          }
        }
      }
      for (Map.Entry<Integer, Map<Long, BaikalBase>> appEntry : waitBaseMap.entrySet()) {
        for (Map.Entry<Long, BaikalBase> entry : appEntry.getValue().entrySet()) {
          baseWaitMap.computeIfAbsent(appEntry.getKey(), k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
        }
      }
      for (Map.Entry<Integer, Map<Long, BaikalConf>> appEntry : waitConfMap.entrySet()) {
        for (Map.Entry<Long, BaikalConf> entry : appEntry.getValue().entrySet()) {
          confWaitMap.computeIfAbsent(appEntry.getKey(), k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
        }
      }
      for (Map.Entry<Integer, Map<Long, BaikalBase>> appEntry : activeChangeBaseMap.entrySet()) {
        for (Map.Entry<Long, BaikalBase> entry : appEntry.getValue().entrySet()) {
          baseActiveMap.computeIfAbsent(appEntry.getKey(), k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
          Map<Long, BaikalBase> map = baseWaitMap.get(appEntry.getKey());
          if (map != null) {
            map.remove(entry.getKey());
          }
        }
      }
      for (Map.Entry<Integer, Map<Long, BaikalConf>> appEntry : activeChangeConfMap.entrySet()) {
        for (Map.Entry<Long, BaikalConf> entry : appEntry.getValue().entrySet()) {
          confActiveMap.computeIfAbsent(appEntry.getKey(), k -> new HashMap<>()).put(entry.getKey(), entry.getValue());
          Map<Long, BaikalConf> map = confWaitMap.get(appEntry.getKey());
          if (map != null) {
            map.remove(entry.getKey());
          }
        }
      }
      version++;
      return version;
    }
  }

  @Override
  public void afterPropertiesSet() {
    Date now = new Date();
    /*baseList*/
    BaikalBaseExample baseExample = new BaikalBaseExample();
    baseExample.createCriteria().andStatusEqualTo(StatusEnum.ONLINE.getStatus()).andUpdateAtLessThanOrEqualTo(now);
    List<BaikalBase> baseList = baseMapper.selectByExample(baseExample);

    if (!CollectionUtils.isEmpty(baseList)) {
      for (BaikalBase base : baseList) {
        appSet.add(base.getApp());
        if (passInvalidCheck(base.getId(), base.getTimeType(), base.getStart(), base.getEnd(), now)) {
          if (passActiveCheck(base.getTimeType(), base.getStart(), base.getEnd(), now)) {
            baseActiveMap.computeIfAbsent(base.getApp(), k -> new HashMap<>()).put(base.getId(), base);
          } else {
            baseWaitMap.computeIfAbsent(base.getApp(), k -> new HashMap<>()).put(base.getId(), base);
          }
        }
      }
    }
    /*ConfList*/
    BaikalConfExample confExample = new BaikalConfExample();
    confExample.createCriteria().andStatusEqualTo(StatusEnum.ONLINE.getStatus()).andUpdateAtLessThanOrEqualTo(now);
    List<BaikalConf> confList = confMapper.selectByExample(confExample);
    if (!CollectionUtils.isEmpty(confList)) {
      for (BaikalConf conf : confList) {
        appSet.add(conf.getApp());
        if (passActiveCheck(conf.getTimeType(), conf.getStart(), conf.getEnd(), now)) {
          confActiveMap.computeIfAbsent(conf.getApp(), k -> new HashMap<>()).put(conf.getId(), conf);
        } else {
          confWaitMap.computeIfAbsent(conf.getApp(), k -> new HashMap<>()).put(conf.getId(), conf);
        }
        if(isLeaf(conf.getType())) {
          Map<Byte, Map<String, Integer>> map = leafClassMap.get(conf.getApp());
          Map<String, Integer> classMap;
          if (map == null) {
            map = new HashMap<>();
            leafClassMap.put(conf.getApp(), map);
            classMap = new HashMap<>();
            map.put(conf.getType(), classMap);
            classMap.put(conf.getConfName(), 0);
          } else {
            classMap = map.get(conf.getType());
            if(classMap == null){
              classMap = new HashMap<>();
              map.put(conf.getType(), classMap);
              classMap.put(conf.getConfName(), 0);
            }else {
              classMap.putIfAbsent(conf.getConfName(), 0);
            }
          }
          classMap.put(conf.getConfName(), classMap.get(conf.getConfName()) + 1);
        }
      }
    }
    lastUpdateTime = now;
  }

  private boolean isLeaf(byte type){
    return type == NodeTypeEnum.LEAF_FLOW.getType() || type == NodeTypeEnum.LEAF_NONE.getType() || type == NodeTypeEnum.LEAF_RESULT.getType();
  }

  /**
   * 时间戳过期校验
   *
   * @param start
   * @return
   */
  private boolean passInvalidCheck(Long id, byte timeType, Date start, Date end, Date now) {
    TimeTypeEnum timeTypeEnum = TimeTypeEnum.getEnum(timeType);
    if (timeTypeEnum == null) {
      log.error("time type not exist id:{}, timeType:{}", id, timeType);
      return false;
    }
    switch (timeTypeEnum) {
      case NONE:
        return true;
      case BETWEEN:
        if (start == null || end == null) {
          log.error("invalid time config id:{}, timeType:{}, baikalStart:{}, baikalEnd:{}", id, timeType, start, end);
          return false;
        }
        /*时间已过*/
        return !now.after(end);
      case AFTER_START:
      case TEST_AFTER_START:
        if (start == null) {
          log.error("invalid time config id:{}, timeType:{}, baikalStart:{}", id, timeType, null);
          return false;
        }
        return true;
      case BEFORE_END:
        if (end == null) {
          log.error("invalid time config id:{}, timeType:{}, baikalEnd:{}", id, timeType, null);
          return false;
        }
        /*时间已过*/
        return !now.after(end);
      case TEST_BETWEEN:
        if (start == null || end == null) {
          log.error("invalid time config id:{}, timeType:{}, baikalStart:{}, baikalEnd:{}", id, timeType, start, end);
          return false;
        }
        return true;
      case TEST_BEFORE_END:
        if (end == null) {
          log.error("invalid time config id:{}, timeType:{}, baikalEnd:{}", id, timeType, null);
          return false;
        }
        return true;
      default:
        break;
    }
    return true;
  }

  /**
   * 时间戳生效中校验(在时间戳检验通过后再校验)
   *
   * @param start
   * @return
   */
  private boolean passActiveCheck(byte timeType, Date start, Date end, Date now) {
    TimeTypeEnum timeTypeEnum = TimeTypeEnum.getEnum(timeType);
    long activeTimeMills = now.getTime() + FOWARD_MILLS;
    switch (timeTypeEnum) {
      case NONE:
      case TEST_BETWEEN:
      case TEST_BEFORE_END:
      case TEST_AFTER_START:
        return true;
      case BETWEEN:
        /*时间正确*/
        return activeTimeMills > start.getTime() && now.before(end);
      case AFTER_START:
        return activeTimeMills > start.getTime();
      case BEFORE_END:
        /*时间正确*/
        return now.before(end);
      default:
        break;
    }
    return false;
  }

  /**
   * 根据app获取生效中的ConfList
   *
   * @param app
   * @return
   */
  @Override
  public Collection<BaikalConfDto> getActiveConfsByApp(Integer app) {
    synchronized (LOCK) {
      Map<Long, BaikalConf> map = confActiveMap.get(app);
      if (map == null) {
        return Collections.emptyList();
      }
      return map.values().stream().map(this::convert).collect(Collectors.toList());
    }
  }

  /**
   * 根据app获取生效中的baseList
   *
   * @param app
   * @return
   */
  @Override
  public Collection<BaikalBaseDto> getActiveBasesByApp(Integer app) {
    synchronized (LOCK) {
      Map<Long, BaikalBase> map = baseActiveMap.get(app);
      if (map == null) {
        return Collections.emptyList();
      }
      return map.values().stream().map(this::convert).collect(Collectors.toList());
    }
  }

  /**
   * 根据app获取初始化json
   *
   * @param app
   * @return
   */
  @Override
  public String getInitJson(Integer app) {
    synchronized (LOCK) {
      Map<String, Object> initMap = new HashMap<>(3);
      initMap.put("insertOrUpdateConfs", this.getActiveConfsByApp(app));
      initMap.put("insertOrUpdateBases", this.getActiveBasesByApp(app));
      initMap.put("version", version);
      return JSON.toJSONString(initMap);
    }
  }

  private BaikalBaseDto convert(BaikalBase base){
    BaikalBaseDto baseDto = new BaikalBaseDto();
    baseDto.setConfId(base.getConfId());
    baseDto.setDebug(base.getDebug());
    baseDto.setId(base.getId());
    baseDto.setStart(base.getStart() == null?0:base.getStart().getTime());
    baseDto.setEnd(base.getEnd() == null?0:base.getEnd().getTime());
    baseDto.setTimeType(base.getTimeType());
    baseDto.setPriority(base.getPriority());
    baseDto.setScenes(base.getScenes());
    baseDto.setStatus(base.getStatus());
    return baseDto;
  }

  private BaikalConfDto convert(BaikalConf conf){
    BaikalConfDto confDto = new BaikalConfDto();
    confDto.setForwardId(conf.getForwardId());
    confDto.setDebug(conf.getDebug());
    confDto.setId(conf.getId());
    confDto.setStart(conf.getStart() == null?0:conf.getStart().getTime());
    confDto.setEnd(conf.getEnd() == null?0:conf.getEnd().getTime());
    confDto.setTimeType(conf.getTimeType());
    confDto.setComplex(conf.getComplex());
    confDto.setSonIds(conf.getSonIds());
    confDto.setStatus(conf.getStatus());
    confDto.setConfName(conf.getConfName());
    confDto.setConfField(conf.getConfField());
    confDto.setInverse(conf.getInverse());
    confDto.setType(conf.getType());
    return confDto;
  }
}

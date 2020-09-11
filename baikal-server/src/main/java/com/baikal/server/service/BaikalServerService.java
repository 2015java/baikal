package com.baikal.server.service;

import com.baikal.common.model.BaikalBaseDto;
import com.baikal.common.model.BaikalConfDto;
import com.baikal.dao.model.BaikalBase;
import com.baikal.dao.model.BaikalConf;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author kowalski
 */
public interface BaikalServerService {

  /**
   * 根据app获取生效中的Conf
   * @param app
   * @return
   */
  Collection<BaikalConfDto> getActiveConfsByApp(Integer app);

  /**
   * 根据app获取生效中的base
   * @param app
   * @return
   */
  Collection<BaikalBaseDto> getActiveBasesByApp(Integer app);

  /**
   * 根据app获取初始化json
   * @param app
   * @return
   */
  String getInitJson(Integer app);

  /**
   * 获取所有的activeBase-从缓存
   * @param app
   * @return
   */
  List<BaikalBase> getBaseActive(Integer app);

  /**
   * 获取所有的active-从库里
   * @param app
   * @return
   */
  List<BaikalBase> getBaseFromDb(Integer app);

  /**
   * 根据confId获取配置信息
   * @param app
   * @param confId
   * @return
   */
  BaikalConf getActiveConfById(Integer app, Long confId);

  Map<String, Integer> getLeafClassMap(Integer app, Byte type);

  void updateByEdit();

  Set<Integer> getAppSet();
  }

package com.baikal.server.service;

import com.baikal.server.model.BaikalConfVo;
import com.baikal.server.model.BaikalBaseVo;
import com.baikal.server.model.WebResult;

/**
 * @author kowalski
 */
public interface BaikalEditService {

  /**
   * get base
   *
   * @param app
   * @param pageIndex
   * @param pageSize
   * @return
   */
  WebResult getBase(Integer app, Integer pageIndex, Integer pageSize);

  /**
   * 编辑base
   *
   * @param baseVo
   * @return
   */
  WebResult editBase(BaikalBaseVo baseVo);

  /**
   * 编辑Conf
   *
   * @param app
   * @param type
   * @param baikalId
   * @param confVo
   * @return
   */
  WebResult editConf(Integer app, Integer type, Long baikalId, BaikalConfVo confVo);

  /**
   * 获取leafClass
   *
   * @param app
   * @param type
   * @return
   */
  WebResult getLeafClass(int app, byte type);

  /**
   * 发布
   *
   * @param app
   * @param baikalId
   * @param reason
   * @return
   */
  WebResult push(Integer app, Long baikalId, String reason);

  /**
   * 发布历史
   *
   * @param app
   * @param baikalId
   * @return
   */
  WebResult history(Integer app, Long baikalId);

  /**
   * 导出数据
   *
   * @param baikalId
   * @param pushId
   * @return
   */
  WebResult exportData(Long baikalId, Long pushId);

  /**
   * 回滚
   *
   * @param pushId
   * @return
   */
  WebResult rollback(Long pushId);

  /**
   * 导入数据
   *
   * @param data
   * @return
   */
  WebResult importData(String data);

//  /**
//   * baikal复制
//   * @param data
//   * @return
//   */
//  WebResult copyData(String data);

}

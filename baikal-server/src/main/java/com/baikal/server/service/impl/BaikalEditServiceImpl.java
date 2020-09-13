package com.baikal.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.baikal.server.model.BaikalConfVo;
import com.baikal.server.model.BaikalLeafClass;
import com.baikal.server.model.PushData;
import com.github.pagehelper.Page;
import com.github.pagehelper.page.PageMethod;
import com.baikal.common.constant.Constant;
import com.baikal.common.enums.NodeTypeEnum;
import com.baikal.dao.mapper.BaikalBaseMapper;
import com.baikal.dao.mapper.BaikalConfMapper;
import com.baikal.dao.mapper.BaikalPushHistoryMapper;
import com.baikal.dao.model.BaikalBase;
import com.baikal.dao.model.BaikalBaseExample;
import com.baikal.dao.model.BaikalConf;
import com.baikal.dao.model.BaikalConfExample;
import com.baikal.dao.model.BaikalPushHistory;
import com.baikal.dao.model.BaikalPushHistoryExample;
import com.baikal.server.model.BaikalBaseVo;
import com.baikal.server.model.WebResult;
import com.baikal.server.service.BaikalEditService;
import com.baikal.server.service.BaikalServerService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author kowalski
 */
@Service
public class BaikalEditServiceImpl implements BaikalEditService {

  @Resource
  private BaikalBaseMapper baseMapper;

  @Resource
  private BaikalConfMapper confMapper;

  @Resource
  private BaikalPushHistoryMapper pushHistoryMapper;

  @Resource
  private BaikalServerService serverService;

  @Resource
  private AmqpTemplate amqpTemplate;

  /**
   * get Base
   *
   * @param app
   */
  @Override
  public WebResult getBase(Integer app, Integer pageIndex, Integer pageSize) {
    BaikalBaseExample example = new BaikalBaseExample();
    example.createCriteria().andAppEqualTo(app);
    example.setOrderByClause("update_at desc");
    Page<BaikalPushHistory> startPage = PageMethod.startPage(pageIndex, pageSize);
    return new WebResult<>(baseMapper.selectByExample(example));
  }

  /**
   * 编辑base
   *
   * @param baseVo
   */
  @Override
  @Transactional
  public WebResult editBase(BaikalBaseVo baseVo) {
    WebResult result = new WebResult<>();
    if (baseVo == null) {
      result.setRet(-1);
      result.setMsg("入参为空");
      return result;
    }
    if (baseVo.getId() == null) {
      /*新增*/
      /*新增的需要在conf里新建一个root root默认是none的status=0*/
      BaikalConf createConf = new BaikalConf();
      createConf.setApp(baseVo.getApp());
      createConf.setType(NodeTypeEnum.NONE.getType());
      createConf.setUpdateAt(new Date());
      confMapper.insertSelective(createConf);
      BaikalBase createBase = convert(baseVo);
      /*存入负值表示新建*/
      createBase.setConfId(-createConf.getId());
      createBase.setUpdateAt(new Date());
      baseMapper.insertSelective(createBase);
      return result;
    }
    /*编辑*/
    BaikalBaseExample example = new BaikalBaseExample();
    example.createCriteria().andIdEqualTo(baseVo.getId());
    baseMapper.updateByExample(convert(baseVo), example);
    return result;
  }

  /**
   * 编辑Conf
   *
   * @param app
   * @param type
   * @param baikalId
   * @param confVo
   * @return
   */
  @Override
  @Transactional
  public WebResult editConf(Integer app, Integer type, Long baikalId, BaikalConfVo confVo) {
    WebResult result = new WebResult<>();

    switch (type) {
      case 1:
        /*新建*/
        if (confVo.getOperateNodeId() == null) {
          /*新建根节点*/
          BaikalBaseExample baseExample = new BaikalBaseExample();
          baseExample.createCriteria().andAppEqualTo(app).andIdEqualTo(baikalId);
          List<BaikalBase> baseList = baseMapper.selectByExample(baseExample);
          if (!CollectionUtils.isEmpty(baseList)) {
            BaikalBase base = baseList.get(0);
            Long rootId = base.getConfId() > 0 ? base.getConfId() : -base.getConfId();
            BaikalConfExample confExample = new BaikalConfExample();
            confExample.createCriteria().andIdEqualTo(rootId);
            BaikalConf conf = new BaikalConf();
            conf.setDebug(confVo.getDebug() ? (byte) 1 : (byte) 0);
            conf.setTimeType(confVo.getTimeType());
            conf.setStart(confVo.getStart() == null?null:new Date(confVo.getStart()));
            conf.setEnd(confVo.getEnd() == null?null:new Date(confVo.getEnd()));
            conf.setInverse(confVo.getInverse() ? (byte) 1 : (byte) 0);
            conf.setType(confVo.getNodeType());
            conf.setName(StringUtils.isEmpty(confVo.getName()) ? "" : confVo.getName());
            if (!isRelation(confVo)) {
              conf.setConfName(confVo.getConfName());
              conf.setConfField(StringUtils.isEmpty(confVo.getConfField()) ? "{}" : confVo.getConfField());
            }
            conf.setUpdateAt(new Date());
            confMapper.updateByExampleSelective(conf, confExample);
            base.setConfId(rootId);
            base.setUpdateAt(new Date());
            baseMapper.updateByExampleSelective(base, baseExample);
          }
        } else {
          BaikalConfExample confExample = new BaikalConfExample();
          confExample.createCriteria().andIdEqualTo(confVo.getOperateNodeId());
          List<BaikalConf> confList = confMapper.selectByExample(confExample);
          if (!CollectionUtils.isEmpty(confList)) {
            BaikalConf operateConf = confList.get(0);
            if (confVo.getNodeId() != null) {
              /*从已知节点ID添加*/
              operateConf.setSonIds(StringUtils.isEmpty(operateConf.getSonIds()) ?
                      String.valueOf(confVo.getNodeId()) :
                      operateConf.getSonIds() + "," + confVo.getNodeId());
            } else {
              BaikalConf createConf = new BaikalConf();
              createConf.setDebug(confVo.getDebug() ? (byte) 1 : (byte) 0);
              createConf.setInverse(confVo.getInverse() ? (byte) 1 : (byte) 0);
              createConf.setTimeType(confVo.getTimeType());
              createConf.setStart(confVo.getStart() == null?null:new Date(confVo.getStart()));
              createConf.setEnd(confVo.getEnd() == null?null:new Date(confVo.getEnd()));
              createConf.setApp(app);
              createConf.setType(confVo.getNodeType());
              createConf.setName(StringUtils.isEmpty(confVo.getName()) ? "" : confVo.getName());
              if (!isRelation(confVo)) {
                createConf.setConfName(confVo.getConfName());
                createConf.setConfField(StringUtils.isEmpty(confVo.getConfField()) ? "{}" : confVo.getConfField());
              }
              createConf.setUpdateAt(new Date());
              confMapper.insertSelective(createConf);
              operateConf.setSonIds(StringUtils.isEmpty(operateConf.getSonIds()) ?
                      String.valueOf(createConf.getId()) :
                      operateConf.getSonIds() + "," + createConf.getId());
            }
            operateConf.setUpdateAt(new Date());
            confMapper.updateByExampleSelective(operateConf, confExample);
          }
        }
        break;
      case 2:
        if (confVo.getOperateNodeId() != null) {
          BaikalConfExample confExample = new BaikalConfExample();
          confExample.createCriteria().andIdEqualTo(confVo.getOperateNodeId());
          List<BaikalConf> confList = confMapper.selectByExample(confExample);
          if (!CollectionUtils.isEmpty(confList)) {
            BaikalConf operateConf = confList.get(0);
            operateConf.setDebug(confVo.getDebug() ? (byte) 1 : (byte) 0);
            operateConf.setTimeType(confVo.getTimeType());
            operateConf.setStart(confVo.getStart() == null?null:new Date(confVo.getStart()));
            operateConf.setEnd(confVo.getEnd() == null?null:new Date(confVo.getEnd()));
            operateConf.setInverse(confVo.getInverse() ? (byte) 1 : (byte) 0);
            operateConf.setName(StringUtils.isEmpty(confVo.getName()) ? "" : confVo.getName());
            if (!isRelation(confVo)) {
              operateConf.setConfName(confVo.getConfName());
              operateConf.setConfField(StringUtils.isEmpty(confVo.getConfField()) ? "{}" : confVo.getConfField());
            }
            operateConf.setUpdateAt(new Date());
            confMapper.updateByExampleSelective(operateConf, confExample);
          }
        }
        break;
      case 3:
        if (confVo.getOperateNodeId() != null) {
          if (confVo.getParentId() != null) {
            BaikalConfExample confExample = new BaikalConfExample();
            confExample.createCriteria().andIdEqualTo(confVo.getParentId());
            List<BaikalConf> confList = confMapper.selectByExample(confExample);
            if (!CollectionUtils.isEmpty(confList)) {
              BaikalConf operateConf = confList.get(0);
              String sonIdStr = operateConf.getSonIds();
              if (!StringUtils.isEmpty(sonIdStr)) {
                String[] sonIdStrs = sonIdStr.split(",");
                StringBuilder sb = new StringBuilder();
                for (String idStr : sonIdStrs) {
                  if (!confVo.getOperateNodeId().toString().equals(idStr)) {
                    sb = sb.append(idStr).append(",");
                  }
                }
                String str = sb.toString();
                if (StringUtils.isEmpty(str)) {
                  operateConf.setSonIds("");
                } else {
                  operateConf.setSonIds(str.substring(0, str.length() - 1));
                }
                operateConf.setUpdateAt(new Date());
                confMapper.updateByExampleSelective(operateConf, confExample);
              }
            }
          } else if (confVo.getNextId() != null) {
            BaikalConfExample confExample = new BaikalConfExample();
            confExample.createCriteria().andIdEqualTo(confVo.getNextId());
            List<BaikalConf> confList = confMapper.selectByExample(confExample);
            if (!CollectionUtils.isEmpty(confList)) {
              BaikalConf operateConf = confList.get(0);
              /*多校验一步*/
              if (operateConf.getForwardId() != null && operateConf.getForwardId().equals(confVo.getOperateNodeId())) {
                operateConf.setForwardId(null);
                operateConf.setUpdateAt(new Date());
                confMapper.updateByExample(operateConf, confExample);
              }
            }
          } else {
            /*该节点没有父节点和next 判断为根节点 根节点删除直接将base表中confId变成负数,避免只是为了改变根节点类型而直接全部删除重做*/
            BaikalBaseExample baseExample = new BaikalBaseExample();
            baseExample.createCriteria().andAppEqualTo(app).andIdEqualTo(baikalId);
            List<BaikalBase> baseList = baseMapper.selectByExample(baseExample);
            if (!CollectionUtils.isEmpty(baseList)) {
              BaikalBase base = baseList.get(0);
              if (base.getConfId().equals(confVo.getOperateNodeId())) {
                /*校验相等再变更*/
                base.setConfId(-confVo.getOperateNodeId());
                base.setUpdateAt(new Date());
                baseMapper.updateByExampleSelective(base, baseExample);
              }
            }
          }
        }
        break;
      case 4:
        if (confVo.getOperateNodeId() != null) {
          BaikalConfExample confExample = new BaikalConfExample();
          confExample.createCriteria().andIdEqualTo(confVo.getOperateNodeId());
          List<BaikalConf> confList = confMapper.selectByExample(confExample);
          if (!CollectionUtils.isEmpty(confList)) {
            BaikalConf operateConf = confList.get(0);
            if (confVo.getNodeId() != null) {
              /*从已知节点ID添加*/
              operateConf.setForwardId(confVo.getNodeId());
            } else {
              BaikalConf createConf = new BaikalConf();
              createConf.setDebug(confVo.getDebug() ? (byte) 1 : (byte) 0);
              createConf.setInverse(confVo.getInverse() ? (byte) 1 : (byte) 0);
              createConf.setTimeType(confVo.getTimeType());
              createConf.setStart(confVo.getStart() == null?null:new Date(confVo.getStart()));
              createConf.setEnd(confVo.getEnd() == null?null:new Date(confVo.getEnd()));
              createConf.setType(confVo.getNodeType());
              createConf.setApp(app);
              createConf.setName(StringUtils.isEmpty(confVo.getName()) ? "" : confVo.getName());
              if (!isRelation(confVo)) {
                createConf.setConfName(confVo.getConfName());
                createConf.setConfField(StringUtils.isEmpty(confVo.getConfField()) ? "{}" : confVo.getConfField());
              }
              createConf.setUpdateAt(new Date());
              confMapper.insertSelective(createConf);
              operateConf.setForwardId(createConf.getId());
            }
            operateConf.setUpdateAt(new Date());
            confMapper.updateByExampleSelective(operateConf, confExample);
          }
        }
        break;
      default:
        result.setRet(-1);
        return result;
    }
    return result;
  }

  public static boolean isRelation(BaikalConfVo dto) {
    return isRelation(dto.getNodeType());
  }

  public static boolean isRelation(Byte type) {
    return type == NodeTypeEnum.NONE.getType() || type == NodeTypeEnum.ALL.getType()
            || type == NodeTypeEnum.AND.getType() || type == NodeTypeEnum.TRUE.getType()
            || type == NodeTypeEnum.ANY.getType();
  }

  /**
   * 获取leafClass
   *
   * @param app
   * @param type
   * @return
   */
  @Override
  public WebResult getLeafClass(int app, byte type) {
    WebResult<List> result = new WebResult<>();
    List<BaikalLeafClass> list = new ArrayList<>();
    if (type == 1) {
      type = 7;
    } else if (type == 2) {
      type = 5;
    } else if (type == 3) {
      type = 6;
    } else {
      result.setData(list);
      return result;
    }
    Map<String, Integer> leafClassMap = serverService.getLeafClassMap(app, type);
    if (leafClassMap != null) {
      for (Map.Entry<String, Integer> entry : leafClassMap.entrySet()) {
        BaikalLeafClass leafClass = new BaikalLeafClass();
        leafClass.setFullName(entry.getKey());
        leafClass.setCount(entry.getValue());
        leafClass.setShortName(entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1));
        list.add(leafClass);
      }
    }
    list.sort(Comparator.comparingInt(BaikalLeafClass::sortNegativeCount));
    result.setData(list);
    return result;
  }

  /**
   * 发布
   *
   * @param app
   * @param baikalId
   * @return
   */
  @Override
  public WebResult push(Integer app, Long baikalId, String reason) {
    WebResult<Long> result = new WebResult<>();
    BaikalBaseExample baseExample = new BaikalBaseExample();
    baseExample.createCriteria().andIdEqualTo(baikalId);
    List<BaikalBase> baseList = baseMapper.selectByExample(baseExample);
    if (CollectionUtils.isEmpty(baseList)) {
      result.setRet(-1);
      result.setMsg("baikalId不存在");
    }
    BaikalBase base = baseList.get(0);
    base.setUpdateAt(new Date());
    if (base.getScenes() == null) {
      base.setScenes("");
    }
    PushData pushData = new PushData();
    pushData.setBase(base);
    Long confId = base.getConfId();
    Object obj = amqpTemplate.convertSendAndReceive(Constant.getShowConfExchange(), String.valueOf(base.getApp()),
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
              Set<Long> allIdSet = new HashSet<>();
              findAllConfIds(rootMap, allIdSet);
              if (!CollectionUtils.isEmpty(allIdSet)) {
                BaikalConfExample confExample = new BaikalConfExample();
                confExample.createCriteria().andIdIn(new ArrayList<>(allIdSet));
                List<BaikalConf> baikalConfs = confMapper.selectByExample(confExample);
                if (!CollectionUtils.isEmpty(baikalConfs)) {
                  for (BaikalConf conf : baikalConfs) {
                    conf.setUpdateAt(new Date());
                    if (isRelation(conf.getType()) && conf.getSonIds() == null) {
                      conf.setSonIds("");
                    }
                  }
                  pushData.setConfs(baikalConfs);
                }
              }
            }
          }
        }
      }
    }
    BaikalPushHistory history = new BaikalPushHistory();
    history.setApp(base.getApp());
    history.setBaikalId(baikalId);
    history.setReason(reason);
    history.setOperator("zjn");
    history.setPushData(JSON.toJSONString(pushData));
    pushHistoryMapper.insertSelective(history);
    result.setData(history.getId());
    return result;
  }

  /**
   * 发布历史
   *
   * @param app
   * @param baikalId
   * @return
   */
  @Override
  public WebResult history(Integer app, Long baikalId) {
    WebResult<List> result = new WebResult<>();
    BaikalPushHistoryExample example = new BaikalPushHistoryExample();
    example.createCriteria().andAppEqualTo(app).andBaikalIdEqualTo(baikalId);
    example.setOrderByClause("create_at desc");
    Page<BaikalPushHistory> startPage = PageMethod.startPage(1, 100);
    pushHistoryMapper.selectByExample(example);
    result.setData(startPage.getResult());
    return result;
  }

  /**
   * 导出数据
   *
   * @param baikalId
   * @param pushId
   * @return
   */
  @Override
  public WebResult exportData(Long baikalId, Long pushId) {
    WebResult<String> result = new WebResult<>();
    if (pushId != null && pushId > 0) {
      BaikalPushHistoryExample historyExample = new BaikalPushHistoryExample();
      historyExample.createCriteria().andIdEqualTo(pushId);
      List<BaikalPushHistory> histories = pushHistoryMapper.selectByExampleWithBLOBs(historyExample);
      if (!CollectionUtils.isEmpty(histories)) {
        result.setData(histories.get(0).getPushData());
        return result;
      }
      result.setRet(-1);
      result.setMsg("pushId不存在");
      return result;
    }
    BaikalBaseExample baseExample = new BaikalBaseExample();
    baseExample.createCriteria().andIdEqualTo(baikalId);
    List<BaikalBase> baseList = baseMapper.selectByExample(baseExample);
    if (CollectionUtils.isEmpty(baseList)) {
      result.setRet(-1);
      result.setMsg("baikalId不存在");
      return result;
    }
    BaikalBase base = baseList.get(0);
    base.setUpdateAt(new Date());
    if (base.getScenes() == null) {
      base.setScenes("");
    }
    PushData pushData = new PushData();
    pushData.setBase(base);
    Long confId = base.getConfId();
    Object obj = amqpTemplate.convertSendAndReceive(Constant.getShowConfExchange(), String.valueOf(base.getApp()),
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
              Set<Long> allIdSet = new HashSet<>();
              findAllConfIds(rootMap, allIdSet);
              if (!CollectionUtils.isEmpty(allIdSet)) {
                BaikalConfExample confExample = new BaikalConfExample();
                confExample.createCriteria().andIdIn(new ArrayList<>(allIdSet));
                List<BaikalConf> baikalConfs = confMapper.selectByExample(confExample);
                if (!CollectionUtils.isEmpty(baikalConfs)) {
                  for (BaikalConf conf : baikalConfs) {
                    conf.setUpdateAt(new Date());
                    if (isRelation(conf.getType()) && conf.getSonIds() == null) {
                      conf.setSonIds("");
                    }
                  }
                  pushData.setConfs(baikalConfs);
                }
              }
            }
          }
        }
      }
    }
    result.setData(JSON.toJSONString(pushData));
    return result;
  }

  /**
   * 回滚
   *
   * @param pushId
   * @return
   */
  @Override
  @Transactional
  public WebResult rollback(Long pushId) {
    WebResult result = new WebResult<>();
    if (pushId != null && pushId > 0) {
      BaikalPushHistoryExample historyExample = new BaikalPushHistoryExample();
      historyExample.createCriteria().andIdEqualTo(pushId);
      List<BaikalPushHistory> histories = pushHistoryMapper.selectByExampleWithBLOBs(historyExample);
      if (!CollectionUtils.isEmpty(histories)) {
        importData(histories.get(0).getPushData());
        return result;
      }
      result.setRet(-1);
      result.setMsg("pushId不存在");
    }
    return result;
  }

  private void findAllConfIds(Map map, Set<Long> ids) {
    Long nodeId = (Long) map.get("baikalNodeId");
    if (nodeId != null) {
      ids.add(nodeId);
    }
    Map foward = (Map) map.get("baikalForward");
    if (foward != null) {
      findAllConfIds(foward, ids);
    }
    List<Map> children = getChild(map);
    if (CollectionUtils.isEmpty(children)) {
      return;
    }
    for (Map child : children) {
      findAllConfIds(child, ids);
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map> getChild(Map map) {
    return (List) map.get("children");
  }

  /**
   * 导入数据
   *
   * @param data
   * @return
   */
  @Override
  @Transactional
  public WebResult importData(String data) {
    WebResult result = new WebResult<>();
    PushData pushData = JSON.parseObject(data, PushData.class);
    BaikalBase base = pushData.getBase();
    List<BaikalConf> confs = pushData.getConfs();
    if (!CollectionUtils.isEmpty(confs)) {
      for (BaikalConf conf : confs) {
        BaikalConfExample confExample = new BaikalConfExample();
        confExample.createCriteria().andIdEqualTo(conf.getId());
        List<BaikalConf> confList = confMapper.selectByExample(confExample);
        if (CollectionUtils.isEmpty(confList)) {
          conf.setCreateAt(null);
          conf.setUpdateAt(new Date());
          confMapper.insertSelectiveWithId(conf);
        } else {
          conf.setId(null);
          conf.setUpdateAt(new Date());
          confMapper.updateByExampleSelective(conf, confExample);
        }
      }
    }
    if (base != null) {
      BaikalBaseExample baseExample = new BaikalBaseExample();
      baseExample.createCriteria().andIdEqualTo(base.getId());
      List<BaikalBase> baseList = baseMapper.selectByExample(baseExample);
      if (CollectionUtils.isEmpty(baseList)) {
        base.setCreateAt(null);
        base.setUpdateAt(new Date());
        baseMapper.insertSelectiveWithId(base);
      } else {
        base.setId(null);
        base.setUpdateAt(new Date());
        baseMapper.updateByExampleSelective(base, baseExample);
      }
    }
    return result;
  }

//  /**
//   * 复制baikal
//   *
//   * @param data
//   * @return
//   */
//  @Override
//  @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
//  public WebResult copyData(String data) {
//    WebResult result = new WebResult<>();
//    PushData pushData = JSON.parseObject(data, PushData.class);
//    Map<Long, Long> oldIds2NewIds = getOldIds2NewIdsMap(pushData);
//
//    BaikalBase base = pushData.getBase();
//    if (base != null) {
//      Long maxBaseId = getMaxBaseId() + 1;
//      BaikalBaseExample baseExample = new BaikalBaseExample();
//      baseExample.createCriteria().andIdEqualTo(maxBaseId);
//      List<BaikalBase> baseList = baseMapper.selectByExample(baseExample);
//      if (CollectionUtils.isEmpty(baseList)) {
//        base.setName(base.getName() + "_copy");
//        base.setId(maxBaseId);
//        base.setConfId(oldIds2NewIds.get(base.getConfId()));
//        base.setCreateAt(null);
//        base.setUpdateAt(new Date());
//        baseMapper.insertSelectiveWithId(base);
//      }
//    }

//    List<BaikalConf> confs = pushData.getConfs();
//    if (!CollectionUtils.isEmpty(confs)) {
//      for (BaikalConf conf : confs) {
//        Long newConfId = oldIds2NewIds.get(conf.getId());
//        BaikalConfExample confExample = new BaikalConfExample();
//        confExample.createCriteria().andIdEqualTo(newConfId);
//        List<BaikalConf> confList = confMapper.selectByExample(confExample);
//        if (CollectionUtils.isEmpty(confList)) {
//          Long oldForwardId = conf.getForwardId();
//          Long forwardId = oldIds2NewIds.get(conf.getForwardId()) == null ? oldForwardId : oldIds2NewIds.get(oldForwardId);
//          String sonIds = getSonIds(oldIds2NewIds, conf.getSonIds());
//          conf.setId(newConfId);
//          conf.setForwardId(forwardId);
//          conf.setSonIds(sonIds);
//          conf.setCreateAt(null);
//          conf.setUpdateAt(new Date());
//          confMapper.insertSelectiveWithId(conf);
//        }
//      }
//    }
//
//    return result;
//  }

  private BaikalBase convert(BaikalBaseVo vo) {
    BaikalBase base = new BaikalBase();
    base.setId(vo.getId());
    base.setTimeType(vo.getTimeType());
    base.setApp(vo.getApp());
    base.setDebug(vo.getDebug());
    base.setName(vo.getName());
    base.setScenes(vo.getScenes() == null ? "" : vo.getScenes());
    base.setStart(vo.getStart());
    base.setEnd(vo.getEnd());
    base.setStatus(vo.getStatus());
    base.setPriority(1L);
    base.setUpdateAt(new Date());
    return base;
  }

//  private Map<Long, Long> getOldIds2NewIdsMap(PushData pushData) {
//    if (Objects.isNull(pushData) || Objects.isNull(pushData.getBase())) {
//      return Maps.newHashMap();
//    }
//
//    List<Long> noNeedCopyList = Arrays.stream(noNeedCopyIds.split(",")).map(Long::parseLong).collect(Collectors.toList());
//    Long maxConfId = 1L;
//    BaikalConfExample confExample = new BaikalConfExample();
//    confExample.setOrderByClause("id desc");
//    Page<BaikalConf> startPage = PageMethod.startPage(0, 1);
//    List<BaikalConf> confList = confMapper.selectByExample(confExample);
//    if (!CollectionUtils.isEmpty(confList)) {
//      maxConfId = confList.get(0).getId();
//    }
//
//    Map<Long, Long> oldId2NewIdMap = new HashedMap<>(pushData.getConfs().size() + 1);
//    Long baseId = pushData.getBase().getConfId();
//    if (noNeedCopyList.contains(baseId)) {
//      oldId2NewIdMap.put(baseId, baseId);
//    } else {
//      oldId2NewIdMap.put(baseId, ++maxConfId);
//    }
//    if (pushData.getConfs().size() > 0) {
//      for (BaikalConf conf : pushData.getConfs()) {
//        if (noNeedCopyList.contains(conf.getId())) {
//          oldId2NewIdMap.put(conf.getId(), conf.getId());
//        } else {
//          Long newId = oldId2NewIdMap.get(conf.getId());
//          if (Objects.isNull(newId)) {
//            oldId2NewIdMap.put(conf.getId(), ++maxConfId);
//          }
//        }
//      }
//    }
//    return oldId2NewIdMap;
//  }

//  /**
//   * 处理sonIds
//   *
//   * @param oldIds2NewIds
//   * @param oldSonIds
//   * @return
//   */
//  private String getSonIds(Map<Long, Long> oldIds2NewIds, String oldSonIds) {
//    if (StringUtils.isEmpty(oldSonIds)) {
//      return null;
//    }
//    if (oldIds2NewIds.size() == 0) {
//      return oldSonIds;
//    }
//    List<String> ids = Lists.newLinkedList();
//    for (String id : oldSonIds.split(",")) {
//      Long newId = oldIds2NewIds.get(Long.parseLong(id)) == null ? Long.parseLong(id) : oldIds2NewIds.get(Long.parseLong(id));
//      ids.add(newId.toString());
//    }
//    return String.join(",", ids);
//  }

//  /**
//   * 查询base id最大值
//   *
//   * @return
//   */
//  private Long getMaxBaseId() {
//    Long maxBaseId = 1L;
//    BaikalBaseExample baseExample = new BaikalBaseExample();
//    baseExample.setOrderByClause("id desc");
//    Page<BaikalConf> startPage = PageMethod.startPage(0, 1);
//    List<BaikalBase> bases = baseMapper.selectByExample(baseExample);
//    if (bases.size() > 0) {
//      maxBaseId = bases.get(0).getId();
//    }
//
//    return maxBaseId;
//  }
}

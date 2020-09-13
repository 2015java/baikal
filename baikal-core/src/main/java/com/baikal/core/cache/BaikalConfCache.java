package com.baikal.core.cache;

import com.alibaba.fastjson.JSON;
import com.baikal.common.enums.NodeTypeEnum;
import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.common.model.BaikalConfDto;
import com.baikal.core.leaf.base.BaseLeafFlow;
import com.baikal.core.leaf.base.BaseLeafNone;
import com.baikal.core.leaf.base.BaseLeafResult;
import com.baikal.core.utils.BaikalBeanUtils;
import com.baikal.core.utils.BaikalLinkedList;
import com.baikal.core.base.BaseNode;
import com.baikal.core.base.BaseRelation;
import com.baikal.core.relation.AllRelation;
import com.baikal.core.relation.AndRelation;
import com.baikal.core.relation.AnyRelation;
import com.baikal.core.relation.NoneRelation;
import com.baikal.core.relation.TrueRelation;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kowalski
 * conf配置缓存
 * conf配置的实例化,层级构建,更新
 */
@Slf4j
public final class BaikalConfCache {

  private static final String REGEX_COMMA = ",";

  private static final Map<Long, BaseNode> confMap = new ConcurrentHashMap<>();

  private static final Map<Long, Set<Long>> parentIdsMap = new ConcurrentHashMap<>();

  private static final Map<Long, Set<Long>> forwardUseIdsMap = new ConcurrentHashMap<>();

  /**
   * 根据ID获取Conf配置
   *
   * @param id
   * @return
   */
  public static BaseNode getConfById(Long id) {
    if (id == null) {
      return null;
    }
    return confMap.get(id);
  }

  public static Map<Long, BaseNode> getConfMap() {
    return confMap;
  }

  /**
   * 缓存更新
   *
   * @param baikalConfDtos
   * @return
   */
  public static List<String> insertOrUpdate(List<BaikalConfDto> baikalConfDtos) {
    List<String> errors = new ArrayList<>(baikalConfDtos.size());
    Map<Long, BaseNode> tmpConfMap = new HashMap<>(baikalConfDtos.size());

    for (BaikalConfDto confDto : baikalConfDtos) {
      try {
        tmpConfMap.put(confDto.getId(), convert(confDto));
      } catch (Exception e) {
        String errorNodeStr = JSON.toJSONString(confDto);
        errors.add("error conf:" + errorNodeStr);
        log.error("baikal error conf:{} please check! e:", errorNodeStr, e);
      }
    }
    for (BaikalConfDto confInfo : baikalConfDtos) {
      BaseNode origin = confMap.get(confInfo.getId());
      if (isRelation(confInfo)) {
        List<Long> sonIds;
        if (confInfo.getSonIds() == null || confInfo.getSonIds().isEmpty()) {
          sonIds = Collections.emptyList();
        } else {
          String[] sonIdStrs = confInfo.getSonIds().split(REGEX_COMMA);
          sonIds = new ArrayList<>();
          for (String sonStr : sonIdStrs) {
            sonIds.add(Long.valueOf(sonStr));
          }
          for (Long sonId : sonIds) {
            Set<Long> parentIds = parentIdsMap.get(sonId);
            if (parentIds == null || parentIds.isEmpty()) {
              parentIds = new HashSet<>();
              parentIdsMap.put(sonId, parentIds);
            }
            parentIds.add(confInfo.getId());
            BaseNode tmpNode = tmpConfMap.get(sonId);
            if (tmpNode == null) {
              tmpNode = confMap.get(sonId);
            }
            if (tmpNode == null) {
              String errorModeStr = JSON.toJSONString(confInfo);
              errors.add("sonId:" + sonId + " not exist conf:" + errorModeStr);
              log.error("sonId:{} not exist please check! conf:{}", sonId, errorModeStr);
            } else {
              ((BaseRelation) tmpConfMap.get(confInfo.getId())).getChildren().add(tmpNode);
            }
          }
        }
        BaseRelation originRelation = (BaseRelation) origin;
        if (originRelation != null && originRelation.getChildren() != null && !originRelation.getChildren().isEmpty()) {
          Set<Long> sonIdSet = new HashSet<>(sonIds);
          BaikalLinkedList<BaseNode> children = originRelation.getChildren();
          BaikalLinkedList.Node<BaseNode> listNode = children.getFirst();
          while (listNode != null) {
            BaseNode sonNode = listNode.item;
            if (sonNode != null && !sonIdSet.contains(sonNode.findBaikalNodeId())) {
              Set<Long> parentIds = parentIdsMap.get(sonNode.findBaikalNodeId());
              if (parentIds != null) {
                parentIds.remove(originRelation.findBaikalNodeId());
              }
            }
            listNode = listNode.next;
          }
        }
      }
      if (origin != null && origin.getBaikalForward() != null) {
        if (confInfo.getForwardId() == null || confInfo.getForwardId() != origin.getBaikalForward()
            .findBaikalNodeId()) {
          Set<Long> forwardUseIds = forwardUseIdsMap.get(origin.getBaikalForward().findBaikalNodeId());
          if (forwardUseIds != null) {
            forwardUseIds.remove(origin.findBaikalNodeId());
          }
        }
      }
      if (confInfo.getForwardId() != null) {
        Set<Long> fowardUseIds = forwardUseIdsMap.get(confInfo.getForwardId());
        if (fowardUseIds == null || fowardUseIds.isEmpty()) {
          fowardUseIds = new HashSet<>();
          forwardUseIdsMap.put(confInfo.getForwardId(), fowardUseIds);
        }
        fowardUseIds.add(confInfo.getId());
      }
      if (confInfo.getForwardId() != null) {
        BaseNode tmpForwardNode = tmpConfMap.get(confInfo.getForwardId());
        if (tmpForwardNode == null) {
          tmpForwardNode = confMap.get(confInfo.getForwardId());
        }
        if (tmpForwardNode == null) {
          String errorModeStr = JSON.toJSONString(confInfo);
          errors.add("forwardId:" + confInfo.getForwardId() + " not exist conf:" + errorModeStr);
          log.error("forwardId:{} not exist please check! conf:{}", confInfo.getForwardId(), errorModeStr);
        } else {
          tmpConfMap.get(confInfo.getId()).setBaikalForward(tmpForwardNode);
        }
      }
    }
    confMap.putAll(tmpConfMap);
    for (BaikalConfDto confInfo : baikalConfDtos) {
      Set<Long> parentIds = parentIdsMap.get(confInfo.getId());
      if (parentIds != null && !parentIds.isEmpty()) {
        for (Long parentId : parentIds) {
          BaseNode tmpParentNode = confMap.get(parentId);
          if (tmpParentNode == null) {
            String errorModeStr = JSON.toJSONString(confInfo);
            errors.add("parentId:" + parentId + " not exist conf:" + errorModeStr);
            log.error("parentId:{} not exist please check! conf:{}", parentId, errorModeStr);
          } else {
            BaseRelation relation = (BaseRelation) tmpParentNode;
            BaikalLinkedList<BaseNode> children = relation.getChildren();
            if (children != null && !children.isEmpty()) {
              BaikalLinkedList.Node<BaseNode> listNode = children.getFirst();
              while (listNode != null) {
                BaseNode node = listNode.item;
                if (node != null && node.findBaikalNodeId() == confInfo.getId()) {
                  listNode.item = confMap.get(confInfo.getId());
                }
                listNode = listNode.next;
              }
            }
          }
        }
      }
      Set<Long> forwardUseIds = forwardUseIdsMap.get(confInfo.getId());
      if (forwardUseIds != null && !forwardUseIds.isEmpty()) {
        for (Long forwardUseId : forwardUseIds) {
          BaseNode tmpNode = confMap.get(forwardUseId);
          if (tmpNode == null) {
            String errorModeStr = JSON.toJSONString(confInfo);
            errors.add("forwardUseId:" + forwardUseId + " not exist conf:" + errorModeStr);
            log.error("forwardUseId:{} not exist please check! conf:{}", forwardUseId, errorModeStr);
          } else {
            BaseNode forward = confMap.get(confInfo.getId());
            if (forward != null) {
              tmpNode.setBaikalForward(forward);
            }
          }
        }
      }
      BaseNode tmpNode = confMap.get(confInfo.getId());
      if (tmpNode != null) {
        BaikalHandlerCache.updateHandlerRoot(tmpNode);
      }
    }
    return errors;
  }

  public static void delete(List<Long> ids) {
    //FIXME 删除的相关性问题(父子节点  forward)
    for (Long id : ids) {
      Set<Long> parentIds = parentIdsMap.get(id);
      if (parentIds != null && !parentIds.isEmpty()) {
        for (Long parentId : parentIds) {
          BaseNode parentNode = confMap.get(parentId);
          BaseRelation relation = (BaseRelation) parentNode;
          BaikalLinkedList<BaseNode> children = relation.getChildren();
          if (children != null && !children.isEmpty()) {
            BaikalLinkedList.Node<BaseNode> listNode = children.getFirst();
            while (listNode != null) {
              BaseNode node = listNode.item;
              if (node != null && node.findBaikalNodeId() == id) {
                children.remove(node);
              }
              listNode = listNode.next;
            }
          }
        }
      }
      confMap.remove(id);
    }
  }

  public static boolean isRelation(BaikalConfDto dto) {
    return dto.getType() == NodeTypeEnum.NONE.getType() || dto.getType() == NodeTypeEnum.ALL.getType()
        || dto.getType() == NodeTypeEnum.AND.getType() || dto.getType() == NodeTypeEnum.TRUE.getType()
        || dto.getType() == NodeTypeEnum.ANY.getType();
  }

  private static BaseNode convert(BaikalConfDto confDto) throws ClassNotFoundException {
    switch (NodeTypeEnum.getEnum(confDto.getType())) {
      case LEAF_FLOW:
        BaseLeafFlow flow = (BaseLeafFlow) JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            Class.forName(confDto.getConfName()));
        flow.setBaikalNodeId(confDto.getId());
        flow.setBaikalNodeDebug(confDto.getDebug() == 1);
        flow.setBaikalInverse(confDto.getInverse() == 1);
        flow.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        flow.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        flow.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        BaikalBeanUtils.autowireBean(flow);
        return flow;
      case LEAF_RESULT:
        BaseLeafResult result = (BaseLeafResult) JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            Class.forName(confDto.getConfName()));
        result.setBaikalNodeId(confDto.getId());
        result.setBaikalNodeDebug(confDto.getDebug() == 1);
        result.setBaikalInverse(confDto.getInverse() == 1);
        result.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        result.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        result.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        BaikalBeanUtils.autowireBean(result);
        return result;
      case LEAF_NONE:
        BaseLeafNone none = (BaseLeafNone) JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            Class.forName(confDto.getConfName()));
        none.setBaikalNodeId(confDto.getId());
        none.setBaikalNodeDebug(confDto.getDebug() == 1);
        none.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        none.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        none.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        BaikalBeanUtils.autowireBean(none);
        return none;
      case NONE:
        NoneRelation noneRelation = JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            NoneRelation.class);
        noneRelation.setBaikalNodeId(confDto.getId());
        noneRelation.setBaikalNodeDebug(confDto.getDebug() == 1);
        noneRelation.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        noneRelation.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        noneRelation.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        return noneRelation;
      case AND:
        AndRelation andRelation = JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            AndRelation.class);
        andRelation.setBaikalNodeId(confDto.getId());
        andRelation.setBaikalNodeDebug(confDto.getDebug() == 1);
        andRelation.setBaikalInverse(confDto.getInverse() == 1);
        andRelation.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        andRelation.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        andRelation.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        return andRelation;
      case TRUE:
        TrueRelation trueRelation = JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            TrueRelation.class);
        trueRelation.setBaikalNodeId(confDto.getId());
        trueRelation.setBaikalNodeDebug(confDto.getDebug() == 1);
        trueRelation.setBaikalInverse(confDto.getInverse() == 1);
        trueRelation.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        trueRelation.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        trueRelation.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        return trueRelation;
      case ALL:
        AllRelation allRelation = JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            AllRelation.class);
        allRelation.setBaikalNodeId(confDto.getId());
        allRelation.setBaikalNodeDebug(confDto.getDebug() == 1);
        allRelation.setBaikalInverse(confDto.getInverse() == 1);
        allRelation.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        allRelation.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        allRelation.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        return allRelation;
      case ANY:
        AnyRelation anyRelation = JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            AnyRelation.class);
        anyRelation.setBaikalNodeId(confDto.getId());
        anyRelation.setBaikalNodeDebug(confDto.getDebug() == 1);
        anyRelation.setBaikalInverse(confDto.getInverse() == 1);
        anyRelation.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        anyRelation.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        anyRelation.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        return anyRelation;
      default:
        BaseNode baseNode = (BaseNode) JSON.parseObject(confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            Class.forName(confDto.getConfName()));
        baseNode.setBaikalNodeId(confDto.getId());
        baseNode.setBaikalNodeDebug(confDto.getDebug() == 1);
        baseNode.setBaikalInverse(confDto.getInverse() == 1);
        baseNode.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        baseNode.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        baseNode.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        BaikalBeanUtils.autowireBean(baseNode);
        return baseNode;
    }
  }
}

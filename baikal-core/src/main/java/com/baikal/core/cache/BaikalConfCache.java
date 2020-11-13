package com.baikal.core.cache;

import com.alibaba.fastjson.JSON;
import com.baikal.common.enums.NodeTypeEnum;
import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.common.model.BaikalConfDto;
import com.baikal.core.annotation.BaikalParam;
import com.baikal.core.base.BaseNode;
import com.baikal.core.base.BaseRelation;
import com.baikal.core.context.BaikalPack;
import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.base.BaseLeafFlow;
import com.baikal.core.leaf.base.BaseLeafNone;
import com.baikal.core.leaf.base.BaseLeafResult;
import com.baikal.core.leaf.pack.BaseLeafPackFlow;
import com.baikal.core.leaf.pack.BaseLeafPackNone;
import com.baikal.core.leaf.pack.BaseLeafPackResult;
import com.baikal.core.relation.AllRelation;
import com.baikal.core.relation.AndRelation;
import com.baikal.core.relation.AnyRelation;
import com.baikal.core.relation.NoneRelation;
import com.baikal.core.relation.TrueRelation;
import com.baikal.core.utils.BaikalBeanUtils;
import com.baikal.core.utils.BaikalLinkedList;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
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

  private static BaseNode convert(BaikalConfDto confDto) throws ClassNotFoundException, NoSuchMethodException {
    switch (NodeTypeEnum.getEnum(confDto.getType())) {
      case LEAF_FLOW:
        BaseLeafFlow flow;
        String flowFiled = confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField();
        String flowConfNameFirst = confDto.getConfName().substring(0, 1);
        if (flowConfNameFirst.equals("#") || flowConfNameFirst.equals("$")) {
          String clzName;
          Class<?> clz;
          Object targetObj = null;
          String methodName;
          String logName;
          if (flowConfNameFirst.equals("#")) {
            String[] confNames = confDto.getConfName().split("#");
            clzName = confNames[1];
            methodName = confNames[2];
            clz = Class.forName(clzName);
            logName = "#" + clz.getSimpleName() + "#" + methodName;
          } else {
            String[] confNames = confDto.getConfName().split("\\$");
            clzName = confNames[1];
            methodName = confNames[2];
            targetObj = BaikalBeanUtils.getBean(clzName);
            if (targetObj == null) {
              throw new ClassNotFoundException("bean named " + clzName + "not found");
            }
            clz = targetObj.getClass();
            logName = "$" + clzName + "$" + methodName;
          }
          Method[] methods = clz.getDeclaredMethods();
          Method targetMethod = null;
          for (Method method : methods) {
            if (method.getName().equals(methodName)) {
              targetMethod = method;
              break;
            }
          }
          if (targetMethod == null) {
            throw new NoSuchMethodException(clzName + " have no method named " + methodName);
          }
          targetMethod.setAccessible(true);
          if (targetObj == null && !Modifier.isStatic(targetMethod.getModifiers())) {
            targetObj = JSON.parseObject(flowFiled, clz);
            BaikalBeanUtils.autowireBean(targetObj);
          }
          Parameter[] params = targetMethod.getParameters();
          int length = params.length;
          String[] targetKeys = new String[length];
          for (int i = 0; i < length; i++) {
            Parameter parameter = params[i];
            BaikalParam param = parameter.getAnnotation(BaikalParam.class);
            targetKeys[i] = param == null ? methodName + "_" + i : param.value();
          }
          final Method finalTargetMethod = targetMethod;
          final Object finalTargetObj = targetObj;
          flow = new BaseLeafPackFlow() {
            @Override
            protected boolean doPackFlow(BaikalPack pack) throws InvocationTargetException, IllegalAccessException {
              Object[] params = new Object[length];
              BaikalRoam roam = pack.getRoam();
              for (int i = 0; i < length; i++) {
                String argvName = targetKeys[i];
                if (argvName.equals("time") || argvName.equals("requestTime")) {
                  params[i] = pack.getRequestTime();
                }
                params[i] = roam.getMulti(argvName);
              }
              if (finalTargetMethod.getReturnType() == boolean.class || finalTargetMethod.getReturnType() == Boolean.class) {
                Boolean res = (Boolean) finalTargetMethod.invoke(finalTargetObj, params);
                return res != null && res;
              }
              finalTargetMethod.invoke(finalTargetObj, params);
              return true;
            }
          };
          flow.setBaikalLogName(logName);
        } else {
          flow = (BaseLeafFlow) JSON.parseObject(flowFiled, Class.forName(confDto.getConfName()));
          flow.setBaikalLogName(flow.getClass().getSimpleName());
          BaikalBeanUtils.autowireBean(flow);
        }
        flow.setBaikalNodeId(confDto.getId());
        flow.setBaikalNodeDebug(confDto.getDebug() == 1);
        flow.setBaikalInverse(confDto.getInverse() == 1);
        flow.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        flow.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        flow.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        return flow;
      case LEAF_RESULT:
        BaseLeafResult result;
        String resultFiled = confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField();
        String resultConfNameFirst = confDto.getConfName().substring(0, 1);
        if (resultConfNameFirst.equals("#") || resultConfNameFirst.equals("$")) {
          String clzName;
          Class<?> clz;
          Object targetObj = null;
          String methodName;
          String logName;
          if (resultConfNameFirst.equals("#")) {
            String[] confNames = confDto.getConfName().split("#");
            clzName = confNames[1];
            methodName = confNames[2];
            clz = Class.forName(clzName);
            logName = "#" + clz.getSimpleName() + "#" + methodName;
          } else {
            String[] confNames = confDto.getConfName().split("\\$");
            clzName = confNames[1];
            methodName = confNames[2];
            targetObj = BaikalBeanUtils.getBean(clzName);
            if (targetObj == null) {
              throw new ClassNotFoundException("bean named " + clzName + "not found");
            }
            clz = targetObj.getClass();
            logName = "$" + clzName + "$" + methodName;
          }
          Method[] methods = clz.getDeclaredMethods();
          Method targetMethod = null;
          for (Method method : methods) {
            if (method.getName().equals(methodName)) {
              targetMethod = method;
              break;
            }
          }
          if (targetMethod == null) {
            throw new NoSuchMethodException(clzName + " have no method named " + methodName);
          }
          targetMethod.setAccessible(true);
          if (targetObj == null && !Modifier.isStatic(targetMethod.getModifiers())) {
            targetObj = JSON.parseObject(resultFiled, clz);
            BaikalBeanUtils.autowireBean(targetObj);
          }
          Parameter[] params = targetMethod.getParameters();
          int length = params.length;
          String[] targetKeys = new String[length];
          for (int i = 0; i < length; i++) {
            Parameter parameter = params[i];
            BaikalParam param = parameter.getAnnotation(BaikalParam.class);
            targetKeys[i] = param == null ? methodName + "_" + i : param.value();
          }
          final Method finalTargetMethod = targetMethod;
          final Object finalTargetObj = targetObj;
          result = new BaseLeafPackResult() {
            @Override
            protected boolean doPackResult(BaikalPack pack) throws InvocationTargetException, IllegalAccessException {
              Object[] params = new Object[length];
              BaikalRoam roam = pack.getRoam();
              for (int i = 0; i < length; i++) {
                String argvName = targetKeys[i];
                if (argvName.equals("time") || argvName.equals("requestTime")) {
                  params[i] = pack.getRequestTime();
                }
                params[i] = roam.getMulti(argvName);
              }
              if (finalTargetMethod.getReturnType() == boolean.class || finalTargetMethod.getReturnType() == Boolean.class) {
                Boolean res = (Boolean) finalTargetMethod.invoke(finalTargetObj, params);
                return res != null && res;
              }
              finalTargetMethod.invoke(finalTargetObj, params);
              return true;
            }
          };
          result.setBaikalLogName(logName);
        } else {
          result = (BaseLeafResult) JSON.parseObject(resultFiled, Class.forName(confDto.getConfName()));
          result.setBaikalLogName(result.getClass().getSimpleName());
          BaikalBeanUtils.autowireBean(result);
        }
        result.setBaikalNodeId(confDto.getId());
        result.setBaikalNodeDebug(confDto.getDebug() == 1);
        result.setBaikalInverse(confDto.getInverse() == 1);
        result.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        result.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        result.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        return result;
      case LEAF_NONE:
        BaseLeafNone none;
        String noneFiled = confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField();
        String noneConfNameFirst = confDto.getConfName().substring(0, 1);
        if (noneConfNameFirst.equals("#") || noneConfNameFirst.equals("$")) {
          String clzName;
          Class<?> clz;
          Object targetObj = null;
          String methodName;
          String logName;
          if (noneConfNameFirst.equals("#")) {
            String[] confNames = confDto.getConfName().split("#");
            clzName = confNames[1];
            methodName = confNames[2];
            clz = Class.forName(clzName);
            logName = "#" + clz.getSimpleName() + "#" + methodName;
          } else {
            String[] confNames = confDto.getConfName().split("\\$");
            clzName = confNames[1];
            methodName = confNames[2];
            targetObj = BaikalBeanUtils.getBean(clzName);
            if (targetObj == null) {
              throw new ClassNotFoundException("bean named " + clzName + "not found");
            }
            clz = targetObj.getClass();
            logName = "$" + clzName + "$" + methodName;
          }
          Method[] methods = clz.getDeclaredMethods();
          Method targetMethod = null;
          for (Method method : methods) {
            if (method.getName().equals(methodName)) {
              targetMethod = method;
              break;
            }
          }
          if (targetMethod == null) {
            throw new NoSuchMethodException(clzName + " have no method named " + methodName);
          }
          targetMethod.setAccessible(true);
          if (targetObj == null && !Modifier.isStatic(targetMethod.getModifiers())) {
            targetObj = JSON.parseObject(noneFiled, clz);
            BaikalBeanUtils.autowireBean(targetObj);
          }
          Parameter[] params = targetMethod.getParameters();
          int length = params.length;
          String[] targetKeys = new String[length];
          for (int i = 0; i < length; i++) {
            Parameter parameter = params[i];
            BaikalParam param = parameter.getAnnotation(BaikalParam.class);
            targetKeys[i] = param == null ? methodName + "_" + i : param.value();
          }
          final Method finalTargetMethod = targetMethod;
          final Object finalTargetObj = targetObj;
          none = new BaseLeafPackNone() {
            @Override
            protected void doPackNone(BaikalPack pack) throws InvocationTargetException, IllegalAccessException {
              Object[] params = new Object[length];
              BaikalRoam roam = pack.getRoam();
              for (int i = 0; i < length; i++) {
                String argvName = targetKeys[i];
                if (argvName.equals("time") || argvName.equals("requestTime")) {
                  params[i] = pack.getRequestTime();
                }
                params[i] = roam.getMulti(argvName);
              }
              finalTargetMethod.invoke(finalTargetObj, params);
            }
          };
          none.setBaikalLogName(logName);
        } else {
          none = (BaseLeafNone) JSON.parseObject(noneFiled, Class.forName(confDto.getConfName()));
          none.setBaikalLogName(none.getClass().getSimpleName());
          BaikalBeanUtils.autowireBean(none);
        }
        none.setBaikalNodeId(confDto.getId());
        none.setBaikalNodeDebug(confDto.getDebug() == 1);
        none.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
        none.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
        none.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
        return none;
      case NONE:
        NoneRelation noneRelation = JSON.parseObject(
            confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" : confDto.getConfField(),
            NoneRelation.class);
        noneRelation.setBaikalLogName("None");
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
        andRelation.setBaikalLogName("And");
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
        trueRelation.setBaikalLogName("True");
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
        allRelation.setBaikalLogName("All");
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
        anyRelation.setBaikalLogName("Any");
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
        baseNode.setBaikalLogName(baseNode.getClass().getSimpleName());
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

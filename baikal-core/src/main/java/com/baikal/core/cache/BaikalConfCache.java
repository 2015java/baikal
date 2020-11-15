package com.baikal.core.cache;

import com.alibaba.fastjson.JSON;
import com.baikal.common.enums.NodeTypeEnum;
import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.common.model.BaikalConfDto;
import com.baikal.core.annotation.BaikalParam;
import com.baikal.core.annotation.BaikalRes;
import com.baikal.core.base.BaseNode;
import com.baikal.core.base.BaseRelation;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.context.BaikalPack;
import com.baikal.core.context.BaikalRoam;
import com.baikal.core.leaf.base.BaseLeafFlow;
import com.baikal.core.leaf.base.BaseLeafNone;
import com.baikal.core.leaf.base.BaseLeafResult;
import com.baikal.core.relation.AllRelation;
import com.baikal.core.relation.AndRelation;
import com.baikal.core.relation.AnyRelation;
import com.baikal.core.relation.NoneRelation;
import com.baikal.core.relation.TrueRelation;
import com.baikal.core.utils.BaikalBeanUtils;
import com.baikal.core.utils.BaikalLinkedList;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
   * @param baikalConfDtos dto
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
    BaseNode node;
    switch (NodeTypeEnum.getEnum(confDto.getType())) {
      case LEAF_FLOW:
        String flowFiled = confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField();
        String flowConfNameFirst = confDto.getConfName().substring(0, 1);
        if (flowConfNameFirst.equals("#") || flowConfNameFirst.equals("$")) {
          final BaikalProxy proxy = getProxy(flowConfNameFirst, confDto);
          node = new BaseLeafFlow() {
            @Override
            protected boolean doFlow(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
              BaikalPack pack = cxt.getPack();
              BaikalRoam roam = pack.getRoam();
              Object[] params = getParams(proxy, cxt, pack, roam);
              Method method = proxy.method;
              if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
                Boolean res = (Boolean) method.invoke(proxy.instance, params);
                return res != null && res;
              }
              if (method.getReturnType() != void.class && method.getReturnType() != Void.class) {
                roam.putMulti(proxy.resKey, method.invoke(proxy.instance, params));
                return true;
              }
              method.invoke(proxy.instance, params);
              return true;
            }
          };
          node.setBaikalLogName(proxy.logName);
        } else {
          node = (BaseLeafFlow) JSON.parseObject(flowFiled, Class.forName(confDto.getConfName()));
          node.setBaikalLogName(node.getClass().getSimpleName());
          BaikalBeanUtils.autowireBean(node);
        }
        break;
      case LEAF_RESULT:
        String resultFiled = confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField();
        String resultConfNameFirst = confDto.getConfName().substring(0, 1);
        if (resultConfNameFirst.equals("#") || resultConfNameFirst.equals("$")) {
          final BaikalProxy proxy = getProxy(resultConfNameFirst, confDto);
          node = new BaseLeafResult() {
            @Override
            protected boolean doResult(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
              BaikalPack pack = cxt.getPack();
              BaikalRoam roam = pack.getRoam();
              Object[] params = getParams(proxy, cxt, pack, roam);
              Method method = proxy.method;
              if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
                Boolean res = (Boolean) method.invoke(proxy.instance, params);
                return res != null && res;
              }
              if (method.getReturnType() != void.class && method.getReturnType() != Void.class) {
                roam.putMulti(proxy.resKey, method.invoke(proxy.instance, params));
                return true;
              }
              method.invoke(proxy.instance, params);
              return true;
            }
          };
          node.setBaikalLogName(proxy.logName);
        } else {
          node = (BaseLeafResult) JSON.parseObject(resultFiled, Class.forName(confDto.getConfName()));
          node.setBaikalLogName(node.getClass().getSimpleName());
          BaikalBeanUtils.autowireBean(node);
        }
        break;
      case LEAF_NONE:
        String noneFiled = confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField();
        String noneConfNameFirst = confDto.getConfName().substring(0, 1);
        if (noneConfNameFirst.equals("#") || noneConfNameFirst.equals("$")) {
          final BaikalProxy proxy = getProxy(noneConfNameFirst, confDto);
          node = new BaseLeafNone() {
            @Override
            protected void doNone(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
              BaikalPack pack = cxt.getPack();
              BaikalRoam roam = pack.getRoam();
              Object[] params = getParams(proxy, cxt, pack, roam);
              Method method = proxy.method;
              if (method.getReturnType() != void.class && method.getReturnType() != Void.class) {
                roam.putMulti(proxy.resKey, method.invoke(proxy.instance, params));
              }
              method.invoke(proxy.instance, params);
            }
          };
          node.setBaikalLogName(proxy.logName);
        } else {
          node = (BaseLeafNone) JSON.parseObject(noneFiled, Class.forName(confDto.getConfName()));
          node.setBaikalLogName(node.getClass().getSimpleName());
          BaikalBeanUtils.autowireBean(node);
        }
        break;
      case NONE:
        node = JSON.parseObject(confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField(), NoneRelation.class);
        node.setBaikalLogName("None");
        break;
      case AND:
        node = JSON.parseObject(confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField(), AndRelation.class);
        node.setBaikalLogName("And");
        break;
      case TRUE:
        node = JSON.parseObject(confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField(), TrueRelation.class);
        node.setBaikalLogName("True");
        break;
      case ALL:
        node = JSON.parseObject(confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField(), AllRelation.class);
        node.setBaikalLogName("All");
        break;
      case ANY:
        node = JSON.parseObject(confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField(), AnyRelation.class);
        node.setBaikalLogName("Any");
        break;
      default:
        node = (BaseNode) JSON.parseObject(confDto.getConfField() == null || confDto.getConfField().isEmpty() ? "{}" :
            confDto.getConfField(), Class.forName(confDto.getConfName()));
        node.setBaikalLogName(node.getClass().getSimpleName());
        BaikalBeanUtils.autowireBean(node);
        break;
    }
    node.setBaikalNodeId(confDto.getId());
    node.setBaikalNodeDebug(confDto.getDebug() == 1);
    node.setBaikalInverse(confDto.getInverse() == 1);
    node.setBaikalTimeTypeEnum(TimeTypeEnum.getEnum(confDto.getTimeType()));
    node.setBaikalStart(confDto.getStart() == null ? 0 : confDto.getStart());
    node.setBaikalEnd(confDto.getEnd() == null ? 0 : confDto.getEnd());
    return node;
  }

  private static Object toArray(Object input, Class<?> type) {
    if (input != null && input.getClass().isArray()) {
      return input;
    }
    Collection<?> collection = (Collection<?>) input;
    if (!type.getComponentType().isPrimitive()) {
      if (collection == null) {
        return (type == Object[].class) ? new Object[0] : Array.newInstance(type.getComponentType(), 0);
      }
      Object copy = (type == Object[].class) ? new Object[collection.size()] :
          Array.newInstance(type.getComponentType(), collection.size());
      collection.toArray((Object[]) copy);
      return copy;
    }
    if (type == int[].class) {
      if (collection == null) {
        return new int[0];
      }
      int[] res = new int[collection.size()];
      Iterator<?> iterator = collection.iterator();
      for (int i = 0; i < collection.size(); i++) {
        res[i] = Integer.parseInt(iterator.next().toString());
      }
      return res;
    }
    if (type == long[].class) {
      if (collection == null) {
        return new long[0];
      }
      long[] res = new long[collection.size()];
      Iterator<?> iterator = collection.iterator();
      for (int i = 0; i < collection.size(); i++) {
        res[i] = Long.parseLong(iterator.next().toString());
      }
      return res;
    }
    if (type == float[].class) {
      if (collection == null) {
        return new float[0];
      }
      float[] res = new float[collection.size()];
      Iterator<?> iterator = collection.iterator();
      for (int i = 0; i < collection.size(); i++) {
        res[i] = Float.parseFloat(iterator.next().toString());
      }
      return res;
    }
    if (type == double[].class) {
      if (collection == null) {
        return new double[0];
      }
      double[] res = new double[collection.size()];
      Iterator<?> iterator = collection.iterator();
      for (int i = 0; i < collection.size(); i++) {
        res[i] = Double.parseDouble(iterator.next().toString());
      }
      return res;
    }
    if (type == boolean[].class) {
      if (collection == null) {
        return new boolean[0];
      }
      boolean[] res = new boolean[collection.size()];
      Iterator<?> iterator = collection.iterator();
      for (int i = 0; i < collection.size(); i++) {
        res[i] = Boolean.parseBoolean(iterator.next().toString());
      }
      return res;
    }
    if (type == byte[].class) {
      if (collection == null) {
        return new byte[0];
      }
      byte[] res = new byte[collection.size()];
      Iterator<?> iterator = collection.iterator();
      for (int i = 0; i < collection.size(); i++) {
        res[i] = Byte.parseByte(iterator.next().toString());
      }
      return res;
    }
    if (type == short[].class) {
      if (collection == null) {
        return new short[0];
      }
      short[] res = new short[collection.size()];
      Iterator<?> iterator = collection.iterator();
      for (int i = 0; i < collection.size(); i++) {
        res[i] = Short.parseShort(iterator.next().toString());
      }
      return res;
    }
    if (type == char[].class) {
      if (collection == null) {
        return new char[0];
      }
      char[] res = new char[collection.size()];
      Iterator<?> iterator = collection.iterator();
      for (int i = 0; i < collection.size(); i++) {
        res[i] = iterator.next().toString().toCharArray()[0];
      }
      return res;
    }
    return null;
  }

  private static BaikalProxy getProxy(String nameFirst, BaikalConfDto confDto)
      throws ClassNotFoundException, NoSuchMethodException {
    String instanceClzName;
    Class<?> instanceClz;
    Object instance = null;
    String methodName;
    String logName;
    if (nameFirst.equals("#")) {
      String[] confNames = confDto.getConfName().split("#");
      instanceClzName = confNames[1];
      methodName = confNames[2];
      instanceClz = Class.forName(instanceClzName);
      logName = "#" + instanceClz.getSimpleName() + "#" + methodName;
    } else {
      String[] confNames = confDto.getConfName().split("\\$");
      instanceClzName = confNames[1];
      methodName = confNames[2];
      instance = BaikalBeanUtils.getBean(instanceClzName);
      if (instance == null) {
        throw new ClassNotFoundException("bean named " + instanceClzName + "not found");
      }
      instanceClz = instance.getClass();
      logName = "$" + instanceClzName + "$" + methodName;
    }
    Method[] methods = instanceClz.getDeclaredMethods();
    Method method = null;
    for (Method m : methods) {
      if (m.getName().equals(methodName)) {
        method = m;
        break;
      }
    }
    if (method == null) {
      throw new NoSuchMethodException(instanceClzName + " have no method named " + methodName);
    }
    method.setAccessible(true);
    if (instance == null && !Modifier.isStatic(method.getModifiers())) {
      instance = JSON.parseObject(confDto.getConfField(), instanceClz);
      BaikalBeanUtils.autowireBean(instance);
    }
    Parameter[] params = method.getParameters();
    int argLength = params.length;
    String[] argKeys = new String[argLength];
    boolean[] argIsArrays = new boolean[argLength];
    Class<?>[] argArrayTypes = new Class<?>[argLength];
    for (int i = 0; i < argLength; i++) {
      Parameter parameter = params[i];
      if (parameter.getType().isArray()) {
        argIsArrays[i] = true;
        argArrayTypes[i] = parameter.getType();
      }
      BaikalParam param = parameter.getAnnotation(BaikalParam.class);
      argKeys[i] = param == null ? methodName + "_" + i : param.value();
    }
    String resKey;
    BaikalRes baikalRes = method.getAnnotation(BaikalRes.class);
    if (baikalRes == null) {
      resKey = methodName + "_res";
    } else {
      resKey = baikalRes.value();
    }
    return new BaikalProxy(method, instance, argKeys, resKey, argIsArrays, argArrayTypes, argLength, logName);
  }

  private static Object[] getParams(BaikalProxy proxy, BaikalContext cxt, BaikalPack pack, BaikalRoam roam) {
    Object[] params;
    if (proxy.argLength > 0) {
      params = new Object[proxy.argLength];
      for (int i = 0; i < proxy.argLength; i++) {
        String argName = proxy.argKeys[i];
        if (proxy.argIsArrays[i]) {
          params[i] = toArray(roam.getMulti(argName), proxy.argArrayTypes[i]);
        } else {
          params[i] = roam.getMulti(argName);
        }
        if (argName.equals("time")) {
          params[i] = pack.getRequestTime();
          continue;
        }
        if (argName.equals("roam")) {
          params[i] = roam;
          continue;
        }
        if (argName.equals("pack")) {
          params[i] = pack;
          continue;
        }
        if (argName.equals("cxt")) {
          params[i] = cxt;
        }
      }
    } else {
      params = null;
    }
    return params;
  }

  @AllArgsConstructor
  private final static class BaikalProxy {
    final Method method;
    final Object instance;
    final String[] argKeys;
    final String resKey;
    final boolean[] argIsArrays;
    final Class<?>[] argArrayTypes;
    final int argLength;
    final String logName;
  }
}

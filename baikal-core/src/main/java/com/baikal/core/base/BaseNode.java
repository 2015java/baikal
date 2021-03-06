package com.baikal.core.base;

import com.baikal.common.enums.NodeRunStateEnum;
import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.utils.BaikalTimeUtils;
import com.baikal.core.utils.ProcessUtils;
import lombok.Data;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 * 基础Node
 * 注意:开发时应避免与基础字段一致
 */
@Data
public abstract class BaseNode {
  /**
   * 节点ID
   */
  private long baikalNodeId;
  /**
   * 时间类型
   */
  private TimeTypeEnum baikalTimeTypeEnum;
  /**
   * 开始时间
   */
  private long baikalStart;
  /**
   * 结束时间
   */
  private long baikalEnd;
  /**
   * baikalNodeDebug
   */
  private boolean baikalNodeDebug;
  /**
   * 反转
   * 1.仅对TRUE和FALSE反转
   * 2.对OUTTIME,NONE的反转无效
   */
  private boolean baikalInverse;
  /**
   * 前置节点
   * 如果前置节点返回FALSE,节点的执行将被拒绝
   * forward节点可以理解为是用AND连接的forward和this
   */
  private BaseNode baikalForward;
  /**
   * 同步锁 默认不开启
   */
  private boolean baikalLock;
  /**
   * 事务 默认不开启
   */
  private boolean baikalTransaction;

  private String baikalLogName;

  /**
   * process
   *
   * @param cxt 入参
   * @return true(f通过 r获得) false(f不通过 r丢失)
   */
  public NodeRunStateEnum process(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    cxt.setCurrentId(this.baikalNodeId);
    if (!BaikalTimeUtils.timeCheck(baikalTimeTypeEnum, cxt.getPack().getRequestTime(), baikalStart, baikalEnd)) {
      ProcessUtils.collectInfo(cxt.getProcessInfo(), this, 'O');
      return NodeRunStateEnum.NONE;
    }
    long start = System.currentTimeMillis();
    if (baikalForward != null) {
      NodeRunStateEnum forwardRes = baikalForward.process(cxt);
      if (forwardRes != NodeRunStateEnum.FALSE) {
        NodeRunStateEnum res = processNode(cxt);
        res = forwardRes == NodeRunStateEnum.NONE ? res : (res == NodeRunStateEnum.NONE ? NodeRunStateEnum.TRUE : res);
        ProcessUtils.collectInfo(cxt.getProcessInfo(), this, start, res);
        return baikalInverse ?
            res == NodeRunStateEnum.TRUE ?
                NodeRunStateEnum.FALSE :
                res == NodeRunStateEnum.FALSE ? NodeRunStateEnum.TRUE : res :
            res;
      }
      ProcessUtils.collectRejectInfo(cxt.getProcessInfo(), this);
      return NodeRunStateEnum.FALSE;
    }
    NodeRunStateEnum res = processNode(cxt);
    ProcessUtils.collectInfo(cxt.getProcessInfo(), this, start, res);
    return baikalInverse ?
        res == NodeRunStateEnum.TRUE ?
            NodeRunStateEnum.FALSE :
            res == NodeRunStateEnum.FALSE ? NodeRunStateEnum.TRUE : res :
        res;
  }

  /**
   * processNode
   *
   * @param cxt 入参
   * @return 节点执行结果
   */
  protected abstract NodeRunStateEnum processNode(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException;

  public Long getBaikalNodeId() {
    return baikalNodeId;
  }

  public long findBaikalNodeId() {
    return baikalNodeId;
  }
}

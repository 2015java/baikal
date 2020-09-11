package com.baikal.core.relation;

import com.baikal.common.enums.NodeRunStateEnum;
import com.baikal.core.base.BaseNode;
import com.baikal.core.base.BaseRelation;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.utils.BaikalLinkedList;

/**
 * @author kowalski
 * 流程->AND关系
 * 有一个子节点返回FALSE将中断执行
 * 有FALSE->FALSE
 * 无FALSE有TRUE->TRUE
 * 无子节点->NONE
 * 全NONE->NONE
 */
public final class AndRelation extends BaseRelation {

  /**
   * 节点复杂度
   * 装配节点时会根据配置的复杂情况调整节点位置
   */
  private int complex;

  /**
   * process relation and
   *
   * @param cxt
   */
  @Override
  protected NodeRunStateEnum processNode(BaikalContext cxt) {
    BaikalLinkedList<BaseNode> children = this.getChildren();
    if (children == null || children.isEmpty()) {
      return NodeRunStateEnum.NONE;
    }
    boolean hasTrue = false;
    int loop = this.getLoop();
    if (loop == 0) {
      for (BaikalLinkedList.Node<BaseNode> listNode = children.getFirst(); listNode != null; listNode = listNode.next) {
        BaseNode node = listNode.item;
        if (node != null) {
          NodeRunStateEnum stateEnum = node.process(cxt);
          if (stateEnum == NodeRunStateEnum.FALSE) {
            return NodeRunStateEnum.FALSE;
          }
          if (!hasTrue) {
            hasTrue = stateEnum == NodeRunStateEnum.TRUE;
          }
        }
      }
    } else if (loop < 0) {
      loop = 0;
      while (true) {
        loop++;
        cxt.setCurrentLoop(loop);
        for (BaikalLinkedList.Node<BaseNode> listNode = children.getFirst();
             listNode != null; listNode = listNode.next) {
          BaseNode node = listNode.item;
          if (node != null) {
            NodeRunStateEnum stateEnum = node.process(cxt);
            if (stateEnum == NodeRunStateEnum.FALSE) {
              return NodeRunStateEnum.FALSE;
            }
            if (!hasTrue) {
              hasTrue = stateEnum == NodeRunStateEnum.TRUE;
            }
          }
        }
      }
    } else {
      for (; loop > 0; loop--) {
        cxt.setCurrentLoop(loop);
        for (BaikalLinkedList.Node<BaseNode> listNode = children.getFirst();
             listNode != null; listNode = listNode.next) {
          BaseNode node = listNode.item;
          if (node != null) {
            NodeRunStateEnum stateEnum = node.process(cxt);
            if (stateEnum == NodeRunStateEnum.FALSE) {
              return NodeRunStateEnum.FALSE;
            }
            if (!hasTrue) {
              hasTrue = stateEnum == NodeRunStateEnum.TRUE;
            }
          }
        }
      }
    }

    if (hasTrue) {
      return NodeRunStateEnum.TRUE;
    }
    return NodeRunStateEnum.NONE;
  }
}

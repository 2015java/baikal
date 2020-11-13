package com.baikal.core.relation;

import com.baikal.common.enums.NodeRunStateEnum;
import com.baikal.core.base.BaseNode;
import com.baikal.core.base.BaseRelation;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.utils.BaikalLinkedList;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 * 结果->ANY关系
 * 有一个子节点返回TRUE将中断执行
 * 有TRUE->TRUE
 * 无TRUE有FALSE->FALSE
 * 无子节点->NONE
 * 全NONE->NONE
 */
public final class AnyRelation extends BaseRelation {
  /**
   * process relation any
   *
   * @param cxt
   */
  @Override
  protected NodeRunStateEnum processNode(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    BaikalLinkedList<BaseNode> children = this.getChildren();
    if (children == null || children.isEmpty()) {
      return NodeRunStateEnum.NONE;
    }
    boolean hasFalse = false;
    int loop = this.getLoop();
    if (loop == 0) {
      for (BaikalLinkedList.Node<BaseNode> listNode = children.getFirst(); listNode != null; listNode = listNode.next) {
        BaseNode node = listNode.item;
        if (node != null) {
          NodeRunStateEnum stateEnum = node.process(cxt);
          if (stateEnum == NodeRunStateEnum.TRUE) {
            return NodeRunStateEnum.TRUE;
          }
          if (!hasFalse) {
            hasFalse = stateEnum == NodeRunStateEnum.FALSE;
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
            if (stateEnum == NodeRunStateEnum.TRUE) {
              return NodeRunStateEnum.TRUE;
            }
            if (!hasFalse) {
              hasFalse = stateEnum == NodeRunStateEnum.FALSE;
            }
          }
        }
      }
    } else {
      for (int i = 0; i < loop; i++) {
        cxt.setCurrentLoop(i);
        for (BaikalLinkedList.Node<BaseNode> listNode = children.getFirst();
             listNode != null; listNode = listNode.next) {
          BaseNode node = listNode.item;
          if (node != null) {
            NodeRunStateEnum stateEnum = node.process(cxt);
            if (stateEnum == NodeRunStateEnum.TRUE) {
              return NodeRunStateEnum.TRUE;
            }
            if (!hasFalse) {
              hasFalse = stateEnum == NodeRunStateEnum.FALSE;
            }
          }
        }
      }
    }
    if (hasFalse) {
      return NodeRunStateEnum.FALSE;
    }
    return NodeRunStateEnum.NONE;
  }
}

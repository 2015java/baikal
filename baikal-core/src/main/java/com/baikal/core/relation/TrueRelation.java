package com.baikal.core.relation;

import com.baikal.common.enums.NodeRunStateEnum;
import com.baikal.core.base.BaseNode;
import com.baikal.core.base.BaseRelation;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.utils.BaikalLinkedList;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 * 结果->TRUE关系
 * 子节点全部执行
 * 无子节点->TRUE
 * 有子节点->TRUE
 */
public final class TrueRelation extends BaseRelation {
  /**
   * process relation true
   *
   * @param cxt
   */
  @Override
  protected NodeRunStateEnum processNode(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    BaikalLinkedList<BaseNode> children = this.getChildren();
    if (children == null || children.isEmpty()) {
      return NodeRunStateEnum.TRUE;
    }

    int loop = this.getLoop();
    if (loop == 0) {
      for (BaikalLinkedList.Node<BaseNode> listNode = children.getFirst(); listNode != null; listNode = listNode.next) {
        BaseNode node = listNode.item;
        if (node != null) {
          node.process(cxt);
        }
      }
    } else {
      for (int i = 0; i < loop; i++) {
        cxt.setCurrentLoop(i);
        for (BaikalLinkedList.Node<BaseNode> listNode = children.getFirst();
             listNode != null; listNode = listNode.next) {
          BaseNode node = listNode.item;
          if (node != null) {
            node.process(cxt);
          }
        }
      }
    }
    return NodeRunStateEnum.TRUE;
  }
}

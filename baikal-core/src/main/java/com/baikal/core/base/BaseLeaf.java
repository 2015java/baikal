package com.baikal.core.base;

import com.baikal.common.enums.ErrorHandleEnum;
import com.baikal.common.enums.NodeRunStateEnum;
import com.baikal.core.context.BaikalContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 * @author kowalski
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public abstract class BaseLeaf extends BaseNode {

  /**
   * 默认仅中止执行SHUT_DOWN
   */
  private ErrorHandleEnum baikalErrorHandleEnum = ErrorHandleEnum.SHUT_DOWN;

  /**
   * processNode
   *
   * @param cxt 入参
   * @return 节点执行结果
   */
  @Override
  protected NodeRunStateEnum processNode(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException {
    try {
      return doLeaf(cxt);
    } catch (Exception e) {
      switch (baikalErrorHandleEnum) {
        case CONTINUE_NONE:
          if (this.isBaikalNodeDebug()) {
            log.error("error occur in {} handle with none", this.findBaikalNodeId());
          }
          return NodeRunStateEnum.NONE;
        case CONTINUE_FALSE:
          if (this.isBaikalNodeDebug()) {
            log.error("error occur in {} handle with false", this.findBaikalNodeId());
          }
          return NodeRunStateEnum.FALSE;
        case CONTINUE_TRUE:
          if (this.isBaikalNodeDebug()) {
            log.error("error occur in {} handle with true", this.findBaikalNodeId());
          }
          return NodeRunStateEnum.TRUE;
        case SHUT_DOWN:
          if (this.isBaikalNodeDebug()) {
            log.error("error occur in {} handle with shut down", this.findBaikalNodeId());
          }
          throw e;
        case SHUT_DOWN_STORE:
          if (this.isBaikalNodeDebug()) {
            log.error("error occur in {} handle with shut down store", this.findBaikalNodeId());
          }
          //TODO store
          throw e;
        default:
          throw e;
      }
    }
  }

  /**
   * process leaf
   *
   * @param cxt
   * @return
   */
  protected abstract NodeRunStateEnum doLeaf(BaikalContext cxt) throws InvocationTargetException, IllegalAccessException;
}

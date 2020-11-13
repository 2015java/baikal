package com.baikal.core.handler;

import com.alibaba.fastjson.JSON;
import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.common.exception.NodeException;
import com.baikal.core.base.BaseNode;
import com.baikal.core.context.BaikalContext;
import com.baikal.core.utils.BaikalTimeUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * @author kowalski
 * 通过scene和baikalId获取到的由配置产生的具体的执行者
 */
@Slf4j
@Data
public final class BaikalHandler {

  /**
   * baikalId
   */
  private long baikalId;

  /**
   * 场景
   */
  private Set<String> scenes;
  /**
   * 时间类型
   *
   * @see TimeTypeEnum
   */
  private TimeTypeEnum timeTypeEnum;
  /**
   * 开始时间
   */
  private long start;
  /**
   * 结束时间
   */
  private long end;

  /**
   * handler的debug
   * 控制着入参,出参与执行过程的打印
   */
  private byte debug;

  /**
   * 执行根节点
   */
  private BaseNode root;

  public void handle(BaikalContext cxt) {
    if (DebugEnum.filter(DebugEnum.IN_PACK, debug)) {
      log.info("handle id:{} in pack:{}", this.baikalId, JSON.toJSONString(cxt.getPack()));
    }
    if (!BaikalTimeUtils.timeCheck(timeTypeEnum, cxt.getPack().getRequestTime(), start, end)) {
      return;
    }
    try {
      if (root != null) {
        root.process(cxt);
        if (DebugEnum.filter(DebugEnum.PROCESS, debug)) {
          log.info("handle id:{} process:{}", this.baikalId, cxt.getProcessInfo().toString());
        }
        if (DebugEnum.filter(DebugEnum.OUT_PACK, debug)) {
          log.info("handle id:{} out pack:{}", this.baikalId, JSON.toJSONString(cxt.getPack()));
        } else {
          if (DebugEnum.filter(DebugEnum.OUT_ROAM, debug)) {
            log.info("handle id:{} out roam:{}", this.baikalId, JSON.toJSONString(cxt.getPack().getRoam()));
          }
        }
      } else {
        log.error("root not exist please check! baikalId:{}", this.baikalId);
      }
    } catch (NodeException ne) {
      log.error("error occur in node baikalId:{} node:{} cxt:{}", this.baikalId, cxt.getCurrentId(), JSON.toJSONString(cxt), ne);
    } catch (Exception e) {
      log.error("error occur baikalId:{} node:{} cxt:{}", this.baikalId, cxt.getCurrentId(), JSON.toJSONString(cxt), e);
    }
  }

  public Long getBaikalId() {
    return this.baikalId;
  }

  public long findBaikalId() {
    return this.baikalId;
  }

  /**
   * handler的debug枚举
   * 控制着入参,出参与执行过程的打印
   */
  private enum DebugEnum {
    /**
     * 入参PACK 1
     */
    IN_PACK,
    /**
     * 执行过程(和节点debug一并使用) 2
     */
    PROCESS,
    /**
     * 结局ROAM 4
     */
    OUT_ROAM,
    /**
     * 结局PACK 8
     */
    OUT_PACK;

    private final byte mask;

    DebugEnum() {
      this.mask = (byte) (1 << ordinal());
    }

    public static boolean filter(DebugEnum debugEnum, byte debug) {
      return (debugEnum.mask & debug) != 0;
    }
  }
}

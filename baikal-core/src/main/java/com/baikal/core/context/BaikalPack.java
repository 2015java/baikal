package com.baikal.core.context;

import com.baikal.common.enums.RequestTypeEnum;
import com.baikal.common.utils.UUIDUtils;
import lombok.Data;
import lombok.ToString;

/**
 * @author kowalski
 * 进入Baikal执行的请求体
 */
@Data
@ToString
public final class BaikalPack {

  /**
   * 请求的 baikalId
   */
  private long baikalId;
  /**
   * 请求的场景 baikalId为空时必填
   */
  private String scene;
  /**
   * 直接将confId作为root发起调用
   */
  private long confId;
  /**
   * 游荡字段
   * 进入baikal的字段是不带G-的
   * 带G-的是业务生成字段
   */
  private volatile BaikalRoam roam = new BaikalRoam();
  /**
   * 请求类型 默认正式
   *
   * @see RequestTypeEnum
   */
  private int type = RequestTypeEnum.FORMAL.getType();
  /**
   * 请求时间
   */
  private long requestTime;
  /**
   * 追踪ID
   */
  private String traceId;
  /**
   * 优先级 如果为0则以执行的handler的优先级为准
   */
  private long priority;

  /**
   * 1.handler 最终以debug|handler.debug展示
   * 2.confRoot 最终以this.debug展示
   */
  private byte debug;

  public BaikalPack newPack(BaikalRoam roam) {
    BaikalPack pack = new BaikalPack(traceId, requestTime);
    pack.setBaikalId(baikalId);
    pack.setScene(scene);
    if(roam != null) {
      /*此处没有用深拷贝*/
      pack.setRoam(new BaikalRoam(roam));
    }
    pack.setType(type);
    pack.setPriority(priority);
    return pack;
  }

  public BaikalPack() {
    this.setTraceId(UUIDUtils.generateMost22UUID());
    this.requestTime = System.currentTimeMillis();
  }

  public BaikalPack(String traceId, long requestTime) {
    if (traceId == null || traceId.isEmpty()) {
      /*traceId为空时,生成traceId*/
      this.setTraceId(UUIDUtils.generateMost22UUID());
    } else {
      this.traceId = traceId;
    }
    if (requestTime <= 0) {
      this.requestTime = System.currentTimeMillis();
    } else {
      this.requestTime = requestTime;
    }
  }

  public BaikalPack(long requestTime) {
    /*traceId为空时,生成traceId*/
    this.setTraceId(UUIDUtils.generateMost22UUID());
    if (requestTime <= 0) {
      this.requestTime = System.currentTimeMillis();
    } else {
      this.requestTime = requestTime;
    }
  }

  public BaikalPack(String traceId) {
    if (traceId == null || traceId.isEmpty()) {
      /*traceId为空时,生成traceId*/
      this.setTraceId(UUIDUtils.generateMost22UUID());
    } else {
      this.traceId = traceId;
    }
    this.requestTime = System.currentTimeMillis();
  }
}

package com.baikal.core.context;

import lombok.Data;
import lombok.ToString;

/**
 * @author kowalski
 * Baikal执行上下文
 */
@Data
@ToString
public final class BaikalContext {

  public BaikalContext(long baikalId, BaikalPack pack) {
    this.baikalId = baikalId;
    this.pack = pack == null ? new BaikalPack() : pack;
  }

  /**
   * 执行的baikalId
   */
  private long baikalId;
  /**
   * 请求内容
   */
  private BaikalPack pack;
  /**
   * 进入baikal开始执行的瞬间(cxt初始化瞬间)
   */
  private final long baikalTime = System.currentTimeMillis();
  /**
   * 当前正在执行的节点ID
   */
  private long currentId;

  /**
   * 当前正在执行的节点的父节点ID
   */
  private long currentParentId;
  /**
   * 当前循环点
   */
  private int currentLoop;
  /***
   * 当前正在执行节点的后置节点ID
   */
  private long nextId;
  /**
   * debug为true的节点执行过程信息
   */
  private StringBuilder processInfo = new StringBuilder();
}

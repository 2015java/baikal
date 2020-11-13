package com.baikal.core.builder;

import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.core.base.BaseNode;
import com.baikal.core.cache.BaikalHandlerCache;
import com.baikal.core.handler.BaikalHandler;
import lombok.Data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author kowalski
 */
@Data
public class BaikalBuilder {

  private BaikalHandler handler;

  public BaikalBuilder(BaseNode root) {
    this.handler = new BaikalHandler();
    this.handler.setScenes(new HashSet<>());
    this.handler.setTimeTypeEnum(TimeTypeEnum.NONE);
    this.handler.setRoot(root);
  }

  public static BaikalBuilder root(BaseNode root) {
    return new BaikalBuilder(root);
  }

  public static BaikalBuilder root(BaseBuilder builder) {
    return new BaikalBuilder(builder.build());
  }

  public BaikalBuilder scene(String... scene) {
    Set<String> originScene = handler.getScenes();
    if (originScene == null) {
      originScene = new HashSet<>();
    }
    originScene.addAll(Arrays.asList(scene));
    return this;
  }

  public BaikalBuilder start(long start) {
    this.handler.setStart(start);
    return this;
  }

  public BaikalBuilder end(long end) {
    this.handler.setEnd(end);
    return this;
  }

  public BaikalBuilder timeType(TimeTypeEnum typeEnum) {
    this.handler.setTimeTypeEnum(typeEnum);
    return this;
  }

  public BaikalBuilder debug(byte debug) {
    this.handler.setDebug(debug);
    return this;
  }

  public void register() {
    BaikalHandlerCache.onlineOrUpdateHandler(this.handler);
  }

  public void register(String... scene) {
    this.scene(scene);
    BaikalHandlerCache.onlineOrUpdateHandler(this.handler);
  }
}

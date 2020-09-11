package com.baikal.core.builder;

import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.core.base.BaseNode;
import lombok.Data;

/**
 * @author kowalski
 */
@Data
public class BaseBuilder {

  private BaseNode node;

  public BaseNode build() {
    return node;
  }

  public BaseBuilder forward(BaseNode forward) {
    this.node.setBaikalForward(forward);
    return this;
  }

  public BaseBuilder forward(BaseBuilder builder) {
    this.node.setBaikalForward(builder.build());
    return this;
  }

  public BaseBuilder start(long start) {
    this.node.setBaikalStart(start);
    return this;
  }

  public BaseBuilder end(long end) {
    this.node.setBaikalEnd(end);
    return this;
  }

  public BaseBuilder timeType(TimeTypeEnum typeEnum) {
    this.node.setBaikalTimeTypeEnum(typeEnum);
    return this;
  }

  public BaseBuilder inverse() {
    this.node.setBaikalInverse(true);
    return this;
  }

  public BaseBuilder debug() {
    this.node.setBaikalNodeDebug(true);
    return this;
  }

  public BaseBuilder(BaseNode node) {
    if (node.getBaikalTimeTypeEnum() == null) {
      node.setBaikalTimeTypeEnum(TimeTypeEnum.NONE);
    }
    node.setBaikalNodeDebug(true);
    this.node = node;
  }
}
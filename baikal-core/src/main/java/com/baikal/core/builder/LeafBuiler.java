package com.baikal.core.builder;

import com.baikal.common.enums.ErrorHandleEnum;
import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.core.base.BaseLeaf;
import com.baikal.core.base.BaseNode;

/**
 * @author kowalski
 */
public class LeafBuiler extends BaseBuilder {

  public static LeafBuiler leaf(BaseLeaf leaf) {
    return new LeafBuiler(leaf);
  }

  @Override
  public LeafBuiler forward(BaseNode forward) {
    return (LeafBuiler) super.forward(forward);
  }

  @Override
  public LeafBuiler forward(BaseBuilder builder) {
    return (LeafBuiler) super.forward(builder);
  }

  @Override
  public LeafBuiler start(long start) {
    return (LeafBuiler) super.start(start);
  }

  @Override
  public LeafBuiler end(long end) {
    return (LeafBuiler) super.end(end);
  }

  @Override
  public LeafBuiler timeType(TimeTypeEnum typeEnum) {
    return (LeafBuiler) super.timeType(typeEnum);
  }

  public LeafBuiler errorHandle(ErrorHandleEnum handleEnum) {
    ((BaseLeaf) this.getNode()).setBaikalErrorHandleEnum(handleEnum);
    return this;
  }

  public LeafBuiler(BaseLeaf leaf) {
    super(leaf);
    if (leaf.getBaikalErrorHandleEnum() == null) {
      leaf.setBaikalErrorHandleEnum(ErrorHandleEnum.SHUT_DOWN);
    }
  }
}
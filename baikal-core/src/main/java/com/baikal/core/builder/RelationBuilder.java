package com.baikal.core.builder;

import com.baikal.common.enums.TimeTypeEnum;
import com.baikal.core.base.BaseNode;
import com.baikal.core.base.BaseRelation;
import com.baikal.core.relation.AllRelation;
import com.baikal.core.relation.AndRelation;
import com.baikal.core.relation.AnyRelation;
import com.baikal.core.relation.NoneRelation;
import com.baikal.core.relation.TrueRelation;

import java.util.Arrays;

/**
 * @author kowalski
 */
public class RelationBuilder extends BaseBuilder {

  public RelationBuilder(BaseRelation relation) {
    super(relation);
  }

  public RelationBuilder(RelationBuilder builder) {
    super(builder.build());
  }

  public static RelationBuilder andRelation() {
    return new RelationBuilder(new AndRelation());
  }

  public static RelationBuilder anyRelation() {
    return new RelationBuilder(new AnyRelation());
  }

  public static RelationBuilder allRelation() {
    return new RelationBuilder(new AllRelation());
  }

  public static RelationBuilder noneRelation() {
    return new RelationBuilder(new NoneRelation());
  }

  public static RelationBuilder trueRelation() {
    return new RelationBuilder(new TrueRelation());
  }

  @Override
  public RelationBuilder forward(BaseNode forward) {
    return (RelationBuilder) super.forward(forward);
  }

  @Override
  public RelationBuilder forward(BaseBuilder builder) {
    return (RelationBuilder) super.forward(builder);
  }

  @Override
  public RelationBuilder start(long start) {
    return (RelationBuilder) super.start(start);
  }

  @Override
  public RelationBuilder end(long end) {
    return (RelationBuilder) super.end(end);
  }

  @Override
  public RelationBuilder timeType(TimeTypeEnum typeEnum) {
    return (RelationBuilder) super.timeType(typeEnum);
  }

  public RelationBuilder son(BaseNode... nodes) {
    ((BaseRelation) this.getNode()).getChildren().addAll(nodes);
    return this;
  }

  public RelationBuilder son(BaseBuilder... builders) {
    BaseNode[] nodes = Arrays.stream(builders).map(BaseBuilder::build).toArray(BaseNode[]::new);
    ((BaseRelation) this.getNode()).getChildren().addAll(nodes);
    return this;
  }
}
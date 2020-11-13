package com.baikal.test.config;

import com.baikal.client.config.BaikalSpringBeanFactory;
import com.baikal.core.builder.BaikalBuilder;
import com.baikal.core.builder.LeafBuilder;
import com.baikal.core.builder.RelationBuilder;
import com.baikal.core.utils.BaikalBeanUtils;
import com.baikal.test.flow.ScoreFlow;
import com.baikal.test.result.AmountResult;
import com.baikal.test.result.PointResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author kowalski
 */
@Configuration
public class TestConfig {

  @Bean
  public BaikalBeanUtils.BaikalBeanFactory baikalBeanFactory() {
    return new BaikalSpringBeanFactory();
  }

  @PostConstruct
  public void baikalBuild() {
    baikalBeanFactory();
    AmountResult amountResult = new AmountResult();
    amountResult.setKey("uid");
    amountResult.setValue(5.0);
    BaikalBeanUtils.autowireBean(amountResult);
    PointResult pointResult = new PointResult();
    pointResult.setKey("uid");
    pointResult.setValue(10.0);
    BaikalBeanUtils.autowireBean(pointResult);
    ScoreFlow s100 = new ScoreFlow();
    s100.setKey("spend");
    s100.setScore(100.0);
    ScoreFlow s50 = new ScoreFlow();
    s100.setKey("spend");
    s100.setScore(50.0);

    RelationBuilder amountAnd = RelationBuilder.andRelation().son(LeafBuilder.leaf(s100), LeafBuilder.leaf(amountResult));
    RelationBuilder pointAnd = RelationBuilder.andRelation().son(LeafBuilder.leaf(s50), LeafBuilder.leaf(pointResult));
    RelationBuilder rootAny = RelationBuilder.anyRelation().son(amountAnd, pointAnd);
    BaikalBuilder.root(rootAny).debug((byte) 7).register("recharge-1");
  }
}

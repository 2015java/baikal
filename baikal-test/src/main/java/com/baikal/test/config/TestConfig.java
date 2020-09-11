package com.baikal.test.config;

import com.baikal.core.builder.BaikalBuilder;
import com.baikal.core.builder.LeafBuiler;
import com.baikal.core.builder.RelationBuilder;
import com.baikal.test.flow.TestFlow;
import com.baikal.test.flow.SetFlow;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * @author kowalski
 */
@Configuration
public class TestConfig {

  @PostConstruct
  public void baikalBuild() {
    BaikalBuilder.root(RelationBuilder.andRelation().forward(LeafBuiler.leaf(new SetFlow()).inverse())
        .son(LeafBuiler.leaf(new TestFlow()))).debug((byte) 7).register("test");
  }
}

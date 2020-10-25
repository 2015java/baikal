package com.baikal.server.config;

import com.baikal.common.constant.Constant;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kowalski
 */
@Configuration
public class RabbitMqConfig {
  @Bean
  public DirectExchange updateExchange() {
    return new DirectExchange(Constant.getUpdateExchange());
  }

  @Bean
  public DirectExchange showConfExchange() {
    return new DirectExchange(Constant.getShowConfExchange());
  }
}

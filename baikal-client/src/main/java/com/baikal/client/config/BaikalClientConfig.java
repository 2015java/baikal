package com.baikal.client.config;

import com.baikal.client.listener.BaikalMockListener;
import com.baikal.client.listener.BaikalShowConfListener;
import com.baikal.client.listener.BaikalUpdateListener;
import com.baikal.common.constant.Constant;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kowalski
 */
@Configuration
public class BaikalClientConfig {

  @Value("${baikal.rabbit.host:}")
  private String host;
  @Value("${baikal.rabbit.username:}")
  private String username;
  @Value("${baikal.rabbit.password:}")
  private String password;
  @Value("${baikal.rabbit.port:}")
  private Integer port;

  @Value("${baikal.app:}")
  private Integer app;
  /**
   * 等待初始化返回时间 默认10s
   */
  @Value("${baikal.init.reply.timeout:10000}")
  private int timeout;

  @Bean(name = "baikalConnectionFactory")
  public ConnectionFactory baikalConnectionFactory() {
    CachingConnectionFactory baikalConnectionFactory = new CachingConnectionFactory();
    baikalConnectionFactory.setUsername(username);
    baikalConnectionFactory.setPassword(password);
    baikalConnectionFactory.setHost(host);
    baikalConnectionFactory.setPort(port);
    return baikalConnectionFactory;
  }

  @Bean(name = "baikalUpdateQueue")
  public Queue baikalUpdateQueue() {
    return QueueBuilder.nonDurable(Constant.genUpdateTmpQueue()).exclusive().autoDelete().build();
  }

  @Bean(name = "baikalUpdateExchange")
  public DirectExchange baikalUpdateExchange() {
    return new DirectExchange(Constant.getUpdateExchange());
  }

  @Bean("baikalUpdateBinding")
  public Binding baikalUpdateBinding(
      @Qualifier("baikalUpdateQueue") Queue baikalUpdateQueue,
      @Qualifier("baikalUpdateExchange") DirectExchange baikalUpdateExchange) {
    return BindingBuilder.bind(baikalUpdateQueue).to(baikalUpdateExchange).with(Constant.getUpdateRoutetKey(app));
  }

  @Bean("baikalUpdateMessageContainer")
  public SimpleMessageListenerContainer baikalUpdateMessageContainer(
      @Qualifier("baikalUpdateQueue") Queue baikalUpdateQueue,
      @Qualifier("baikalConnectionFactory") ConnectionFactory baikalConnectionFactory,
      @Qualifier("baikalRabbitTemplate") RabbitTemplate baikalRabbitTemplate) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(baikalConnectionFactory);
    container.setQueues(baikalUpdateQueue);
    container.setExposeListenerChannel(true);
    container.setPrefetchCount(1);
    container.setConcurrentConsumers(1);
    container.setAcknowledgeMode(AcknowledgeMode.NONE);
    /**版本兼容*/
    container.setMessageListener((Object) new BaikalUpdateListener());
    return container;
  }

  @Bean(name = "baikalShowConfQueue")
  public Queue baikalShowConfQueue() {
    return QueueBuilder.nonDurable(Constant.getShowConfQueue(app)).autoDelete().build();
  }

  @Bean(name = "baikalShowConfExchange")
  public DirectExchange baikalShowConfExchange() {
    return new DirectExchange(Constant.getShowConfExchange());
  }

  @Bean("baikalShowConfBinding")
  public Binding baikalShowConfBinding(
      @Qualifier("baikalShowConfQueue") Queue baikalShowConfQueue,
      @Qualifier("baikalShowConfExchange") DirectExchange baikalShowConfExchange) {
    return BindingBuilder.bind(baikalShowConfQueue).to(baikalShowConfExchange).with(String.valueOf(app));
  }

  @Bean("baikalShowConfMessageContainer")
  public SimpleMessageListenerContainer baikalShowConfMessageContainer(
      @Qualifier("baikalShowConfQueue") Queue baikalShowConfQueue,
      @Qualifier("baikalConnectionFactory") ConnectionFactory baikalConnectionFactory,
      @Qualifier("baikalRabbitTemplate") RabbitTemplate baikalRabbitTemplate) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(baikalConnectionFactory);
    container.setQueues(baikalShowConfQueue);
    container.setExposeListenerChannel(true);
    container.setPrefetchCount(1);
    container.setConcurrentConsumers(1);
    container.setAcknowledgeMode(AcknowledgeMode.NONE);
    /**版本兼容*/
    container.setMessageListener((Object) new BaikalShowConfListener(app, baikalRabbitTemplate));
    return container;
  }

  @Bean(name = "baikalMockQueue")
  public Queue baikalMockQueue() {
    return QueueBuilder.nonDurable(Constant.getMockQueue(app)).autoDelete().build();
  }

  @Bean(name = "baikalMockExchange")
  public DirectExchange baikalMockExchange() {
    return new DirectExchange(Constant.getMockExchange());
  }

  @Bean("baikalMockBinding")
  public Binding baikalMockBinding(
      @Qualifier("baikalMockQueue") Queue baikalMockQueue,
      @Qualifier("baikalMockExchange") DirectExchange baikalMockExchange) {
    return BindingBuilder.bind(baikalMockQueue).to(baikalMockExchange).with(String.valueOf(app));
  }

  @Bean
  public BaikalMockListener baikalMockListener() {
    return new BaikalMockListener();
  }

  @Bean("baikalMockMessageContainer")
  public SimpleMessageListenerContainer baikalMockMessageContainer(
      @Qualifier("baikalMockQueue") Queue baikalMockQueue,
      @Qualifier("baikalConnectionFactory") ConnectionFactory baikalConnectionFactory) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(baikalConnectionFactory);
    container.setQueues(baikalMockQueue);
    container.setExposeListenerChannel(true);
    container.setPrefetchCount(1);
    container.setConcurrentConsumers(1);
    container.setAcknowledgeMode(AcknowledgeMode.NONE);
    /*版本兼容*/
    container.setMessageListener((Object) baikalMockListener());
    return container;
  }

  @Bean(name = "baikalRabbitTemplate")
  public RabbitTemplate baikalRabbitTemplate(@Qualifier("baikalConnectionFactory") ConnectionFactory baikalConnectionFactory) {
    RabbitTemplate baikalRabbitTemplate = new RabbitTemplate(baikalConnectionFactory);
    baikalRabbitTemplate.setReplyTimeout(timeout);
    return baikalRabbitTemplate;
  }

  @Bean(name = "baikalAmqpAdmin")
  public AmqpAdmin baikalAmqpAdmin(@Qualifier("baikalConnectionFactory") ConnectionFactory baikalConnectionFactory) {
    return new RabbitAdmin(baikalConnectionFactory);
  }
}

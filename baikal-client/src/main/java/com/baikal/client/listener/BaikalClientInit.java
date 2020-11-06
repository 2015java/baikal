package com.baikal.client.listener;

import com.alibaba.fastjson.JSON;
import com.baikal.common.constant.Constant;
import com.baikal.common.exception.BaikalException;
import com.baikal.common.model.BaikalTransferDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

/**
 * @author kowalski
 */
@Slf4j
@Service
@DependsOn("baikalBeanFactory")
public final class BaikalClientInit implements InitializingBean {

  @Value("${baikal.app}")
  private Integer app;

  @Resource(name = "baikalAmqpAdmin")
  private AmqpAdmin baikalAmqpAdmin;

  @Resource(name = "baikalRabbitTemplate")
  private RabbitTemplate baikalRabbitTemplate;

  /**
   * 避免初始化与更新之间存在遗漏更新消息,此处先保证mq初始化完毕
   * 初始化baikal通过restTemplate远程调用server链接完成
   */
  @Override
  public void afterPropertiesSet() {
    log.info("baikal client init baikalStart");

    Object obj = baikalRabbitTemplate.convertSendAndReceive(Constant.getInitExchange(), "", String.valueOf(app));
    String json = (String) obj;
    if (!StringUtils.isEmpty(json)) {
      BaikalTransferDto infoDto = JSON.parseObject(json, BaikalTransferDto.class);
      log.info("baikal client init content:{}", JSON.toJSONString(infoDto));
      BaikalUpdate.update(infoDto);
      log.info("baikal client init baikalEnd success");
      BaikalUpdateListener.initEnd(infoDto.getVersion());
      return;
    }
    throw new BaikalException("baikal init error maybe server is down app:" + app);
  }
}

package com.baikal.server;

import com.baikal.server.service.BaikalServerService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author kowalski
 * client在启动时向server发送请求init消息处理
 */
@Slf4j
@Component
public class BaikalInitHandler {

  private final BaikalServerService serverService;

  @Contract(pure = true)
  public BaikalInitHandler(BaikalServerService serverService) {
    this.serverService = serverService;
  }

  @RabbitListener(bindings = @QueueBinding(
      exchange = @Exchange("#{T(com.baikal.common.constant.Constant).getInitExchange()}"),
      value = @Queue(value = "baikal.init.queue",
          durable = "true")))
  public String processMessage(Message message) {
    if (message.getBody() != null && message.getBody().length > 0) {
      String appStr = new String(message.getBody());
      Integer app = Integer.valueOf(appStr);
      return serverService.getInitJson(app);
    }
    return "";
  }
}

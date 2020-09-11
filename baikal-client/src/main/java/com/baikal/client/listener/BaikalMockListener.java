package com.baikal.client.listener;

import com.alibaba.fastjson.JSON;
import com.baikal.client.BaikalClient;
import com.baikal.core.context.BaikalPack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

/**
 * @author kowalski
 * mock信息
 */
@Slf4j
public class BaikalMockListener implements MessageListener {

  @Override
  public void onMessage(Message message) {
    if (message.getBody() != null && message.getBody().length > 0) {
      String json = new String(message.getBody());
      BaikalPack pack = JSON.parseObject(json, BaikalPack.class);
      BaikalClient.process(pack);
    }
  }
}

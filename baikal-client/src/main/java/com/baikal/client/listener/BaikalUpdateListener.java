package com.baikal.client.listener;

import com.alibaba.fastjson.JSON;
import com.baikal.common.model.BaikalTransferDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kowalski
 */
@Slf4j
public final class BaikalUpdateListener implements MessageListener {

  private static volatile boolean waitInit = true;

  private static volatile long initVersion;

  private List<Message> waitMessageList = new ArrayList<>();

  @Override
  public void onMessage(Message message) {
    try {
      if (waitInit) {
        log.info("wait init message:{}", JSON.toJSONString(message));
        waitMessageList.add(message);
        return;
      }
      if (!CollectionUtils.isEmpty(waitMessageList)) {
        for (Message waitMessage : waitMessageList) {
          handleBeforeInitMessage(waitMessage);
        }
        waitMessageList = null;
      }
      handleMessage(message);
    } catch (Exception e) {
      log.error("baikal listener update error message:{} e:", JSON.toJSONString(message), e);
    }
  }

  private void handleBeforeInitMessage(Message message) {
    String json = new String(message.getBody());
    BaikalTransferDto baikalInfo = JSON.parseObject(json, BaikalTransferDto.class);
    if (baikalInfo.getVersion() > initVersion) {
      /*一旦后面有出现比initVersion大的version 将initVersion置为-1 防止server端重启导致version从0开始*/
      log.info("baikal listener update wait msg baikalStart baikalInfo:{}", json);
      BaikalUpdate.update(baikalInfo);
      log.info("baikal listener update wait msg baikalEnd success");
      return;
    }
    log.info("baikal listener msg version low then init version:{}, msg:{}", initVersion, JSON.toJSONString(baikalInfo));
  }

  private void handleMessage(Message message) {
    String json = new String(message.getBody());
    BaikalTransferDto baikalInfo = JSON.parseObject(json, BaikalTransferDto.class);
    log.info("baikal listener update msg baikalStart baikalInfo:{}", json);
    BaikalUpdate.update(baikalInfo);
    log.info("baikal listener update msg baikalEnd success");
  }

  public static void initEnd(long version) {
    waitInit = false;
    initVersion = version;
  }
}

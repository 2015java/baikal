package com.baikal.client.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import com.baikal.common.codec.BaikalLongCodec;
import com.baikal.common.utils.AddressUtils;
import com.baikal.core.base.BaseNode;
import com.baikal.core.base.BaseRelation;
import com.baikal.core.cache.BaikalConfCache;
import com.baikal.core.cache.BaikalHandlerCache;
import com.baikal.core.handler.BaikalHandler;
import com.baikal.core.utils.BaikalBeanUtils;
import com.baikal.core.utils.BaikalLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kowalski.zhang on 2020-03-17
 *
 * @author kowalski
 */
@Slf4j
public class BaikalShowConfListener implements MessageListener {

  private static final SerializeConfig FAST_JSON_CONFIG;
  private static final SpringBeanAndForwardFilter SPRING_BEAN_FILTER = new SpringBeanAndForwardFilter();

  static {
    FAST_JSON_CONFIG = new SerializeConfig();
    FAST_JSON_CONFIG.put(Long.class, BaikalLongCodec.getInstance());
  }

  private final RabbitTemplate baikalRabbitTemplate;
  private Integer app;
  private String address;
  private MessageConverter messageConverter = new SimpleMessageConverter();

  public BaikalShowConfListener(RabbitTemplate baikalRabbitTemplate, MessageConverter messageConverter) {
    this.baikalRabbitTemplate = baikalRabbitTemplate;
    this.messageConverter = messageConverter;
  }

  public BaikalShowConfListener(RabbitTemplate baikalRabbitTemplate) {
    this.baikalRabbitTemplate = baikalRabbitTemplate;
  }

  public BaikalShowConfListener(Integer app, RabbitTemplate baikalRabbitTemplate) {
    this.app = app;
    this.baikalRabbitTemplate = baikalRabbitTemplate;
  }

  private String getAddress() {
    address = address == null ? AddressUtils.getAddressPort() : address;
    return address;
  }

  @Override
  public void onMessage(Message message) {
    Address replyToAddress = message.getMessageProperties().getReplyToAddress();
    if (replyToAddress == null) {
      throw new AmqpRejectAndDontRequeueException("No replyToAddress in inbound AMQP Message");
    }
    if (message.getBody() != null && message.getBody().length > 0) {
      Map<Object, Object> resMap = new HashMap<>();
      resMap.put("ip", getAddress());
      String baikalIdStr = new String(message.getBody());
      Long baikalId = Long.valueOf(baikalIdStr);
      resMap.put("baikalId", baikalId);
      resMap.put("app", app);
      if (baikalId <= 0) {
        resMap.put("handlerMap", BaikalHandlerCache.getIdHandlerMap());
        resMap.put("confMap", BaikalConfCache.getConfMap());
      } else {
        BaikalHandler handler = BaikalHandlerCache.getHandlerById(baikalId);
        if (handler != null) {
          Map<Object, Object> handlerMap = new HashMap<>();
          handlerMap.put("baikalId", handler.findBaikalId());
          handlerMap.put("scenes", handler.getScenes());
          handlerMap.put("debug", handler.getDebug());
          handlerMap.put("start", handler.getStart());
          handlerMap.put("end", handler.getEnd());
          handlerMap.put("timeTypeEnum", handler.getTimeTypeEnum());
          BaseNode root = handler.getRoot();
          if (root != null) {
            handlerMap.put("root", assembleNode(root));
          }
          resMap.put("handler", handlerMap);
        }
      }
      send(JSON.toJSONString(resMap, FAST_JSON_CONFIG, SerializerFeature.DisableCircularReferenceDetect),
          replyToAddress);
    } else {
      send("", replyToAddress);
    }
  }

  @SuppressWarnings("unchecked")
  private Map assembleNode(BaseNode node) {
    if (node == null) {
      return null;
    }
    Map map = new HashMap<>();
    if (node instanceof BaseRelation) {
      BaseRelation relation = (BaseRelation) node;
      BaikalLinkedList<BaseNode> children = relation.getChildren();
      if (children != null && !children.isEmpty()) {
        List<Map> showChildren = new ArrayList<>(children.getSize());
        for (BaikalLinkedList.Node<BaseNode> listNode = children.getFirst();
             listNode != null; listNode = listNode.next) {
          BaseNode child = listNode.item;
          Map childMap = assembleNode(child);
          if (childMap != null) {
            showChildren.add(childMap);
          }
        }
        map.put("children", showChildren);
      }
      BaseNode foward = relation.getBaikalForward();
      if (foward != null) {
        Map forwardMap = assembleNode(foward);
        if (forwardMap != null) {
          map.put("baikalForward", forwardMap);
        }
      }
      map.put("baikalNodeId", relation.getBaikalNodeId());
      map.put("baikalTimeTypeEnum", relation.getBaikalTimeTypeEnum());
      map.put("baikalStart", relation.getBaikalStart());
      map.put("baikalEnd", relation.getBaikalEnd());
      map.put("baikalNodeDebug", relation.isBaikalNodeDebug());
      map.put("baikalInverse", relation.isBaikalInverse());
    } else {
      map = JSON.parseObject(JSON.toJSONString(node, FAST_JSON_CONFIG, SPRING_BEAN_FILTER,
          SerializerFeature.DisableCircularReferenceDetect), Map.class);
      BaseNode forward = node.getBaikalForward();
      if (forward != null) {
        Map forwardMap = assembleNode(forward);
        if (forwardMap != null) {
          map.put("baikalForward", forwardMap);
        }
      }
    }
    return map;
  }

  private void send(Object object, Address replyToAddress) {
    Message message = this.messageConverter.toMessage(object, new MessageProperties());
    baikalRabbitTemplate.send(replyToAddress.getExchangeName(), replyToAddress.getRoutingKey(), message);
  }

  private static final class SpringBeanAndForwardFilter extends SimplePropertyPreFilter {
    @Override
    public boolean apply(JSONSerializer serializer, Object source, String name) {
      if ("baikalForward".equals(name)) {
        return false;
      }
      return !BaikalBeanUtils.containsBean(name);
    }
  }
}

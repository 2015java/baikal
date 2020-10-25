package com.baikal.server.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baikal.common.constant.Constant;
import com.baikal.core.context.BaikalPack;
import com.baikal.server.model.WebResult;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author kowalski
 */

@RestController
public class BaikalMockController {

  @Resource
  private AmqpTemplate amqpTemplate;

  @RequestMapping(value = "/baikal/amqp/mock", method = RequestMethod.POST)
  public WebResult amqpMock(@RequestParam Integer app, @RequestBody BaikalPack pack) {
    if (app <= 0 || pack == null) {
      return new WebResult<>(-1, "参数不正确", null);
    }
    if (pack.getBaikalId() <= 0 && StringUtils.isEmpty(pack.getScene())) {
      return new WebResult<>(-1, "BaikalId和Scene不能同时为空", null);
    }
    amqpTemplate.convertAndSend(Constant.getMockExchange(), String.valueOf(app),
        JSON.toJSONString(pack, SerializerFeature.WriteClassName));
    return new WebResult<>();
  }

  @RequestMapping(value = "/baikal/amqp/mocks", method = RequestMethod.POST)
  public WebResult amqpMocks(@RequestParam Integer app, @RequestBody List<BaikalPack> packs) {
    if (app <= 0 || CollectionUtils.isEmpty(packs)) {
      return new WebResult<>(-1, "参数不正确", null);
    }
    WebResult<List<BaikalPack>> result = new WebResult<>();
    List<BaikalPack> errPacks = new ArrayList<>();
    for (BaikalPack pack : packs) {
      if (pack.getBaikalId() <= 0 && StringUtils.isEmpty(pack.getScene())) {
        errPacks.add(pack);
        continue;
      }
      amqpTemplate.convertAndSend(Constant.getMockExchange(), String.valueOf(app),
          JSON.toJSONString(pack, SerializerFeature.WriteClassName));
    }
    result.setData(errPacks);
    return result;
  }
}

package com.baikal.test.controller;

import com.alibaba.fastjson.JSON;
import com.baikal.client.BaikalClient;
import com.baikal.core.context.BaikalPack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author kowalski
 */
@Slf4j
@RestController
public class TestController {

  @RequestMapping(value = "/test", method = RequestMethod.POST)
  public String test(@RequestBody Map map) {
    BaikalPack pack = JSON.parseObject(JSON.toJSONString(map), BaikalPack.class);
    return JSON.toJSONString(BaikalClient.processCxt(pack));
  }
}

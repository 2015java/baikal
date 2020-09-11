package com.baikal.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by kowalski.zhang on 2018/9/6
 * @author kowalski
 */
@RestController
public class WebController {

  @RequestMapping("/hello")
  public String hello(){
    return "Hello World~";
  }
}

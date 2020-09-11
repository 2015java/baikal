package com.baikal.server.model;

import lombok.Data;

/**
 * @author kowalski
 */
@Data
public class WebResult<T> {
  private int ret;
  private String msg;
  private T data;

  public WebResult() {
    this.ret = 0;
  }

  public WebResult(T data) {
    this.ret = 0;
    this.data = data;
  }

  public WebResult(int ret, T data) {
    this.ret = ret;
    this.data = data;
  }

  public WebResult(int ret, String msg, T data) {
    this.ret = ret;
    this.msg = msg;
    this.data = data;
  }
}

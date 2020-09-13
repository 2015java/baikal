package com.baikal.common.constant;

import com.baikal.common.utils.AddressUtils;

/**
 * @author kowalski
 * 常量
 */
public final class Constant {

  private Constant() {
  }

  public static String getUpdateExchange() {
    return "baikal.update.exchange";
  }

  public static String getShowConfExchange() {
    return "baikal.show.conf.exchange";
  }

  public static String getMockExchange() {
    return "baikal.mock.exchange";
  }

  public static String getInitExchange() {
    return "baikal.init.exchange";
  }

  public static String getUpdateRoutetKey(Integer app) {
    return "baikal.update." + app;
  }

  public static String getShowConfQueue(Integer app) {
    return "baikal.show.conf." + app;
  }

  public static String getMockQueue(Integer app) {
    return "baikal.mock." + app;
  }

  public static String genUpdateTmpQueue() {
    /*"baikal.tmp.queue-" + host + ":" + port*/
    return "baikal.tmp.queue-" + AddressUtils.getAddressPort();
  }
}

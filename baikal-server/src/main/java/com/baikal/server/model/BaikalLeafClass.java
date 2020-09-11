package com.baikal.server.model;

import lombok.Data;

/**
 * @author kowalski
 */
@Data
public class BaikalLeafClass {

  private String shortName;
  private String fullName;
  private int count;

  public int sortNegativeCount() {
    return -count;
  }
}

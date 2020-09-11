package com.baikal.common.model;

import lombok.Data;

/**
 * @author kowalski
 */
@Data
public final class BaikalBaseDto {

  private Long id;

  private String scenes;

  private Byte status;

  private Long confId;

  private Byte timeType;

  private Long start;

  private Long end;

  private Byte debug;

  private Integer priority;
}

package com.baikal.common.model;

import lombok.Data;

/**
 * @author kowalski
 */
@Data
public final class BaikalConfDto {

  private Long id;

  private String sonIds;

  private Byte type;

  private Byte status;

  private String confName;

  private String confField;

  private Byte timeType;

  private Long start;

  private Long end;

  private Long forwardId;

  private Integer complex;

  private Byte debug;

  private Byte inverse;
}

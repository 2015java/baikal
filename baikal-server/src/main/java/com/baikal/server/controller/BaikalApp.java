package com.baikal.server.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author kowalski
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaikalApp {
  private Integer app;
  private String appName;
}

package com.baikal.server.model;

import com.baikal.dao.model.BaikalBase;
import com.baikal.dao.model.BaikalConf;
import lombok.Data;

import java.util.List;

/**
 * @author kowalski
 */
@Data
public class PushData {

  private BaikalBase base;

  private List<BaikalConf> confs;
}

package com.baikal.test.none;

import com.baikal.core.context.BaikalPack;
import com.baikal.core.leaf.pack.BaseLeafPackNone;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

/**
 * @author kowalski
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class TimeChangeNone extends BaseLeafPackNone {

  private Date time;

  private long cursorMills;

  @Value("${environment}")
  private String environment;

  /**
   * 叶子节点处理
   *
   * @param pack
   * @return
   */
  @Override
  protected void doPackNone(BaikalPack pack) {
    if(!"product".equals(environment)) {
      if (time != null) {
        pack.setRequestTime(time.getTime());
      } else {
        pack.setRequestTime(pack.getRequestTime() + cursorMills);
      }
    }
  }
}

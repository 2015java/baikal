package com.baikal.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author kowalski
 */
@Data
public final class BaikalTransferDto {

  private long version;

  private List<BaikalConfDto> insertOrUpdateConfs;

  private List<Long> deleteConfIds;

  private List<BaikalBaseDto> insertOrUpdateBases;

  private List<Long> deleteBaseIds;
}

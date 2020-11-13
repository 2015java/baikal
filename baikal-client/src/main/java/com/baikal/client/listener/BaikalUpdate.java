package com.baikal.client.listener;

import com.baikal.common.model.BaikalTransferDto;
import com.baikal.core.cache.BaikalConfCache;
import com.baikal.core.cache.BaikalHandlerCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author kowalski
 * 更新Client端本地缓存
 */
@Slf4j
public final class BaikalUpdate {

  public static void update(BaikalTransferDto info) {
    /*优先conf*/
    if (!CollectionUtils.isEmpty(info.getDeleteConfIds())) {
      BaikalConfCache.delete(info.getDeleteConfIds());
    }
    if (!CollectionUtils.isEmpty(info.getInsertOrUpdateConfs())) {
      BaikalConfCache.insertOrUpdate(info.getInsertOrUpdateConfs());
    }
    /*其次handler*/
    if (!CollectionUtils.isEmpty(info.getDeleteBaseIds())) {
      BaikalHandlerCache.delete(info.getDeleteBaseIds());
    }
    if (!CollectionUtils.isEmpty(info.getInsertOrUpdateBases())) {
      BaikalHandlerCache.insertOrUpdate(info.getInsertOrUpdateBases());
    }
  }
}

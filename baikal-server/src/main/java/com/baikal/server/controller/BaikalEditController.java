package com.baikal.server.controller;

import com.alibaba.fastjson.JSON;
import com.baikal.server.model.BaikalConfVo;
import com.github.kevinsawicki.http.HttpRequest;
import com.baikal.server.model.BaikalBaseVo;
import com.baikal.server.model.WebResult;
import com.baikal.server.service.BaikalEditService;
import com.baikal.server.service.BaikalServerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * @author kowalski
 */

@RestController
public class BaikalEditController {

  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Resource
  private BaikalEditService editService;

  @Resource
  private BaikalServerService serverService;

  @Value("${environment.id}")
  private String environmentId;

  /**
   * 编辑baikal
   */
  @RequestMapping(value = "/baikal/edit", method = RequestMethod.POST)
  public WebResult eidtBase(@RequestBody BaikalBaseVo baseVo) {
    WebResult result = editService.editBase(baseVo);
    serverService.updateByEdit();
    return result;
  }

  /**
   * 编辑节点
   */
  @RequestMapping(value = "/baikal/conf/edit", method = RequestMethod.POST)
  public WebResult eidtConf(@RequestBody BaikalConfVo confVo) {
    WebResult result = editService.editConf(confVo.getApp(), confVo.getType(), confVo.getBaikalId(), confVo);
    serverService.updateByEdit();
    return result;
  }

  /**
   * 获取叶子节点类
   */
  @RequestMapping(value = "/baikal/conf/edit/getClass", method = RequestMethod.GET)
  public WebResult getClass(@RequestParam Integer app, @RequestParam Byte type) {
    return editService.getLeafClass(app, type);
  }

  /**
   * 发布
   */
  @RequestMapping(value = "/baikal/conf/push", method = RequestMethod.POST)
  public WebResult push(@RequestBody Map map) {
    return editService.push((Integer) map.get("app"), Long.parseLong(map.get("baikalId").toString()), (String) map.get("reason"));
  }

  /**
   * 发布到线上
   */
  @RequestMapping(value = "/baikal/topro", method = RequestMethod.POST)
  public WebResult toPro(@RequestBody Map map) {
    WebResult result = new WebResult();
    if(!"1".equals(environmentId)){
      int code = HttpRequest.post("http://127.0.0.1/baikal-server/baikal/conf/import")
              .connectTimeout(5000)
              .readTimeout(5000)
              .header("Content-Type", "application/json; charset=utf-8")
              .send(JSON.toJSONString(map))
              .code();
      result.setMsg(String.valueOf(code));
    }
    return result;
  }

  /**
   * 发布历史
   */
  @RequestMapping(value = "/baikal/conf/push/history", method = RequestMethod.GET)
  public WebResult history(@RequestParam Integer app,
                           @RequestParam Long baikalId) {
    return editService.history(app, baikalId);
  }

  /**
   * 导出
   */
  @RequestMapping(value = "/baikal/conf/export", method = RequestMethod.GET)
  public WebResult exportData(@RequestParam Long baikalId,
                              @RequestParam(defaultValue = "-1") Long pushId) {
    return editService.exportData(baikalId, pushId);
  }

  /**
   * 回滚
   */
  @RequestMapping(value = "/baikal/conf/rollback", method = RequestMethod.POST)
  public WebResult exportData(@RequestParam Long pushId) {
    WebResult result = editService.rollback(pushId);
    serverService.updateByEdit();
    return result;
  }

  /**
   * 导入
   */
  @RequestMapping(value = "/baikal/conf/import", method = RequestMethod.POST)
  public WebResult importData(@RequestBody Map map) {
    WebResult result = editService.importData((String) map.get("data"));
    serverService.updateByEdit();
    return result;
  }

//  /**
//   * 复制
//   */
//  @RequestMapping(value = "/baikal/conf/copy", method = RequestMethod.POST)
//  public WebResult copyData(@RequestBody String data) {
//    WebResult result = null;
//    if (!environmentId.equals("1")) {
//      result = editService.copyData(data);
//      serverService.updateByEdit();
//    }
//    return result;
//  }
}

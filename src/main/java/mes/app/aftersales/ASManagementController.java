package mes.app.aftersales;

import lombok.extern.slf4j.Slf4j;
import mes.app.aftersales.service.ASManagementService;
import mes.domain.entity.bogo.tb_as011;
import mes.domain.entity.bogo.tb_as012;
import mes.domain.model.AjaxResult;
import mes.domain.repository.bogo.tb_as011Repository;
import mes.domain.repository.bogo.tb_as012Repository;
import mes.domain.services.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/definition/ASManagement")
public class ASManagementController {

  @Autowired
  ASManagementService asManagementService;

  @Autowired
  tb_as011Repository as011Repository;

  @Autowired
  tb_as012Repository as012Repository;

  @GetMapping("/read")
  public AjaxResult getList(@RequestParam(value = "txtRegno", required = false)String regno) {

    List<Map<String, Object>> items = this.asManagementService.getList(regno);

    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  @GetMapping("/detail")
  public AjaxResult getDetail(@RequestParam(value = "id") Integer id){

    Map<String, Object> detail = this.asManagementService.getDetail(id);
    AjaxResult result = new AjaxResult();
    result.data = detail;
    return result;

  }

  @PostMapping("/save")
  @Transactional
  public AjaxResult saveData(@RequestBody Map<String, Object> payload) {
    AjaxResult result = new AjaxResult();
    try {
      // 1) 헤더 파싱
      Integer id         = CommonUtil.tryIntNull(payload.get("id"));
      Integer as_id      = CommonUtil.tryIntNull(payload.get("as_id"));
      String  pernm  = CommonUtil.tryString(payload.get("pernm"));
      String  vechidno   = CommonUtil.tryString(payload.get("vechidno"));
      String  regdateStr = CommonUtil.tryString(payload.get("regdate"));
      String  fixdateStr = CommonUtil.tryString(payload.get("fixdate"));
      String  vechregno  = CommonUtil.tryString(payload.get("regno"));
      String mileage    = CommonUtil.tryString(payload.get("mileage"));

      String partamt        = CommonUtil.tryString(payload.get("partamt"));
      String total_workpay  = CommonUtil.tryString(payload.get("total_workpay"));
      String total_amt      = CommonUtil.tryString(payload.get("total_amt"));
      String vat            = CommonUtil.tryString(payload.get("vat"));
      String totamt         = CommonUtil.tryString(payload.get("totamt"));

      Date regdate = CommonUtil.trySqlDate(regdateStr);
      Date fixdate = CommonUtil.trySqlDate(fixdateStr);

      tb_as011 as011 = (id != null)
          ? as011Repository.findById(id).orElseThrow(() -> new IllegalArgumentException("헤더 없음: id=" + id))
          : new tb_as011();

      as011.setAsid(as_id);
      if (regdate != null) as011.setRegdate(regdate);
      if (fixdate != null) as011.setFixdate(fixdate);
      as011.setVechidno(vechidno);
      as011.setVechregno(vechregno);
      as011.setMileage(Integer.valueOf(mileage));
      as011.setPernm(pernm);
      as011.setPartamt(Integer.valueOf(partamt));
      as011.setWorkamt(Integer.valueOf(total_workpay));
      as011.setAmount(Integer.valueOf(total_amt));
      as011.setVamt(Integer.valueOf(vat));
      as011.setTotamt(Integer.valueOf(totamt));

      as011Repository.save(as011);

      as012Repository.deleteByRepid(as011.getId());

      List<Map<String, Object>> items = (List<Map<String, Object>>) payload.getOrDefault("items", java.util.Collections.emptyList());

      if (!items.isEmpty()) {
        List<tb_as012> toSave = new java.util.ArrayList<>(items.size());
        for (Map<String, Object> item : items) {
          Integer partqty  = CommonUtil.tryIntNull(item.get("partqty"));
          Integer uamt     = CommonUtil.tryIntNull(item.get("uamtPrice"));
          Integer amt      = CommonUtil.tryIntNull(item.get("amt"));
          Integer workpay  = CommonUtil.tryIntNull(item.get("workpay"));
          String  fixtext  = CommonUtil.tryString(item.get("fixtext"));
          String  partName = CommonUtil.tryString(item.get("txtProductName"));

          tb_as012 detail = new tb_as012();
          detail.setRepid(as011.getId());
          detail.setPartgroup(partName);
          detail.setFixtext(fixtext);
          detail.setPartqty(partqty);
          detail.setUamt(uamt);
          detail.setTotamt(amt);
          detail.setWorkpay(workpay);

          toSave.add(detail);
        }
        as012Repository.saveAll(toSave);
      }

      result.success = true;
      result.message = (id != null) ? "수정(재저장) 성공" : "등록 성공";
      result.data = java.util.Map.of("headId", as011.getId());
      return result;

    } catch (Exception e) {
      log.error("저장 실패: {}", e.getMessage(), e);
      result.success = false;
      result.message = "저장 실패: " + e.getMessage();
      return result;
    }
  }


  @PostMapping("/delete")
  @Transactional
  public AjaxResult delete(@RequestParam(value = "id") Integer id) {
    AjaxResult result = new AjaxResult();
    try {

      if (!as011Repository.existsById(id)) {
        result.success = false;
        result.message = "해당 데이터가 존재하지 않습니다.";
        return result;
      }

      as012Repository.deleteByRepid(id);

      as011Repository.deleteById(id);

      result.success = true;
      result.message = "삭제되었습니다.";
    } catch (Exception e) {
      result.success = false;
      result.message = "삭제 중 오류가 발생했습니다: " + e.getMessage();
      //log.error(e.getMessage(), e);
    }

    return result;
  }
}

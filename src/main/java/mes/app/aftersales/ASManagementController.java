package mes.app.aftersales;

import lombok.extern.slf4j.Slf4j;
import mes.app.aftersales.service.ASManagementService;
import mes.domain.entity.bogo.tb_as011;
import mes.domain.model.AjaxResult;
import mes.domain.repository.bogo.tb_as011Repository;
import mes.domain.services.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
  public AjaxResult saveData(@RequestBody Map<String, Object> payload){
    AjaxResult result = new AjaxResult();
    try {

      Integer id = CommonUtil.tryIntNull(payload.get("id"));
      Integer as_id = CommonUtil.tryIntNull(payload.get("as_id"));
      String partgroup = CommonUtil.tryString(payload.get("partgroup"));
      String vechidno = CommonUtil.tryString(payload.get("vechidno"));
      String pernm = CommonUtil.tryString(payload.get("pernm"));
      String fixText = CommonUtil.tryString(payload.get("fixText"));
      Integer partqty   = tryInt(payload.get("partqty"));   // ← 핵심 수정
      Integer uamt      = tryInt(payload.get("uamt"));
      Integer totamt    = tryInt(payload.get("totamt"));
      Integer mileage   = tryInt(payload.get("mileage"));
      Integer workpay   = tryInt(payload.get("workpay"));
      String regdate = CommonUtil.tryString(payload.get("regdate"));
      String fixdate = CommonUtil.tryString(payload.get("fixdate"));
      String endflag    = (String) payload.get("endflag");

      tb_as011 as011;

      if(id != null){
        as011 = as011Repository.findById(id).orElseThrow(() ->
        new IllegalArgumentException("해당 ID 데이터가 없습니다. id=" + id));
      }else {
        as011 = new tb_as011();
      }

      as011.setAsid(as_id);
      as011.setRegdate(Date.valueOf(regdate));
      as011.setFixdate(Date.valueOf(fixdate));
      as011.setPartgroup(partgroup);
      as011.setMileage(mileage);
      as011.setVechidno(vechidno);
      as011.setPartqty(partqty);
      as011.setUamt(uamt);
      as011.setTotamt(totamt);
      as011.setWorkpay(workpay);
      as011.setFixtext(fixText);
      as011.setPernm(pernm);
      as011.setEndflag(endflag);

      as011Repository.save(as011);

      result.success = true;
      result.message = (id != null) ? "수정 성공" : "등록 성공";

    }catch (Exception e) {
      log.error("저장 실패: {}", e.getMessage(), e); // 전체 스택 로그 남김
      result.success = false;
      result.message = "저장 실패: " + e.getMessage();
    }
    return result;
  }
  private static Integer tryInt(Object v) {
    if (v == null) return null;
    // 숫자/마이너스 외 제거: "1,000 km" → "1000"
    String s = v.toString().replaceAll("[^0-9-]", "");
    if (s.isEmpty() || s.equals("-")) return null;
    try {
      return Integer.valueOf(s);
    } catch (NumberFormatException e) {
      // 필요시 로깅 후 null 또는 예외 재던지기
      return null;
    }
  }

  @PostMapping("/delete")
  public AjaxResult delete(@RequestParam(value = "id") Integer id) {
    AjaxResult result = new AjaxResult();
    try {

      if (!as011Repository.existsById(id)) {
        result.success = false;
        result.message = "해당 데이터가 존재하지 않습니다.";
        return result;
      }

      as011Repository.deleteById(id);

      result.success = true;
      result.message = "삭제되었습니다.";
    } catch (Exception e) {
      result.success = false;
      result.message = "삭제 중 오류가 발생했습니다: " + e.getMessage();
    }

    return result;
  }
}

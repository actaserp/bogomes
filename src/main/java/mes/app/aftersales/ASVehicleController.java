package mes.app.aftersales;

import lombok.extern.slf4j.Slf4j;
import mes.app.aftersales.service.ASVehicleService;
import mes.domain.entity.bogo.tb_as010;
import mes.domain.model.AjaxResult;
import mes.domain.repository.bogo.tb_as010Repository;
import mes.domain.services.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/definition/ASVehicle")
public class ASVehicleController {

  @Autowired
  ASVehicleService asVehicleService;
  @Autowired
  tb_as010Repository as010Repository;


  @GetMapping("/read")
  public AjaxResult getList(@RequestParam(value = "spcmngno", required = false) Integer spcmngno,
                            @RequestParam(value = "vechidno", required = false) String vechidno) {

    List<Map<String, Object>> items = this.asVehicleService.getList(spcmngno, vechidno);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

  @GetMapping("/detail")
  public AjaxResult getDetail(@RequestParam(value = "id") Integer id) {

    Map<String, Object> detail = this.asVehicleService.getDetail(id);
    AjaxResult result = new AjaxResult();
    result.data = detail;
    return result;
  }

  @PostMapping("/delete")
  public AjaxResult delete(@RequestParam(value = "id") Integer id) {
    AjaxResult result = new AjaxResult();

    try {
      if (!as010Repository.existsById(id)) {
        result.success = false;
        result.message = "해당 데이터가 존재하지 않습니다.";
        return result;
      }

      as010Repository.deleteById(id);

      result.success = true;
      result.message = "삭제되었습니다.";
    } catch (Exception e) {
      result.success = false;
      result.message = "삭제 중 오류가 발생했습니다: " + e.getMessage();
    }

    return result;
  }

  @PostMapping("/save")
  @Transactional
  public AjaxResult save(@RequestBody Map<String, Object> payload) {
    AjaxResult result = new AjaxResult();
    try {
      // 1. 파라미터 추출
      Integer id = CommonUtil.tryIntNull(payload.get("id"));
      String spcmngno   = (String) payload.get("spcmngno");
      String vechidno   = (String) payload.get("vechidno");
      String itemCode   = (String) payload.get("Material_id");
      String owner      = (String) payload.get("OWNER");
      String pernm      = (String) payload.get("PERNM");
      String inputDate  = (String) payload.get("inputDate");
      String outDate    = (String) payload.get("outDate");
      String endflag    = (String) payload.get("endflag"); // 0/1
      String fixText    = (String) payload.get("fixText");

      // 2. 신규/수정 분기
      tb_as010 as010;

      if (id != null) {
        as010 = as010Repository.findById(id).orElseThrow(() ->
            new IllegalArgumentException("해당 ID 데이터가 없습니다. id=" + id));
      } else {
        // 신규: 빈 엔티티 생성
        as010 = new tb_as010();
      }

      // 3. 데이터 매핑
      as010.setSpcmngno(spcmngno);
      as010.setVechidno(vechidno);
      as010.setItemcode(itemCode);
      as010.setOwner(owner);
      as010.setPernm(pernm);
      as010.setInputdate(CommonUtil.trySqlDate(inputDate));
      as010.setOutdate(CommonUtil.trySqlDate(outDate));
      as010.setEndflag(endflag);
      as010.setFixtext(fixText);

      // 4. 저장 (신규든 수정이든 save() 한 번으로 처리 가능)
      as010Repository.save(as010);

      result.success = true;
      result.message = (id != null) ? "수정 성공" : "등록 성공";

    } catch (Exception e) {
      result.success = false;
      result.message = "저장 실패: " + e.getMessage();
    }
    return result;
  }

}

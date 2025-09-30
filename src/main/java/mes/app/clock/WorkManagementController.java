package mes.app.clock;

import lombok.extern.slf4j.Slf4j;
import mes.app.clock.service.WorkManagementService;
import mes.domain.entity.User;
import mes.domain.entity.commute.TB_PB201;
import mes.domain.entity.commute.TB_PB201_PK;
import mes.domain.model.AjaxResult;
import mes.domain.repository.commute.TB_PB201Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/clock/work_management")
public class WorkManagementController {

  @Autowired
  WorkManagementService workManagementService;

  @Autowired
  TB_PB201Repository tbPB201Repository;

  @GetMapping("/read")
  public AjaxResult getWorkManagementList(@RequestParam(value = "depart" ,required = false) String depart,
                                          @RequestParam(value = "SearchDate") String searchDate) {

    LocalDate startDate = LocalDate.parse(searchDate + "-01");
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth()); // 월의 마지막 일
    String start = startDate.toString();
    String end = endDate.toString();

    List<Map<String, Object>> items = this.workManagementService.getWorkManagementList(start,end,depart);

    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }
  @GetMapping("/defects")
  public AjaxResult defectsList (@RequestParam(value = "InspectionDate" ,required = false) String InspectionDate,
                                 @RequestParam(value = "person_id",required = false) Integer person_id) {

    List<Map<String, Object>> items = this.workManagementService.defectsList(InspectionDate, person_id);
    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

  @PostMapping("/save")
  public AjaxResult saveManage(
          @RequestParam(value = "id", required = false) Long id,  // hidden pk
          @RequestParam(value = "InspectionDate", required = false) String inspectionDate,
          @RequestParam(value = "person_id", required = false) Integer personId,
          @RequestParam(value = "saveStartTime", required = false) String saveStartTime,
          @RequestParam(value = "saveEndTime", required = false) String saveEndTime,
          @RequestParam(value = "workType", required = false) String workCode,
          Authentication auth) {

    AjaxResult result = new AjaxResult();
    User user = (User) auth.getPrincipal();

    TB_PB201 entity;

    // 🔹 UPDATE: id 있으면 기존 데이터 불러오기
    if (id != null) {
      entity = tbPB201Repository.findById(id).orElse(new TB_PB201());
    } else {
      entity = new TB_PB201();
    }

    entity.setSpjangcd(user.getSpjangcd());
    entity.setWorkym(inspectionDate.replace("-", "").substring(0, 6));
    entity.setWorkday(inspectionDate.replace("-", "").substring(6, 8));
    entity.setPersonid(personId);
    entity.setStarttime(saveStartTime);
    entity.setEndtime(saveEndTime);
    entity.setWorkcd(workCode);

    // 근무시간 계산
    if (saveStartTime != null && saveEndTime != null) {
      try {
        LocalTime startTime = LocalTime.parse(saveStartTime);
        LocalTime endTime   = LocalTime.parse(saveEndTime);

        if (endTime.isBefore(startTime)) {
          endTime = endTime.plusHours(24);
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes / 60.0)
                .setScale(2, RoundingMode.HALF_UP);

        entity.setWorktime(hours);
      } catch (DateTimeParseException e) {
        result.success = false;
        result.message = "시간 형식 오류: " + e.getMessage();
        return result;
      }
    }

    entity.setWorkyn("Y");
    entity.setRemark(id == null ? "신규 저장" : "수정됨");

    tbPB201Repository.save(entity);

    result.success = true;
    return result;
  }

  @PostMapping("/delete")
  public AjaxResult deleteManage(
          @RequestParam(value = "id", required = false) Long id,
          Authentication auth) {

    AjaxResult result = new AjaxResult();
    User user = (User) auth.getPrincipal();

    if (id == null) {
      result.success = false;
      result.message = "삭제할 ID가 없습니다.";
      return result;
    }

    Optional<TB_PB201> optionalEntity = tbPB201Repository.findById(id);

    if (optionalEntity.isEmpty()) {
      result.success = false;
      result.message = "데이터를 찾을 수 없습니다.";
      return result;
    }

    TB_PB201 entity = optionalEntity.get();

    // 🔹 본인 사업장(spjangcd) 데이터만 삭제 가능하도록 보안 체크
    if (!user.getSpjangcd().equals(entity.getSpjangcd())) {
      result.success = false;
      result.message = "권한이 없습니다.";
      return result;
    }

    try {
      tbPB201Repository.delete(entity);

      result.success = true;
      result.message = "삭제되었습니다.";
    } catch (Exception e) {
      result.success = false;
      result.message = "삭제 실패: " + e.getMessage();
    }

    return result;
  }


}

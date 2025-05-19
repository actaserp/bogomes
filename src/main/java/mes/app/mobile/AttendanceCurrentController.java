package mes.app.mobile;

import mes.app.mobile.Service.AttendanceCurrentService;
import mes.app.mobile.Service.AttendanceStatisticsService;
import mes.domain.entity.User;
import mes.domain.entity.approval.TB_E080;
import mes.domain.entity.approval.TB_E080_PK;
import mes.domain.entity.mobile.TB_PB204;
import mes.domain.model.AjaxResult;
import mes.domain.repository.mobile.TB_PB204Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/attendance_current")
public class AttendanceCurrentController {
    @Autowired
    AttendanceCurrentService attendanceCurrentService;
    @Autowired
    TB_PB204Repository tbPb204Repository;
    // 개인별 휴가 현황 조회
    @GetMapping("/read")
    public AjaxResult getUserInfo(
            @RequestParam(value="workcd", required = false) String workcd,
            @RequestParam(value="searchYear") String searchYear,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        String username = user.getUsername();
        int personId = user.getPersonid();

        // 개인별 연차정보 조회
        Map<String, Object> annInfo = attendanceCurrentService.getAnnInfo(personId);
        String rtdate = "";
        if(annInfo != null) {
            rtdate = (String) annInfo.get("rtdate");
            annInfo.put("rtdate",rtdate.substring(0, 4) + "." + rtdate.substring(4, 6) + "." + rtdate.substring(6));
        }
        // 개인별 휴가정보 조회
        List<Map<String, Object>> vacInfo = attendanceCurrentService.getVacInfo(workcd, searchYear, personId);
        for(Map<String, Object>vacDetail : vacInfo) {
            String reqdate = (String) vacDetail.get("reqdate"); // YYYYMM
            String frdate = (String) vacDetail.get("frdate");
            String todate = (String) vacDetail.get("todate");
            String yearflag = (String) vacDetail.get("yearflag"); // 연차구분
            vacDetail.put("reqdate",reqdate.substring(0, 4) + "." + reqdate.substring(4, 6) + "." + reqdate.substring(6));
            vacDetail.put("frdate",frdate.substring(0, 4) + "." + frdate.substring(4, 6) + "." + frdate.substring(6));
            vacDetail.put("todate",todate.substring(0, 4) + "." + todate.substring(4, 6) + "." + todate.substring(6));
            if("1".equals(yearflag)) {
                vacDetail.put("yearflag","O");
            }else{
                vacDetail.put("yearflag","");
            }
        }
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("annInfo", annInfo);
        resultMap.put("vacInfo", vacInfo);

        result.data = resultMap;
        return result;
    }
    // 휴가정보 수정
    @PostMapping("/updateAttendance")
    public AjaxResult submitCommute(
            @RequestParam(value="vacId", required=false) Integer vacId,
            @RequestParam(value="attKind", required=false) String attKind,
            @RequestParam(value="startDateTime", required=false) String startDateTime,
            @RequestParam(value="endDateTime", required=false) String endDateTime,
            @RequestParam(value="isAnnual", required=false) String isAnnual,
            @RequestParam(value="useDate", required=false) BigDecimal useDate,
            @RequestParam(value="remark", required=false) String remark,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        // 기존 휴가 데이터 조회
        Optional<TB_PB204> optionalTbPb204 = tbPb204Repository.findById(vacId);

        if (optionalTbPb204.isPresent()) {
            TB_PB204 savedtbPb204 = optionalTbPb204.get();

            Map<String, String> startParts = extractDateTimeParts(startDateTime);
            Map<String, String> endParts = extractDateTimeParts(endDateTime);

            savedtbPb204.setFrdate(startParts.get("date")); // 시작일자
            savedtbPb204.setSttime(startParts.get("time")); // 시작시간
            savedtbPb204.setTodate(endParts.get("date"));   // 종료일자
            savedtbPb204.setEdtime(endParts.get("time"));   // 종료시간
            savedtbPb204.setDaynum(useDate); // 휴가기간
            savedtbPb204.setWorkcd(attKind); // 휴가구분
            savedtbPb204.setRemark(remark); // 휴가 사유
            result.data = tbPb204Repository.save(savedtbPb204);
            result.message = "휴가수정이 완료되었습니다.";
        } else {
            System.out.println("해당 ID로 데이터를 찾을 수 없습니다.");
        }
        return result;
    }

    // 날자 시간 분리 메서드
    private Map<String, String> extractDateTimeParts(String dateTime) {
        Map<String, String> dateTimeParts = new HashMap<>();

        if (dateTime != null && dateTime.contains("T")) {
            String[] parts = dateTime.split("T");
            dateTimeParts.put("date", parts[0].replaceAll("-", "")); // YYYYMMDD
            dateTimeParts.put("time", parts[1]);                     // HH:mm
        } else {
            dateTimeParts.put("date", null);
            dateTimeParts.put("time", null);
        }

        return dateTimeParts;
    }
}

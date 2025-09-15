package mes.app.mobile;

import mes.app.mobile.Service.CommuteCurrentService;
import mes.app.mobile.Service.MobileMainService;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commute_current")
public class CommuteCurrentController {
    // 출퇴근현황
    @Autowired
    CommuteCurrentService commuteCurrentService;

    // 사용자 근무
    @GetMapping("/read")
    public AjaxResult getUserInfo(
            @RequestParam(value="workcd", required = false) String workcd,
            @RequestParam(value="workDate") String workDate,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();
        String username = user.getUsername();

        List<Map<String, Object>> data = commuteCurrentService.getUserInfo(username, workcd, workDate);

        for (Map<String, Object> dataDetail : data) {
            
        }
        result.data = data;

        return result;
    }

    // 사용자 근무조회페이지 조회
    @GetMapping("/read_record")
    public AjaxResult getUserInfo2(
            @RequestParam(value="selectWorkcd", required = false) String workcd,
            @RequestParam(value="searchToDate") String searchToDate,
            @RequestParam(value="searchFromDate") String searchFromDate,
            HttpServletRequest request,
            Authentication auth) {
        AjaxResult result = new AjaxResult();
        User user = (User)auth.getPrincipal();
        String username = user.getUsername();

        List<Map<String, Object>> data = commuteCurrentService.getUserInfo2(username, workcd, searchFromDate, searchToDate);

        for (Map<String, Object> dataDetail : data) {
            // 일자 포멧
            String workym = (String) dataDetail.get("workym");
            String formatted = workym.substring(0, 4) + "." + workym.substring(4, 6);
            dataDetail.put("workym", formatted);
        }
        result.data = data;

        return result;
    }
}

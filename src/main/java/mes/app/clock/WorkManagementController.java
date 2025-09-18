package mes.app.clock;

import lombok.extern.slf4j.Slf4j;
import mes.app.clock.service.WorkManagementService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/clock/work_management")
public class WorkManagementController {

  @Autowired
  WorkManagementService workManagementService;

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
  public AjaxResult defectsList () {

    List<Map<String, Object>> items = this.workManagementService.defectsList();
    AjaxResult result = new AjaxResult();
    result.data = items;

    return result;
  }

}

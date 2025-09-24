package mes.app.aftersales;

import lombok.extern.slf4j.Slf4j;
import mes.app.aftersales.service.ASRepairStatusService;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/aftersales/ASRepairStatus")
public class ASRepairStatusController {

  @Autowired
  ASRepairStatusService asRepairStatusService;

  @GetMapping("/read")
  public AjaxResult getList(@RequestParam (value = "start") Date start,
                            @RequestParam (value = "end") Date end,
                            @RequestParam (value = "txtvechidno", required = false) String vechidno,
                            @RequestParam(value = "cboEndflag", required = false) String cboEndflag) {

    List<Map<String, Object>> items = asRepairStatusService.getList(start, end, vechidno, cboEndflag);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

}

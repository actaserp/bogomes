package mes.app.aftersales;

import lombok.extern.slf4j.Slf4j;
import mes.app.aftersales.service.ASListStatusService;
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
@RequestMapping("/api/aftersales/as_list_status")
public class ASListStatusController {

  @Autowired
  ASListStatusService asListStatusService;

  @GetMapping("/read")
  public AjaxResult getList(@RequestParam(value = "start") Date start,
                            @RequestParam(value = "end") Date end,
                            @RequestParam(value = "txtvechidno", required = false)String vechidno,
                            @RequestParam(value = "cboEndflag", required = false) String cboEndflag){

    List<Map<String, Object>> items = this.asListStatusService.getList(start, end, vechidno, cboEndflag);
    AjaxResult result = new AjaxResult();
    result.data = items;
    return result;
  }

}

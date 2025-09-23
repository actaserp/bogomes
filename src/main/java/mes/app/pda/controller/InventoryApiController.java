package mes.app.pda.controller;

import lombok.extern.slf4j.Slf4j;
import mes.app.pda.service.InventoryApiService;
import mes.app.pda.service.ShipmentApiService;
import mes.app.util.UtilClass;
import mes.domain.model.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pda/inventory/material_current_stock")
public class InventoryApiController {

    @Autowired
    InventoryApiService inventoryApiService;

    @GetMapping("/read")
    public AjaxResult getMaterialCurrentStockList(
            @RequestParam(value="mat_type", required=false) String mat_type,
            @RequestParam(value="mat_grp_pk", required=false) Integer mat_grp_pk,
            @RequestParam(value="mat_name", required=false) String mat_name,
            @RequestParam(value="store_house_id", required=false) Integer store_house_id,
            @RequestParam(value="spjangcd", required=false) String spjangcd,
            HttpServletRequest request) {

        List<Map<String, Object>> items = this.inventoryApiService.getMaterialCurrentStockList(mat_type, mat_grp_pk, mat_name, store_house_id, spjangcd);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }


}

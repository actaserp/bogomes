package mes.app.shipment;

import mes.app.shipment.service.ShipmentDoaService;
import mes.app.shipment.service.VehicleDevService;
import mes.domain.model.AjaxResult;
import mes.domain.repository.ShipmentHeadRepository;
import mes.domain.repository.ShipmentRepository;
import mes.domain.repository.SujuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipment/vehicle_dev")
public class VehicleDevController {

    @Autowired
    private VehicleDevService vehicleDevService;

    @Autowired
    ShipmentRepository shipmentRepository;

    @Autowired
    ShipmentHeadRepository shipmentHeadRepository;

    @Autowired
    SujuRepository sujuRepository;

    @GetMapping("/order_list")
    public AjaxResult getOrderList(
            @RequestParam("srchVehicleNum") String srchVehicleNum,
            @RequestParam("srchVehiclePer") String srchVehiclePer){

        List<Map<String, Object>> items = this.vehicleDevService.getOrderList(srchVehicleNum, srchVehiclePer);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @GetMapping("/order_list2")
    public AjaxResult getOrderList2(
            @RequestParam("srchVehicleDate") String srchVehicleDate,
            @RequestParam("srchVehicleNum2") String srchVehicleNum,
            @RequestParam("srchVehiclePer2") String srchVehiclePer){

        List<Map<String, Object>> items = this.vehicleDevService.getOrderList2(srchVehicleDate, srchVehicleNum, srchVehiclePer);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @GetMapping("/search_detail_suju")
    public AjaxResult getSujuDetail(
            @RequestParam("searchId") Integer searchId)
    {

        Map<String, Object> item = new HashMap<>();
        item = this.vehicleDevService.getSujuDetailSuju(searchId);

        AjaxResult result = new AjaxResult();
        result.data = item;

        return result;
    }
}

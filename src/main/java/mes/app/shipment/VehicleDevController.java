package mes.app.shipment;

import mes.app.shipment.service.ShipmentDoaService;
import mes.app.shipment.service.VehicleDevService;
import mes.domain.entity.Material;
import mes.domain.entity.Shipment;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.MaterialRepository;
import mes.domain.repository.ShipmentHeadRepository;
import mes.domain.repository.ShipmentRepository;
import mes.domain.repository.SujuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
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

    @Autowired
    TransactionOperations transactionTemplate;

    @Autowired
    MaterialRepository materialRepository;

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
            @RequestParam("searchId") Integer searchId,
            @RequestParam("searchType") String searchType
    )
    {

        Map<String, Object> item = new HashMap<>();
        if(searchType.equals("shipped")){

        }
        item = this.vehicleDevService.getSujuDetailSuju(searchId);

        AjaxResult result = new AjaxResult();
        result.data = item;

        return result;
    }

    // 출고 처리
    @PostMapping("/shipment_status_complete")
    public AjaxResult shipmentStatusComplete(
            @RequestParam(value = "searchId", required = false) Integer searchId,
            @RequestParam(value = "vechidno", required = false) String vechidno,
            @RequestParam(value = "spcno", required = false) String spcno,
            @RequestParam(value = "CustomerBarcode", required = false) String CustomerBarcode,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();

        User user = (User)auth.getPrincipal();

        // Validation 체크 - 출하처리시 Material.currentStock 값과 Shipment.Qty(처리량)을 비교하여 값이 '-'인 경우 출하가 되지않도록 처리
        List<Shipment> shipmentList = this.shipmentRepository.findByShipmentHeadId(searchId);

        if (shipmentList != null) {
            for (int i = 0; i < shipmentList.size(); i++) {
                Integer materialId = shipmentList.get(i).getMaterialId();
                Material material = this.materialRepository.getMaterialById(materialId);

                if (material != null) {
                    Float currentStock = material.getCurrentStock() != null ? material.getCurrentStock() : 0;
                    Double shipQty = shipmentList.get(i).getQty() != null ? shipmentList.get(i).getQty() : 0;

                    Float parsedShipQty = shipQty.floatValue();

                    if (Float.compare(currentStock, parsedShipQty) < 0) {
                        result.success = false;
                        result.message = "재고 수량이 부족합니다.";
                        return result;
                    }
                }
            }
        }

        this.transactionTemplate.executeWithoutResult(status->{

            // Shipment 테이블의 상태값 변경시 트리거 사용하여 "a"로 설정시 트리거를 통해 mat_inout 테이블에 출고데이터가 추가됨
            List<Shipment> smList = this.shipmentRepository.findByShipmentHeadId(searchId);

            if (smList != null) {
                for (int i = 0; i < smList.size(); i++) {
                    Shipment sm = smList.get(i);

                    if (sm != null) {
                        sm.set_status("a");
                        sm.set_audit(user);
                        sm.setVechidno(vechidno);
                        sm.setSpcno(spcno);
                        sm.setSpcmngno(CustomerBarcode);
                        sm.setDevdate(new Date());
                        Double orderQty = sm.getOrderQty();
                        sm.setQty(orderQty);
                        this.shipmentRepository.save(sm);
                    }
                }
            }
            // 수주헤더 기준으로 출하항목(shipment) 금액합산 정리
            this.vehicleDevService.updateShipmentStateComplete(searchId);
            // 관련 수주를 찾아서 수주의 출하 상태를 변경한다.
            this.vehicleDevService.updateSujuShipmentState(searchId);
        });

        return result;
    }

    // 출고 취소
    @PostMapping("/shipment_status_cancel")
    public AjaxResult shipmentStatusCancel(
            @RequestParam(value = "id", required = false) Integer searchId,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();

        this.transactionTemplate.executeWithoutResult(status -> {
            // Shipment 리스트 조회
            List<Shipment> smList = this.shipmentRepository.findByShipmentHeadId(searchId);

            if (smList != null) {
                for (Shipment sm : smList) {
                    // 출고 확정("a") 상태인 건만 취소 처리
                    if (sm != null && "a".equals(sm.get_status())) {

                        sm.setVechidno(null);
                        sm.setQty((double) 0);
                        sm.set_status("t");        // 취소 → 상태를 "t"(임시/ordered) 로 되돌림
                        sm.set_audit(user);        // 변경자 기록
                        sm.setDevdate(null); // 처리일시 초기화
                        this.shipmentRepository.save(sm);
                    }
                }
            }
            // 수주헤더 기준으로 출하항목(shipment) state변경
            this.vehicleDevService.updateShipmentStateCancel(searchId);
            // 관련 수주 상태도 취소 처리에 맞게 갱신
            this.vehicleDevService.updateSujuShipmentStateCancel(searchId);
        });

        result.success = true;
        result.message = "출고가 취소되었습니다.";
        return result;
    }



}

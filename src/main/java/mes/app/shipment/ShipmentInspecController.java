package mes.app.shipment;

import lombok.extern.slf4j.Slf4j;
import mes.app.pda.service.ShipmentApiService;
import mes.app.shipment.service.ShipmentInspecService;
import mes.app.util.UtilClass;
import mes.domain.entity.*;
import mes.domain.model.AjaxResult;
import mes.domain.repository.*;
import mes.domain.services.DateUtil;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/shipment/shipment_inspec")
public class ShipmentInspecController {

    @Autowired
    ShipmentInspecService shipmentApiService;

    @Autowired
    MatLotRepository matLotRepository;

    @Autowired
    MatProcInputRepository matProcInputRepository;

    @Autowired
    JobResRepository jobResRepository;

    @Autowired
    MatProcInputReqRepository matProcInputReqRepository;

    @Autowired
    MaterialRepository materialRepository;

    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    MatLotConsRepository matLotConsRepository;

    @Autowired
    MatInoutRepository matInoutRepository;

    @Autowired
    MatConsuRepository matConsuRepository;

    @Autowired
    ShipmentHeadRepository shipmentHeadRepository;

    @PostMapping("/read")
    public AjaxResult getShipmentOrderList(
            @RequestParam(value="srchStartDt", required=false) String date_from,
            @RequestParam(value="srchEndDt", required=false) String date_to,
            @RequestParam(value="chkNotShipped", required=false) String not_ship,
            @RequestParam(value="cboCompany", required=false) Integer comp_pk,
            @RequestParam(value="cboMatGroup", required=false) Integer mat_grp_pk,
            @RequestParam(value="cboMaterial", required=false) Integer mat_pk,
            @RequestParam(value="keyword", required=false) String keyword,
            @RequestParam(value="spjangcd", required=false) String spjangcd,
            HttpServletRequest request) {

        String state = "";
        AjaxResult result = new AjaxResult();

        try{
            if("Y".equals(not_ship)) {
                state= "ordered";
            } else {
                state = "";
            }

            if(!date_from.contains("-")){
                date_from = UtilClass.toContainsHyphenDateString(date_from);
            }
            if(!date_to.contains("-")){
                date_to = UtilClass.toContainsHyphenDateString(date_to);
            }

            List<Map<String, Object>> items = this.shipmentApiService.ApigetShipmentOrderList(date_from, date_to, state, comp_pk, mat_grp_pk, mat_pk, keyword);
            //List<Map<String, Object>> test = Collections.emptyList();


            result.data = items;

        }catch(Exception e){
            result.success = false;
            result.data = null;
            result.message = "서버에러 발생";
        }

        return result;
    }

    @GetMapping("/input_lot_list")
    public AjaxResult getInputLotList(
            @RequestParam(value = "jr_pk", required = false) Integer jrPk,
            @RequestParam(value = "mat_code", required = false) String mat_code,
            @RequestParam(value = "ship_id", required = false) Integer shipId
    ) {

        List<Map<String, Object>> items = this.shipmentApiService.getInputLotList(jrPk, shipId, mat_code);

        AjaxResult result = new AjaxResult();
        result.data = items;

        return result;
    }

    @PostMapping("/add_lot_input")
    @Transactional
    public AjaxResult addLotInput(
            @RequestParam("jr_pk") Integer jr_pk,
            @RequestParam("lot_id") Integer lotId,
            @RequestParam("input_qty") Float inputQty,
            @RequestParam("ship_id") Integer ship_id,
            Authentication auth) {

        AjaxResult result = new AjaxResult();

        User user = (User) auth.getPrincipal();

        Timestamp inoutTime = DateUtil.getNowTimeStamp();

        JobRes jr = this.jobResRepository.getJobResById(jr_pk);

        MaterialLot ml = this.matLotRepository.getMatLotById(lotId);

        if (ml != null) {
            if (ml.getCurrentStock() <= 0) {
                result.message = "가용한 재고가 없는 LOT을 지정했습니다.(" + ml.getLotNumber() + ")";
                result.success = false;
                return result;
            }

            if (ml.getStoreHouseId() == null) {
                result.message = "해당 품목의 기본창고가 지정되지 않았습니다(" + ml.getLotNumber() + ")";
                result.success = false;
                return result;
            }

            List<MatProcInput> mpiList = this.matProcInputRepository.findByMaterialProcessInputRequestIdAndMaterialLotId(jr.getMaterialProcessInputRequestId(), ml.getId());
            Integer mpiCount = mpiList.size();
            if (mpiCount > 0) {
                result.message = "이미 지정된 로트입니다.(" + ml.getLotNumber() + ")";
                result.success = false;
                return result;
            }

            MatProcInputReq mir = null;

            if (jr != null) {
                if (jr.getMaterialProcessInputRequestId() == null) {
                    mir = new MatProcInputReq();
                    mir.setRequestDate(inoutTime);
                    mir.setRequesterId(user.getId());
                    mir.set_audit(user);
                    mir = this.matProcInputReqRepository.save(mir);
                    jr.setMaterialProcessInputRequestId(mir.getId());

                } else {
                    mir = this.matProcInputReqRepository.getMatProcInputReqById(jr.getMaterialProcessInputRequestId());
                }
            }

            MatProcInput mpi = new MatProcInput();
            mpi.setMaterialProcessInputRequestId(mir.getId());
            mpi.setMaterialId(ml.getMaterialId());
            mpi.setRequestQty(inputQty);
            mpi.setInputQty((float) 0);
            mpi.setMaterialLotId(ml.getId());
            mpi.setMaterialStoreHouseId(ml.getStoreHouseId());
            mpi.setState("requested");
            mpi.setInputDateTime(inoutTime);
            mpi.setActorId(user.getId());
            mpi.set_audit(user);
            mpi.setShipId(ship_id);
            mpi = this.matProcInputRepository.save(mpi);

            result.success = true;
            result.data = mpi;
        } else {
            result.success = false;
        }

        return result;
    }

    @PostMapping("/del_lot_list")
    @org.springframework.transaction.annotation.Transactional
    public AjaxResult delLotlist(
            @RequestParam(value = "mpi_pk", required = false) Integer mpi_pk,
            HttpServletRequest request,
            Authentication auth) {

        AjaxResult result = new AjaxResult();

        this.matProcInputRepository.deleteById(mpi_pk);
        return result;
    }

    @PostMapping("/confirm_shipment")
    @Transactional
    public AjaxResult confirmShipment(
            @RequestParam("ship_id") Integer shipId,
            @RequestParam("jr_pk") Integer jrPk,
            @RequestParam("spjangcd") String spjangcd,
            Authentication auth) {

        AjaxResult result = new AjaxResult();
        User user = (User) auth.getPrincipal();
        Timestamp now = DateUtil.getNowTimeStamp();
        LocalDateTime ldt = now.toLocalDateTime();

        JobRes jr = jobResRepository.getJobResById(jrPk);

        List<Map<String, Object>> mpiList = shipmentApiService.getMaterialProcessInputListByShipId(jr.getMaterialProcessInputRequestId(), shipId);

        float totalQty = 0f;
        Map<Integer, Float> consumedByMat = new HashMap<>();

        for (Map<String, Object> mpiMap : mpiList) {
            float reqQty = Float.parseFloat(mpiMap.get("req_qty").toString());
            int matLotId = (int) mpiMap.get("ml_id");
            int matPk = (int) mpiMap.get("mat_pk");
            int shPk = (int) mpiMap.get("sh_id");
            String lotNumber = (String) mpiMap.get("lot_number");
            float currentStock = Float.parseFloat(mpiMap.get("curr_qty").toString());
            consumedByMat.merge(matPk, reqQty, Float::sum);

            if (reqQty <= 0 || currentStock < reqQty) {
                result.success = false;
                result.message = "LOT 재고 부족: " + lotNumber;
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return result;
            }

            // MatLotCons
            MatLotCons mlc = new MatLotCons();
            mlc.setMaterialLotId(matLotId);
            mlc.setOutputDateTime(now);
            mlc.setSourceDataPk(jr.getId());
            mlc.setSourceTableName("shipment_inspec");
            mlc.setOutputQty(reqQty);
            mlc.set_audit(user);
            mlc.setSpjangcd(spjangcd);
            mlc.setShipId(shipId);
            matLotConsRepository.save(mlc);

            // Inout (out)
            MaterialInout mio = new MaterialInout();
            mio.setMaterialId(matPk);
            mio.setStoreHouseId(shPk);
            mio.setLotNumber(lotNumber);
            mio.setInoutDate(ldt.toLocalDate());
            mio.setInoutTime(ldt.toLocalTime().withNano(0));
            mio.setInOut("out");
            mio.setOutputType("shipment_inspec_out");
            mio.setOutputQty(reqQty);
            mio.setSourceDataPk(mlc.getId());    // mlc 기준으로 연결
            mio.setSourceTableName("mat_lot_cons");
            mio.setState("confirmed");
            mio.set_status("a");
            mio.setDescription("출고 검사 소요");
            mio.set_audit(user);
            mio.setSpjangcd(spjangcd);
            matInoutRepository.save(mio);

            totalQty += reqQty;

        }

        for (Map.Entry<Integer, Float> e : consumedByMat.entrySet()) {
            int matPk = e.getKey();
            float consumedQty = e.getValue();
            Material consMat = materialRepository.getMaterialById(matPk);

            MaterialConsume mc = new MaterialConsume();
            mc.setJobResponseId(jr.getId());
            mc.setMaterialId(matPk);
            mc.setProcessOrder(0);
            mc.setLotIndex(0);
            mc.setStartTime(now);
            mc.setEndTime(now);
            mc.setDescription("출고 검사 소요");
            mc.setBomQty(consumedQty);
            mc.setConsumedQty(consumedQty);
            mc.set_audit(user);
            mc.setState("finished");
            mc.set_status("a");
            mc.setStoreHouseId(consMat.getStoreHouseId());
            mc.setSpjangcd(spjangcd);
            matConsuRepository.save(mc);
        }

        ShipmentHead head = shipmentHeadRepository.findById(shipId)
                .orElseThrow(() -> new RuntimeException("ShipmentHead not found: " + shipId));
        head.setState("inspec");
        head.set_audit(user); // 감리 컬럼 있다면
        shipmentHeadRepository.save(head);


        result.success = true;
        result.message = "출고 검사를 완료되었습니다.";
        return result;
    }

    @GetMapping("/consumed_list")
    public AjaxResult getConsumedList(
            @RequestParam(value = "prod_date", required = false) String prodDate,
            @RequestParam(value = "prod_mat_id", required = false) Integer prod_mat_id,
            @RequestParam(value = "need_pro_mat_qty", required = false) BigDecimal need_pro_mat_qty) {


        List<Map<String, Object>> items;
        items = this.shipmentApiService.getConsumedListPlan(prod_mat_id, need_pro_mat_qty, prodDate);


        AjaxResult result = new AjaxResult();
        result.data = items;
        System.out.println(items);
        return result;
    }



}

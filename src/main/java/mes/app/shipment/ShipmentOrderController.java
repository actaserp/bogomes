package mes.app.shipment;

import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

import mes.domain.entity.*;
import mes.domain.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.app.shipment.service.ShipmentOrderService;
import mes.domain.model.AjaxResult;
import mes.domain.services.CommonUtil;

@RestController
@RequestMapping("/api/shipment/shipment_order")
public class ShipmentOrderController {

	@Autowired 
	private ShipmentOrderService shipmentOrderService;
	
	@Autowired
	ShipmentRepository shipmentRepository;
	
	@Autowired
	ShipmentHeadRepository shipmentHeadRepository;
	
	@Autowired
	RelationDataRepository relationDataRepository;

	@Autowired
	TransactionTemplate transactionTemplate;

	@Autowired
	MaterialRepository materialRepository;


	@GetMapping("/suju_list")
	public AjaxResult getSujuList(
			@RequestParam("srchStartDt") String dateFrom,
			@RequestParam("srchEndDt") String dateTo,
			@RequestParam("not_ship") String notShip,
			@RequestParam("cboCompany") String compPk,
			@RequestParam("cboMatGroup") String matGrpPk,
			@RequestParam("cboMaterial") String matPk,
			@RequestParam("keyword") String keyword ){
		
		List<Map<String, Object>> items = this.shipmentOrderService.getSujuList(dateFrom,dateTo,notShip,compPk,matGrpPk,matPk,keyword);
		
		AjaxResult result = new AjaxResult();
		result.data = items;
		
		return result;
	}

	@GetMapping("/product_list")
	public AjaxResult getProductList(
			@RequestParam("cboMatGroup") String matGrpPk,
			@RequestParam("cboMaterial") String matPk,
			@RequestParam("keyword") String keyword){
		
		List<Map<String, Object>> items = this.shipmentOrderService.getProductList(matGrpPk,matPk,keyword);
		
		AjaxResult result = new AjaxResult();
		result.data = items;
		
		return result;
	}

	@PostMapping("/save_shipment_order")
	@Transactional
	public AjaxResult saveShipmentOrder(
			@RequestParam("Company_id") Integer CompanyId,
			@RequestParam(value = "Description", required = false) String Description,
			@RequestParam("ShipDate") String Ship_date,
			@RequestParam MultiValueMap<String,Object> sujuData, // 단일 값 넘어옴
			@RequestParam("TableName") String TableName,
			Authentication auth) {

		User user = (User) auth.getPrincipal();
		AjaxResult result = new AjaxResult();

		Timestamp today = new Timestamp(System.currentTimeMillis());
		Timestamp shipDate = CommonUtil.tryTimestamp(Ship_date);

		ShipmentHead smh = new ShipmentHead();
		smh.setCompanyId(CompanyId);
		smh.setShipDate(shipDate);
		smh.setOrderDate(today);
		smh.setDescription(Description);
		smh.set_audit(user);
		smh.setState("ordered");

		int orderSum = 0;
		double totalPrice = 0;
		double totalVat = 0;

		// ✅ 단일 값 파싱
		Integer sujuPk = sujuData.getFirst("suju_pk") != null
				? Integer.valueOf(sujuData.getFirst("suju_pk").toString())
				: null;

		Integer matId = sujuData.getFirst("mat_id") != null
				? Integer.valueOf(sujuData.getFirst("mat_id").toString())
				: Integer.valueOf(sujuData.getFirst("matId").toString());

		Integer orderQty = sujuData.getFirst("order_qty") != null
				? Integer.valueOf(sujuData.getFirst("order_qty").toString())
				: 0;

		String matName = sujuData.getFirst("mat_name") != null
				? sujuData.getFirst("mat_name").toString()
				: "";

		// ✅ 수주일 경우 relationSujuList 조회
		List<Suju> relationSujuList = new ArrayList<>();
		if ("suju".equalsIgnoreCase(TableName) && sujuPk != null) {
			relationSujuList = shipmentOrderService.getRelationSujuList(sujuPk);
		}

		// ✅ 품목일 경우 productItems 조회
		List<Map<String,Object>> productItems = new ArrayList<>();
		if ("product".equalsIgnoreCase(TableName) && matId != null) {
			productItems = shipmentOrderService.getProdcutList(
					Collections.singleton(matId), CompanyId);
		}

		// ✅ 현재고 체크
		if (matId != null) {
			Material material = materialRepository.findById(matId).orElse(null);
			if (material == null || material.getCurrentStock() == null) {
				result.success = false;
				result.message = "품목 [" + matName + "]의 재고 현황이 존재하지 않습니다.";
				return result;
			}
			if (orderQty > material.getCurrentStock()) {
				result.success = false;
				result.message = "품목 [" + matName + "]의 출하 수량이 현재고를 초과합니다.";
				return result;
			}
		}

		smh = this.shipmentHeadRepository.save(smh);

		Shipment sm = new Shipment();
		sm.setShipmentHeadId(smh.getId());
		sm.setMaterialId(matId);
		sm.setOrderQty((double) orderQty);
		sm.setQty(0d);
		sm.setDescription(Description);
		sm.setSourceDataPk(sujuPk);

		if ("product".equalsIgnoreCase(TableName)) {
			sm.setSourceTableName("product");

			Map<String,Object> product = productItems.stream()
					.filter(p -> Objects.equals(p.get("Material_id"), matId))
					.findFirst().orElse(null);

			if (product != null) {
				Double unitPrice = (Double) product.get("UnitPrice");
				sm.setUnitPrice(unitPrice);
				sm.setPrice(unitPrice * orderQty);
				totalPrice += unitPrice * orderQty;
				totalVat += (unitPrice * orderQty) * 0.1;
			}

		} else if ("suju".equalsIgnoreCase(TableName)) {
			Suju item = relationSujuList.stream()
					.filter(s -> Objects.equals(s.getId(), sujuPk))
					.findFirst().orElse(null);

			if (item != null) {
				double qty = item.getSujuQty();
				if (item.getVat() != null) {
					double vat = item.getVat().doubleValue();
					double unitVat = vat / qty;
					double vatSum = unitVat * orderQty;
					sm.setVat(vatSum);
					totalVat += vatSum;
				}
				if (item.getPrice() != null) {
					double price = item.getPrice().doubleValue();
					double unitPrice = price / qty;
					double priceSum = unitPrice * orderQty;
					sm.setUnitPrice(unitPrice);
					sm.setPrice(priceSum);
					totalPrice += priceSum;
				}
				sm.setSourceTableName("rela_data");
			}
		}

		sm.set_audit(user);
		sm = this.shipmentRepository.save(sm);

		// ✅ RelationData 연결 (수주일 경우만)
		if ("suju".equalsIgnoreCase(TableName) && sujuPk != null) {
			RelationData rd = new RelationData();
			rd.setTableName1("suju");
			rd.setDataPk1(sujuPk);
			rd.setTableName2("shipment");
			rd.setDataPk2(sm.getId());
			rd.set_audit(user);
			rd.setRelationName("");
			rd.setNumber1(orderQty);
			this.relationDataRepository.save(rd);
		}

		orderSum += orderQty;

		// ✅ ShipmentHead 저장
		smh.setTotalQty((float) orderSum);
		smh.setTotalPrice(totalPrice);
		smh.setTotalVat(totalVat);
		smh = this.shipmentHeadRepository.save(smh);

		result.data = smh;
		return result;
	}



	// 출하지시 목록 조회
	@GetMapping("/order_list")
	public AjaxResult getShipmentOrderList(
			@RequestParam(value="srchStartDt", required=false) String date_from, 
			@RequestParam(value="srchEndDt", required=false) String date_to,
			@RequestParam(value="chkNotShipped", required=false) String not_ship, 
			@RequestParam(value="cboCompany", required=false) Integer comp_pk,
			@RequestParam(value="cboMatGroup", required=false) Integer mat_grp_pk, 
			@RequestParam(value="cboMaterial", required=false) Integer mat_pk,
			@RequestParam(value="keyword", required=false) String keyword,
			HttpServletRequest request) {
			
		String state = "";
		if("Y".equals(not_ship)) {
			state= "ordered";
		} else {
			state = "";
		}
		
		List<Map<String, Object>> items = this.shipmentOrderService.getShipmentOrderList(date_from, date_to, state, comp_pk, mat_grp_pk, mat_pk, keyword);
        AjaxResult result = new AjaxResult();
        result.data = items;
		return result;
	}
	
	// 출하 품목 목록 조회
	@GetMapping("/shipment_item_list")
	public AjaxResult getShipmentItemList(
			@RequestParam(value="head_id", required=false) Integer head_id,
			HttpServletRequest request) {
		List<Map<String, Object>> items = this.shipmentOrderService.getShipmentItemList(head_id);
        AjaxResult result = new AjaxResult();
        result.data = items;
		return result;
	}
	
	// 출하일 변경
	@PostMapping("/update_ship_date")
	public AjaxResult updateShipDate(
			@RequestParam(value="head_id", required=false) Integer head_id,
			@RequestParam(value="ship_date", required=false) String ship_date,
			HttpServletRequest request,
			Authentication auth) {
		
        AjaxResult result = new AjaxResult();
		User user = (User)auth.getPrincipal();
		
		ShipmentHead shipmentHead = this.shipmentHeadRepository.getShipmentHeadById(head_id);

		if ("shipped".equals(shipmentHead.getState())) {		//if (shipmentHead.getState().equals("shipped")) {
			result.success = false;
		} else {
			shipmentHead.setShipDate(CommonUtil.tryTimestamp(ship_date));
			shipmentHead.set_audit(user);
			
			shipmentHead = this.shipmentHeadRepository.save(shipmentHead);
			
			result.data = shipmentHead;
		}
		
		return result;
	}
	
	// 출하지시 취소
	@PostMapping("/cancel_order")
	public AjaxResult cancelOrder(
			@RequestParam(value="shipmenthead_id", required=false) Integer head_id,
			HttpServletRequest request,
			Authentication auth) {
		
        AjaxResult result = new AjaxResult();
		
		ShipmentHead head = this.shipmentHeadRepository.getShipmentHeadById(head_id);

		if ("shipped".equals(head.getState())) {
			result.success = false;
		} else {
			this.transactionTemplate.executeWithoutResult(status->{			
				try {
					
					this.shipmentRepository.deleteByShipmentHeadId(head_id);
					this.shipmentHeadRepository.deleteById(head_id);
				}
				catch(Exception ex) {
					TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
					result.success=false;
					result.message = ex.toString();
				}				
			});					
		}
		return result;
	}

	@GetMapping("/search_detail_suju")
	public AjaxResult getSujuDetail(
			@RequestParam("TableName") String TableName,
			@RequestParam("searchId") Integer searchId)
	{

		Map<String, Object> item = new HashMap<>();
		if(TableName.equals("product")) {
			item = this.shipmentOrderService.getSujuDetailProd(searchId);
		} else if (TableName.equals("suju")) {
			item = this.shipmentOrderService.getSujuDetailSuju(searchId);
		}



		AjaxResult result = new AjaxResult();
		result.data = item;

		return result;
	}
}

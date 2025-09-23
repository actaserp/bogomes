package mes.app.shipment.service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VehicleDevService {
    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getOrderList(String srchVehicleNum, String srchVehiclePer) {

        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("srchVehicleNum", "%" + srchVehicleNum + "%");
        paramMap.addValue("srchVehiclePer", "%" + srchVehiclePer + "%");

        String sql = """
			    select sh.*,
			    c."Name" as "CompanyNm",
			    m."Name" as "matName",
			    m."CustomerBarcode",
			    fn_code_name('shipment_state', sh."State") as "headState",
			    s.devdate,
			    s.vechidno,
			    sj.id as "suju_pk",
			    m."CustomerBarcode"
			    from shipment_head sh 
			    left join company c ON sh."Company_id" = c.id
			    left join shipment s ON s."ShipmentHead_id" = sh.id
			    left join suju sj ON s.suju_head_id = sj."SujuHead_id"
			    left join material m ON sj."Material_id" = m.id 
			    where 1=1
                """;

        if (StringUtils.isEmpty(srchVehiclePer)==false)  sql += " and c.\"Name\" LIKE :srchVehiclePer";
        if (StringUtils.isEmpty(srchVehicleNum)==false)  sql += " and s.\"vechidno\"  LIKE :srchVehicleNum";

        List<Map<String,Object>> items = this.sqlRunner.getRows(sql, paramMap);

        return items;
    }
    // 그리드 더블클릭 출고관리 조회
    public Map<String, Object> getSujuDetailSuju (Integer searchId){

        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("searchId", searchId);

        String sql = """
				select sh.*,
				m.*,
				c.*,
			    c."Name" as "CompanyNm",
			    m."Name" as "matName",
			    m.id as "matId",
			    m."CustomerBarcode",
			    fn_code_name('shipment_state', sh."State") as "headState",
			    s.devdate,
			    s.vechidno,
			    sj.id as "suju_pk",
			    m."CustomerBarcode"
			    from shipment_head sh 
			    left join company c ON sh."Company_id" = c.id
			    left join shipment s ON s."ShipmentHead_id" = sh.id
			    left join suju sj ON s.suju_head_id = sj."SujuHead_id"
			    left join material m ON sj."Material_id" = m.id 
			    where sh.id = :searchId
		""";

        Map<String, Object> item = this.sqlRunner.getRow(sql, paramMap);

        return item;
    }
	// 관리대장 조회
	public List<Map<String, Object>> getOrderList2(String srchVehicleDate, String srchVehicleNum, String srchVehiclePer) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("srchVehicleNum", "%" + srchVehicleNum + "%");
		paramMap.addValue("srchVehiclePer", "%" + srchVehiclePer + "%");
//		paramMap.addValue("srchVehicleDate", srchVehicleDate);

		String sql = """
			    select sh.*,
			    c."Name" as "CompanyNm",
			    c.*,
			    m."Name" as "matNm",
			    s.*,
			    mg."Name" as "gubunnm",
			    sj.id as "suju_pk",
			    m.*
			    from shipment_head sh 
			    left join company c ON sh."Company_id" = c.id
			    left join shipment s ON s."ShipmentHead_id" = sh.id
			    left join suju sj ON s.suju_head_id = sj."SujuHead_id"
			    left join material m ON sj."Material_id" = m.id 
			    left join mat_grp mg ON m."MaterialGroup_id" = mg.id
			    where 1=1
                """;

		if (StringUtils.isEmpty(srchVehiclePer)==false)  sql += " and c.\"Name\" LIKE :srchVehiclePer";
		if (StringUtils.isEmpty(srchVehicleNum)==false)  sql += " and s.\"vechidno\"  LIKE :srchVehicleNum";
//		if (StringUtils.isEmpty(srchVehicleDate)==false)  sql += " and s.\"devdate\"  = :srchVehicleDate";

		List<Map<String,Object>> items = this.sqlRunner.getRows(sql, paramMap);

		return items;
	}

	// 수주헤더 기준으로 출하항목(shipment) 금액합산 정리
	public void updateShipmentStateComplete (Integer searchId) {

		updateShipmentQantityByLotConsume(searchId, null);

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("searchId", searchId);

		String sql = """
				with A as(
				select 
		        sh.id as sh_id
		        , count(s.id) as s_count
		        , sum(s."Price") as "TotalPrice"
		        , sum(s."Vat") as "TotalVat"
		        from shipment s 
		        inner join shipment_head sh on sh.id=s."ShipmentHead_id"
		        where sh.id=:searchId
		        group by sh.id 
		        )
		        update 
		        shipment_head 
		        set "TotalVat" = A."TotalVat"
		        , "TotalPrice" = A."TotalPrice"
		        , "State" = 'shipped'
		        from A 
		        where id=A.sh_id
				""";

		this.sqlRunner.execute(sql, paramMap);
	}

	public void updateShipmentQantityByLotConsume (Integer sh_id, Integer shipment_id) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("sh_id", sh_id);
		paramMap.addValue("shipment_id", shipment_id);

		String sql = """
				with A as(
	            select
	            s.id, coalesce(sum(mlc."OutputQty"),0) as qty  
	            from shipment s  
	            inner join shipment_head sh on sh.id = s."ShipmentHead_id" 
	            left join mat_lot_cons mlc on mlc."SourceTableName" ='shipment' and mlc."SourceDataPk" = s.id
	            where 1=1 
	            and sh.id = :sh_id
				""";

		if (shipment_id != null) {
			sql += " and s.id = :shipment_id ";
		}

		sql += """
				group by s.id),
				UPC as (
	            select
	            s.id
	            , s."Material_id"
	            , sh."Company_id"
	            , mcu."UnitPrice"
	            , m."VatExemptionYN"
	            from A
	            inner join shipment s on s.id = A.id
	            inner join shipment_head sh on sh.id = s."ShipmentHead_id" 
	            inner join material m on m.id = s."Material_id" 
	            left join mat_comp_uprice mcu on mcu."Material_id"=s."Material_id" and mcu."Company_id"=sh."Company_id" and mcu."ApplyStartDate" <=now() and mcu."ApplyEndDate" > now()
	            where sh.id = :sh_id 
	        ), B as(        
	           select 
	           s.id
	           , A.qty
	           , UPC."UnitPrice" 
	           , (A.qty * UPC."UnitPrice") as "Price"
	           , case when UPC."VatExemptionYN"='Y' then 0 else (A.qty * UPC."UnitPrice"*0.1) end  as "Vat" 
	           , s."Material_id"
	           , UPC."Company_id"
	           from shipment s 
	             inner join shipment_head sh2 on sh2.id = s."ShipmentHead_id"
	             inner join A on A.id = s.id             
	             inner join UPC on UPC.id = s.id
	        )
	        update shipment set 
	         "Qty" = B.qty 
	         , "UnitPrice" = B."UnitPrice"
	         , "Price" =  B."Price"
	         , "Vat" = B."Vat"
	        from B
	        where shipment.id = B.id
				""";

		this.sqlRunner.execute(sql, paramMap);
	}

	// 관련 수주를 찾아서 수주의 출하 상태를 변경한다.
	public void updateSujuShipmentState (Integer sh_id) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("sh_id", sh_id);

		String sql = """
		        with A as(
		        select
		        s.id as shipment_id
		        ,sh.id as sh_id
		        , rd."DataPk1" as suju_id
		        , sj."State"
		        , sj."ShipmentState"
		        from shipment s 
		        inner join shipment_head sh on sh.id=s."ShipmentHead_id"
		        inner join rela_data rd on rd."TableName1" ='suju' and rd."TableName2" ='shipment' and rd."DataPk2" =s.id
		        inner join suju sj on sj.id = rd."DataPk1" 
		        where sh.id = :sh_id
		        )
		        update suju set "ShipmentState" ='shipped'
		        from A where A.suju_id = id
				""";

		this.sqlRunner.execute(sql, paramMap);
	}
}

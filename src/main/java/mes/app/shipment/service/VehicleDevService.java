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
				select sj.*,
				sh.*,
				cp."Name" as "CompanyNm",
				cp.*,
				m."Name" as "matName",
				m.id as "matId",
				m.*,
				sm.*
				from suju sj
				left join suju_head sh ON sj."SujuHead_id" = sh.id
				left join company cp ON cp.id = sj."Company_id"
				left join material m ON m.id = sj."Material_id"
				left join shipment sm ON sm.suju_head_id = sj."SujuHead_id"
				where sj.id = :searchId
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
}

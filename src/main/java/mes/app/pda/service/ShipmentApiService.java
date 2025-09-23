package mes.app.pda.service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.app.util.UtilClass;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Service
public class ShipmentApiService {

    @Autowired
    SqlRunner sqlRunner;


    public List<Map<String, Object>> ApigetShipmentOrderList(String date_from, String date_to, String state, Integer comp_pk, Integer mat_grp_pk, Integer mat_pk, String keyword) {

        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("date_from", Date.valueOf(date_from));
        paramMap.addValue("date_to", Date.valueOf(date_to));
        paramMap.addValue("state", state);
        paramMap.addValue("comp_pk", comp_pk);
        paramMap.addValue("mat_grp_pk", mat_grp_pk);
        paramMap.addValue("mat_pk", mat_pk);
        paramMap.addValue("keyword", "%" + UtilClass.getStringSafe(keyword) + "%");

        String sql = """
				select --sh.id
				  sh.id
				 ,su."JumunNumber" as jumun_number
				, c."Name" as company_name
				--, s."Material_id" as material_id
					
				,(min(m."Name") ||\s
				 case when count(distinct m."Name") > 1\s
				      then ' 외 ' || (count(distinct m."Name") - 1) || '건'\s
				      else '' end
				) as material_name_summary
				, sum(s."OrderQty") ::int as total_qty
				, sum(s."Qty") ::int 		as qty
				, sh."OrderDate" as order_date
				, su."DueDate"  as due_date -- 납기일
				--, sh."ShipDate" as ship_date  --출하일
				, sh."State" as state
                from shipment s 
                
                left outer join shipment_head sh
				on sh.id = s."ShipmentHead_id"   
                
                left outer join suju su
				on su.id = s."SourceDataPk"
				
				inner join material m
				on s."Material_id" = m.id
				
				join company c on c.id = sh."Company_id"
				
				where sh."OrderDate" between :date_from and :date_to
				and c."Name" like :keyword
				""";

				if(StringUtils.isEmpty(state) == false){
					sql += "  and sh.\"State\" = :state ";
				}

				sql += """
						group by sh.id, su."JumunNumber", c."Name", sh."OrderDate", su."DueDate", sh."State"
						order by su."JumunNumber"
						""";




        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);

        return items;
    }

	public List<Map<String, Object>> getShipList(String dateFrom, String dateTo, String state, String keyword) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("dateFrom", dateFrom);
		paramMap.addValue("dateTo", dateTo);
		paramMap.addValue("keyword", keyword);
		paramMap.addValue("state", state);

		String sql = """
			    with SH as
			    (	select sh.id
	                , sh."Company_id" as company_id
	                , c."Name" as company_name
	                , sh."ShipDate" as ship_date
	                , sh."TotalQty" as total_qty
	                , sh."State" as state
	                , fn_code_name('shipment_state', sh."State") as state_name
	                from shipment_head sh
		            left join company c on c.id = sh."Company_id"
    		        where sh."ShipDate"  between cast(:dateFrom as date) and cast(:dateTo as date) 
                """;

		if (StringUtils.isEmpty(state)==false)  sql += " and sh.\"State\" = :state ";

		sql += """
    		), S as 
		    (
		    select s."ShipmentHead_id" as head_id
            , sum(s."OrderQty") as tot_order_qty
            , sum(s."Qty") as tot_ship_qty
		    from SH
		    inner join shipment s on s."ShipmentHead_id" = SH.id 
            inner join material m on m.id = s."Material_id"
            where 1 = 1 
    		""";

		if (StringUtils.isEmpty(keyword)==false)  sql += " and ( m.\"Name\" ilike concat('%%',:keyword,'%%') or m.\"Code\" ilike concat('%%',:keyword,'%%')) ";

		sql += """
			    group by s."ShipmentHead_id"
			    )
			    select SH.*
	            , S.tot_order_qty
	            , S.tot_ship_qty
			    from SH 
			    inner join S on S.head_id = SH.id
	            where 1 = 1
        		""";

		List<Map<String,Object>> items = this.sqlRunner.getRows(sql, paramMap);

		return items;
	}

	public List<Map<String, Object>> getShipmentItemList(String headId) {
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("headId", headId);

		String sql = """
			select s.id as ship_pk
				, s."Material_id" as mat_pk
				, m."Code" as mat_code
				, m."Name" as mat_name
				, u."Name" as unit_name
				, s."OrderQty" as order_qty
				, s."Qty" as ship_qty
				, s."SourceDataPk" as src_data_pk
				, s."SourceTableName" as src_table_name
				from shipment  s
				inner join material m on m.id = s."Material_id"
				left join unit u on u.id = m."Unit_id"
				inner join shipment_head sh on sh.id = s."ShipmentHead_id"
			where s."ShipmentHead_id" = cast(:headId as Integer)
            order by m."Code", m."Name"
				""";

		List<Map<String,Object>> items = this.sqlRunner.getRows(sql, paramMap);

		return items;
	}

}

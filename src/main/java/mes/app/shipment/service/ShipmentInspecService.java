package mes.app.shipment.service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.app.util.UtilClass;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;

@Service
public class ShipmentInspecService {

    @Autowired
    SqlRunner sqlRunner;


    public List<Map<String, Object>> ApigetShipmentOrderList(String date_from, String date_to, String state, Integer comp_pk, Integer mat_grp_pk, Integer mat_pk, String keyword) {

        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("date_from", Date.valueOf(date_from));
        paramMap.addValue("date_to", Date.valueOf(date_to));
        paramMap.addValue("keyword", "%" + UtilClass.getStringSafe(keyword) + "%");

        String sql = """
				select --sh.id
				  sh.id as ship_id
				 ,su."JumunNumber" as jumun_number
				, c."Name" as company_name
				--, s."Material_id" as material_id
					
				,(min(m."Name") ||
				 case when count(distinct m."Name") > 1
				      then ' 외 ' || (count(distinct m."Name") - 1) || '건'
				      else '' end
				) as material_name_summary
				, sum(s."OrderQty") ::int as total_qty
				, sum(s."Qty") ::int 		as qty
				, sh."OrderDate" as order_date
				, su."DueDate"  as due_date -- 납기일
				--, sh."ShipDate" as ship_date  --출하일
				, sh."State" as state
				, jr."ProductionDate" AS prod_date
				, m.id as mat_pk
				, jr.id as jr_pk
				, jr."GoodQty" as prod_qty
                from shipment s 
                
                left outer join shipment_head sh
				on sh.id = s."ShipmentHead_id"   
                
                left outer join suju su
				on su.id = s."SourceDataPk"
				
				inner join material m
				on s."Material_id" = m.id
				
				join company c on c.id = sh."Company_id"
				
				LEFT JOIN job_res jr
				       ON jr."SourceTableName" = 'suju'
				      AND jr."SourceDataPk" = su.id
				
				where sh."OrderDate" between :date_from and :date_to
				and c."Name" like :keyword
				and sh."State" = 'ordered'
				and jr."State" = 'finished'
				AND (jr."Parent_id" IS NULL)
				""";

				if(StringUtils.isEmpty(state) == false){
					sql += "  and sh.\"State\" = :state ";
				}

				sql += """
						group by sh.id, su."JumunNumber", c."Name", sh."OrderDate", su."DueDate", sh."State", jr."ProductionDate", m.id, jr.id
						order by su."JumunNumber"
						""";




        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);

        return items;
    }

	public List<Map<String, Object>> getShipList(String dateFrom, String dateTo, String keyword) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("dateFrom", dateFrom);
		paramMap.addValue("dateTo", dateTo);
		paramMap.addValue("keyword", keyword);

		String sql = """
		with SH as
		(	select sh.id
			, sh."Company_id" as company_id
			, c."Name" as company_name
			, sh."ShipDate" as ship_date
			, sh."TotalQty" as total_qty
			, sh."State" as state
			, CASE
				  WHEN sh."State" = 'ordered' THEN '출하'
				  WHEN sh."State" = 'shipped' THEN '출고'
				  WHEN sh."State" = 'inspec'  THEN '검사'
				  ELSE sh."State"
				END as state_name
			from shipment_head sh
			left join company c on c.id = sh."Company_id"
			where sh."ShipDate"  between cast(:dateFrom as date) and cast(:dateTo as date) 
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

	public List<Map<String, Object>> getConsumedListPlan(Integer prodMatId, BigDecimal needProMatQty, String prodDate) {
		MapSqlParameterSource p = new MapSqlParameterSource();
		p.addValue("prodMatId", prodMatId);
		p.addValue("needQty", needProMatQty);
		p.addValue("prodDate", (prodDate == null || prodDate.isBlank()) ? null : prodDate);

		String sql = """
        WITH bom1 AS (
            SELECT
                b1.id AS bom_pk,
                b1."Material_id" AS prod_pk,
                b1."OutputAmount" AS produced_qty,
                :needQty::numeric AS order_qty,
                ROW_NUMBER() OVER (PARTITION BY b1."Material_id" ORDER BY b1."Version" DESC) AS g_idx
            FROM bom b1
            WHERE b1."BOMType" = 'manufacturing'
              AND (NULLIF(:prodDate,'')::date IS NULL
		      OR NULLIF(:prodDate,'')::date BETWEEN b1."StartDate" AND b1."EndDate")
              AND b1."Material_id" = :prodMatId
        ),
        BT AS (
            SELECT
                bc."Material_id" AS mat_pk,
                b.produced_qty,
                bc."Amount" AS quantity,
                (bc."Amount" / NULLIF(b.produced_qty,0)) AS bom_ratio,
                (bc."Amount" / NULLIF(b.produced_qty,0)) * b.order_qty AS bom_requ_qty
            FROM bom_comp bc
            JOIN bom1 b ON b.bom_pk = bc."BOM_id"
            WHERE b.g_idx = 1
        )
        SELECT
            BT.mat_pk,
            mg."MaterialType" AS mat_type,
            fn_code_name('mat_type', mg."MaterialType") AS mat_type_name,
            mg."Name" AS mat_group_name,
            m."Code" AS mat_code,
            m."Name" AS mat_name,
            m."LotSize" AS lot_size,
            mh."CurrentStock" AS "currentStock",
            u."Name" AS unit,
            BT.bom_ratio,
            ROUND(BT.bom_requ_qty::numeric) AS bom_consumed,   -- 예상 소요
            0::numeric AS consumed_qty,                        -- 아직 미시작이므로 0
            sh."Name" AS storehouse_name,
            0::numeric AS mc_qty,
            0::numeric AS current_qty_sum,
            COALESCE(m."LotUseYN",'N') AS "lotUseYn",
            CASE WHEN m."Useyn"='1' THEN 'Y' WHEN m."Useyn"='0' THEN 'N' ELSE NULL END AS useyn
        FROM BT
        JOIN material m   ON m.id = BT.mat_pk
        LEFT JOIN mat_grp mg  ON mg.id = m."MaterialGroup_id"
        LEFT JOIN unit u      ON u.id = m."Unit_id"
        LEFT JOIN store_house sh ON sh.id = m."StoreHouse_id"
        LEFT JOIN mat_in_house mh ON mh."Material_id" = m.id AND mh."StoreHouse_id" = m."StoreHouse_id"
        WHERE m."Useyn" = '0'
        ORDER BY m."Code"
    """;

		return this.sqlRunner.getRows(sql, p);
	}

	public List<Map<String, Object>> getInputLotList(Integer jrPk, Integer shipId, String mat_code) {

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("jrPk", jrPk);
		dicParam.addValue("shipId", shipId);
		dicParam.addValue("mat_code", mat_code);

		String sql = """
                with AA as (
                         select 
                         ml."LotNumber"
                         , sum(mlc."OutputQty") as "OutputQty" 
                         from mat_produce mp 
                         inner join job_res jr on jr.id = mp."JobResponse_id"
                         inner join mat_lot_cons mlc on mlc."SourceDataPk" = mp.id and mlc."SourceTableName" ='mat_produce'   
                         inner join mat_lot ml on ml.id = mlc."MaterialLot_id" 
                         where jr.id= :jrPk group by ml."LotNumber" 
                         ), R as (
                             select  mpir.id as mpir_id
                             , mpi.id as mpi_id
                             , mpi."Material_id" as mat_pk
                             , fn_code_name('mat_type', mg."MaterialType") as mat_type_name
                             , mg."Name" as mat_group_name
                             , m."Code" as mat_code
                             , m."Name" as mat_name 
                             , u."Name" as unit_name
                             , mpi."RequestQty" as req_qty
                             , mpi."InputQty" 
                             , to_char(mpi."InputDateTime",'yyyy-MM-dd') as "InputDateTime"
                             , ml."LotNumber"
                             , ml."CurrentStock" as cur_stock
                             , m."ProcessSafetyStock" as proc_safety_stock
                             , mpi."MaterialStoreHouse_id"
                             , mpi."ProcessStoreHouse_id"
                             , mpi."State"
                             , fn_code_name('mat_proc_input_state', mpi."State") as state_name
                             , sh."Name" as "StoreHouseName"
                             from job_res jr 
                             inner join mat_proc_input_req mpir on mpir.id = jr."MaterialProcessInputRequest_id" 
                             inner join mat_proc_input mpi on mpi."MaterialProcessInputRequest_id" =mpir.id
                             inner join material m on m.id = mpi."Material_id"
                             inner join mat_grp mg on mg.id = m."MaterialGroup_id"
                             left join unit u on u.id = m."Unit_id"
                             left join mat_lot ml on ml.id = mpi."MaterialLot_id"
                             left join store_house sh on sh.id=ml."StoreHouse_id"
                             where jr.id =  :jrPk
                             and (:mat_code is null or :mat_code = '' or m."Code" = :mat_code)
                             and mpi.ship_id = :shipId
                            
                          )
                          select R.mat_pk, R.mat_type_name, R.mat_group_name, R.mat_code, R.mat_name
                          , R.mpir_id
                          , R.mpi_id
                          , R.req_qty
                          , R."InputQty" 
                          , R."LotNumber" as lot_number
                          , R.state_name
                          , R.unit_name
                          , R.cur_stock
                          , R."State" 
                          , R."InputDateTime" as start_date
                          , R."StoreHouseName"
                          , COALESCE(AA."OutputQty", 0) as consumed_qty
                          from R 
                          left join AA on AA."LotNumber" = R."LotNumber"
                          order by R."InputDateTime", R."LotNumber"
                	""";

		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
		return items;
	}

	public List<Map<String, Object>> getMaterialProcessInputListByShipId(int mpir_id, int shipId) {

		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue("mpir_id", mpir_id);
		param.addValue("shipId", shipId);

		String sql = """
                select  mpi.id  as mpi_id
                	  ,	mpi."RequestQty" as req_qty
                	  , mpi."InputQty" as input_qty
                	  , mpi."Material_id" as mat_pk
                	  , mpi."MaterialStoreHouse_id" as sh_id
                	  , ml."CurrentStock" as curr_qty
                	  , ml.id as ml_id
                	  , ml."LotNumber"
                	  , ml."EffectiveDate" as eff_date
                from job_res jr 
                inner join mat_proc_input mpi on mpi."MaterialProcessInputRequest_id"  = jr."MaterialProcessInputRequest_id"
                inner join mat_lot ml on ml.id = mpi."MaterialLot_id" 
                where jr."MaterialProcessInputRequest_id" = :mpir_id
                and mpi.ship_id = :shipId
                order by ml."EffectiveDate"
                   """;

		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, param);

		return items;
	}

}

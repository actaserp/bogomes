package mes.app.system.service;

import java.util.List;
import java.util.Map;

import mes.domain.entity.WorkCategoryId;
import mes.domain.repository.WorkCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;

@Service
public class ShiftService {
	
	@Autowired
	SqlRunner sqlRunner;

	@Autowired
	private WorkCategoryRepository workCategoryRepository;

	public List<Map<String, Object>> getShiftList( String shift_name, String spjangcd) {
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        

		dicParam.addValue("spjangcd", spjangcd);
		dicParam.addValue("shift_name", "%" + shift_name + "%");

        String sql = """
				select
				 w."StartTime" as start_time
				 ,w."EndTime" as end_time
				 ,w."JobResId" as job_res_id
				 ,w."JobResNum" as job_res_num
				 ,w."JumunNum" as jumun_num
				 ,w."Desciption" as description
				 ,w."ProjectName" as project_name
				 ,c."Name" as company_name
				 ,w.company_id as company_id
				 from work_category w
				 left join company c on c.id = w.company_id  
				 where w.spjangcd = :spjangcd
				 """;

		if(!shift_name.isEmpty()){
			sql += """
					and w."ProjectName" like :shift_name
					""";
		}

		sql += """ 
				  group by
				 w."StartTime"
				 ,w."EndTime"
				 ,w."JobResId"
				 ,w."JobResNum"
				 ,w."JumunNum"
				 ,w."Desciption"
				 ,w."ProjectName"
				 ,c."Name"
				 ,w.company_id
				 order by w."JobResNum" desc
            """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
	}
	
	public Map<String, Object> getShiftDetail(Integer id) {
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("id", id);
        
        String sql = """
			select s.id 
			, s."Code" 
			, s."Name" 
			, s."StartTime" 
			, s."EndTime" 
			, s."Description" 
			from shift s
			where s.id = :id
            """;

        Map<String, Object> items = this.sqlRunner.getRow(sql, dicParam);
        return items;
	}

	public List<Map<String, Object>> getJob_res_List(String date_from, String date_to){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();

		dicParam.addValue("date_from", date_from);
		dicParam.addValue("date_to", date_to);

		String sql = """
				select jr.id as id
				, jr."WorkOrderNumber" as work_number --작지번호
				, su."JumunNumber" jumun_number --수주번호 (주문번호)
				, su."DueDate" as due_date --납기일
				, su."Company_id" as company_id -- 판매처 id
				, c."Name" as company_name --판매처
				, fn_code_name('mat_type', mg."MaterialType") as mat_type_name --제품구분
				, m."Code" as mat_code  --제품코드
				, m."Name" as mat_name  --제품명
				--, u."Name" as unit_name
				--, jr."OrderQty" as "OrderQty"
				, jr."WorkCenter_id"
				, jr."ShiftCode" as work_group --근무조
				
				, wc."Name" as "WorkcenterName" --워크센터
				, jr."Equipment_id"
				, e."Name" as "EquipmentName"
				, jr."State" --작지상태
				, fn_code_name('job_state', jr."State") as "StateName"
				, jr."SourceDataPk" as suju_id
				, fn_code_name('suju_state', su."State") as "StateName"
				, su."State"
				from job_res jr
				inner join material m on m.id = jr."Material_id"
				inner join mat_grp mg on mg.id = m."MaterialGroup_id"
				inner join suju su on jr."SourceTableName" = 'suju' and su.id = jr."SourceDataPk"
				left join company c on c.id = su."Company_id"
				left join unit u on u.id = m."Unit_id"
				left join shift s on s."Code" = jr."ShiftCode"
				left join work_center wc on wc.id = jr."WorkCenter_id"
				left join equ e on e.id = jr."Equipment_id"
				
				where 1=1
				and jr."SourceTableName" ='suju'
				and su."State" <> 'shipped'  -- 출하된거는 제외!
				and su."DueDate" between to_date(:date_from, 'YYYY-MM-DD') and to_date(:date_to, 'YYYY-MM-DD')
				order by jr."WorkOrderNumber" desc, jr.id
				""";
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
		return items;
	}

	/**
	 * job_res_id + WorkCode 조합 중복 체크
	 * 리스트 중 하나라도 DB에 존재하면 false 반환
	 */
	public boolean validateDuplicate(int jobResId, List<Map<String, String>> workList) {

		for (Map<String, String> item : workList) {

			String code = item.get("code");

			WorkCategoryId pk = new WorkCategoryId(jobResId, code);

			if (workCategoryRepository.existsById(pk)) {
				return false; // 하나라도 중복이면 false
			}
		}

		return true; // 모두 통과하면 true
	}

	public List<Map<String, Object>> findByIdJobResId(int job_res_id){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();

		dicParam.addValue("job_res_id", job_res_id);

		String sql = """
				select "WorkName" as name, "WorkCode" as code from work_category where "JobResId" = :job_res_id;
				""";
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
		return items;

	}

	public List<Map<String, Object>> getCodeInWorkLog(int job_res_id){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();

		dicParam.addValue("job_id", job_res_id);

		String sql = """
				select workcd as code from
				tb_pb201 pb2
				inner join work_category wc on wc."WorkCode" = pb2.workcd and wc."JobResId" = pb2."JobResId"
				where pb2."JobResId" = :job_id
				""";
		List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
		return items;
	}
}

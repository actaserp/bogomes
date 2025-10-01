package mes.app.aftersales.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ASVehicleService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getList(Integer spcmngno, String vechidno) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("spcmngno", spcmngno);
    dicParam.addValue("vechidno", vechidno);
    String sql = """
        select
        a.id,
        mg."Name" as spcmngno,
        m."Name" as "ItemCode",
        a.vechidno,
        a."owner" ,
        a.outdate ,
        a.inputdate ,
        a.regdate,
        a.regno, 
        a.endflag as endflag_code,
        case
        	when endflag = '0' then '출고'
        	else '미출고'
        end as endflag
        from tb_as010 a
        left join mat_grp mg on mg.id =  a.spcmngno::int
        left join material m on a.itemcode::int = m.id
        where 1 = 1
        """;
    if(spcmngno != null && !spcmngno.equals(Integer.valueOf(0))) {
      sql += """
            and a.spcmngno::int = :spcmngno
            """;
    }
    if(vechidno != null && !vechidno.isEmpty()) {
      sql += """
          and LOWER(a.vechidno) LIKE '%' || LOWER(:vechidno) || '%'
            """;
    }

    return this.sqlRunner.getRows(sql, dicParam);
  }

  public Map<String, Object> getDetail(Integer id) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("id", id);

    String sql= """
        select
           a.id,
           a.spcmngno ,
           a.itemcode as "Material_id",
           m."Name" as "ItemCode",
           a.vechidno,
           a.pernm, 
           a."owner" ,
           a.outdate ,
           a.inputdate ,
           a.regdate  ,
           a.endflag ,
           a.fixtext,
           a.regno
           from tb_as010 a
           left join mat_grp mg on mg.id =  a.spcmngno::int
           left join material m on a.itemcode::int = m.id
           where a.id = :id
        """;
    return this.sqlRunner.getRow(sql, param);

  }

  public List<Map<String, Object>> getSearchVechidno(String vechidno, String owner) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("vechidno", vechidno);
    param.addValue("owner", owner);

    String sql= """
      select
        s.vechidno,
        su."CompanyName"           as company_name,
        su."Material_id"           as material_id,
        m."Name"                   as mat_name, 
        m."MaterialGroup_id"       as material_group_id
      from shipment s
      left join suju_head sh on sh.id = s.suju_head_id
      left join suju su     on sh.id = su."SujuHead_id"
      left join material m  on m.id = su."Material_id"
      left join mat_grp mg  on m."MaterialGroup_id" = mg.id
      where 1=1 and s._status ='a'
      """;


    if(vechidno != null && !vechidno.isEmpty()) {
      sql += """
           and LOWER(s.vechidno) LIKE '%' || LOWER(:vechidno) || '%'
           """;
    }

    if(owner != null && !owner.isEmpty()) {
      sql += """
           and su."CompanyName" like '%' || :owner || '%'
           """;
    }

    return this.sqlRunner.getRows(sql, param);
  }
}

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
}

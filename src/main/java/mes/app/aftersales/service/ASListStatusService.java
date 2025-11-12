package mes.app.aftersales.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Service
public class ASListStatusService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getList(Date start, Date end, String vechidno, String cboEndflag) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("start", start);
    param.addValue("end", end);
    param.addValue("vechidno", vechidno);
    param.addValue("cboEndflag", cboEndflag);

    String sql= """
        select
        a.id,
        a.vechidno,
        a."owner" ,
        a.outdate ,
        a.inputdate ,
        a.regno,
        mg."Name" as spcmngno,
        a.itemcode  as "ItemCode",
        a.endflag as endflag_code,
        a.regdate  ,
        case
        	when a.endflag = '0' then '출고'
        	else '미출고'
        end as endflag
        from tb_as010 a 
        left join mat_grp mg on mg.id =  a.spcmngno::int
        where a.regdate between cast(:start as date) and cast(:end as date)
        """;

    if(vechidno != null && !vechidno.isEmpty()) {
      sql += """
          and LOWER(a.vechidno) LIKE '%' || LOWER(:vechidno) || '%'
            """;
    }

    if(cboEndflag != null && !cboEndflag.isEmpty()) {
      sql += """
        and a.endflag = :cboEndflag
            """;
    }

    sql+= """
        order by a.regdate desc;
        """;

    return sqlRunner.getRows(sql,param);
  }
}

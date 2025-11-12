package mes.app.aftersales.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.List;
import java.util.Map;

@Service
public class ASRepairStatusService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getList(Date start, Date end, String vechidno,String cboEndflag) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("start", start);
    param.addValue("end", end);
    param.addValue("vechidno", vechidno);
    param.addValue("cboEndflag", cboEndflag);
    String sql = """
        select
           a.id ,
           a.asid as as_id,
           a.regdate ,
           a.vechregno ,
           b.vechidno,
           b.itemcode ,
           a.fixdate ,
           a.pernm ,
           a.partamt,
           a.workamt ,
           a.amount ,
           a.vamt ,
           a.totamt ,
           case
              when b.endflag = '0' then '출고'
              else '미출고'
           end as endflag,
           c.id as detail_id,
           c.fixtext ,
           c.partgroup ,
           c.partqty ,
           c.uamt ,
           c.totamt    as detail_totamt,
           c.workpay   as detail_workpay
        from tb_as011 a		-- head
        left join tb_as010 b on b.id = a.asid
        left join tb_as012 c on c.repid = a.id 	-- detail
        where 1=1
        and  a.regdate between :start and :end
        """;
    if(vechidno != null && !vechidno.isEmpty()) {
      sql += """
          and LOWER(a.vechidno) LIKE '%' || LOWER(:vechidno) || '%'
            """;
    }
    if(cboEndflag != null && !cboEndflag.isEmpty()) {
      sql += """
        and b.endflag = :cboEndflag
            """;
    }
    return sqlRunner.getRows(sql,param);
  }

}

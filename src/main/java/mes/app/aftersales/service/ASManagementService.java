package mes.app.aftersales.service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ASManagementService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getList(String regno) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("regno", regno);
    String sql= """
        select
        a.id,
        a.asid as as_id,
        a.regdate, 
        a.fixdate,
        m."Name" as itemcode,
        b.regno,
        a.fixtext ,
        a.pernm
        from tb_as011 a
        left join tb_as010 b on a.asid = b.id 
        left join material m on b.itemcode::int = m.id
        where 1=1
        -- and a.regdate between :start and :end
        """;
    if(regno != null && !regno.isEmpty()) {
      sql += """
           and  b.regno like '%' || :regno || '%'
           """;
    }
    return this.sqlRunner.getRows(sql, dicParam);
  }

  public Map<String, Object> getDetail(Integer id) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("id", id);
    String sql= """
        select
        a.id,
        a.asid as as_id,
        b.spcmngno ,
        b.itemcode as "Material_id",
        m."Name" as itemcode,
        a.vechidno,
        a.pernm,
        a.regdate,
        a.fixdate,
        a.endflag ,
        a.fixtext,
        a.partgroup ,
        a.partqty ,
        a.uamt,
        a.totamt,
        a.workpay,
        a.mileage ,
        b."owner",
        b.regno,
        b.vechidno
        from tb_as011 a
        left join tb_as010 b on a.asid = b.id
        left join mat_grp mg on mg.id = b.spcmngno::int
        left join material m on b.itemcode::int = m.id
        where a.id = :id
        """;
    return this.sqlRunner.getRow(sql, dicParam);
  }
}

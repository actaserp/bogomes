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
           a.id ,
           a.asid as as_id,
           a.regdate ,
           a.vechregno ,
           a.fixdate ,
           a.pernm ,
           a.partamt,
           a.workamt ,
           a.amount ,
           a.vamt ,
           a.totamt ,
           b.itemcode
           from tb_as011 a
           left join tb_as010 b on b.id = a.asid
           where 1=1
        """;
    if(regno != null && !regno.isEmpty()) {
      sql += """
           and  a.vechregno like '%' || :regno || '%'
           """;
    }
    return this.sqlRunner.getRows(sql, dicParam);
  }

  public Map<String, Object> getDetail(Integer id) {
    MapSqlParameterSource dicParam = new MapSqlParameterSource();
    dicParam.addValue("id", id);
    String head_sql= """
        select
             a.id ,
             a.asid as as_id,
             a.regdate ,
             a.vechregno ,
             a.fixdate ,
             a.mileage  ,
             a.pernm ,
             a.partamt,
             a.workamt ,
             a.amount ,
             a.vamt ,
             a.totamt as h_totamt,
             b.itemcode
          from tb_as011 a
          left join tb_as010 b on a.asid = b.id
          where a.id = :id
        """;

    String detail_sql = """
        select
           id,
           fixtext ,
           partgroup ,
           partqty ,
           uamt ,
           totamt ,
           workpay 
           from tb_as012
           where repid = :id;
        """;
    Map<String, Object> head = this.sqlRunner.getRow(head_sql, dicParam);

    // 디테일 여러 건
    List<Map<String, Object>> items = this.sqlRunner.getRows(detail_sql, dicParam);

    head.put("items", items);  // ← 같이 리턴
    return head;
  }
}

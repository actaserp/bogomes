package mes.app.sales.service;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SujuDeliveryStatusService {

  @Autowired
  SqlRunner sqlRunner;


  public List<Map<String, Object>> getList(LocalDate start, LocalDate  end, String company) {
    MapSqlParameterSource param = new MapSqlParameterSource();
    param.addValue("start", start);
    param.addValue("end", end);
    param.addValue("company", company);

    String sql = """
            SELECT
                  h.id,
                  c."Name" AS com_name,
                  d."JumunDate",
                  h."DeliveryDate",
                  h.contractnm,
                  d.id AS suju_id,
                  m."CustomerBarcode",
                  m."Name" AS mat_name,
                  d."SujuQty",
                  COALESCE(s.ship_oty, 0) AS ship_oty,
                  GREATEST((d."SujuQty" - COALESCE(s.ship_oty, 0)), 0) AS "SujuQty3",
                  s.devdate,
                  s.vechidno,
                  CASE
                    WHEN s.devdate IS NOT NULL AND h."JumunDate" IS NOT NULL THEN
                      DATE_PART('day', s.devdate::timestamp - d."JumunDate"::timestamp)
                    ELSE NULL
                  END AS "deliveryDays"
                FROM suju d
                LEFT JOIN suju_head h ON h.id = d."SujuHead_id"
                LEFT JOIN (
                  SELECT
                    "SourceDataPk",                     -- 🔁 suju.id에 해당
                    SUM("Qty") AS ship_oty,             -- 출고 수량 합계
                    MAX(devdate) AS devdate,            -- 가장 최근 출고일
                    MAX(vechidno) AS vechidno           -- 대표 차대번호
                  FROM shipment
                  GROUP BY "SourceDataPk"
                ) s ON s."SourceDataPk" = d.id          -- ✅ 핵심 조인 변경
                LEFT JOIN company c ON c.id = h."Company_id"
                LEFT JOIN material m ON m.id = d."Material_id"
                WHERE 1=1
                  AND h."DeliveryDate" BETWEEN :start AND :end
                   
        """;
    if (StringUtils.isEmpty(company)==false)
      sql+="and upper(c.\"Name\") like concat('%%',upper(:company),'%%')";

    sql+= """
        order by h."DeliveryDate", d.id;
        """;

//    log.info("수주별납품현황 SQL: {}", sql);
//    log.info("수주별납품현황 데이터: {}", param.getValues());
    return this.sqlRunner.getRows(sql, param);
  }
}

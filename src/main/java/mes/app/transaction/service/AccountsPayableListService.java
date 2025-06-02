package mes.app.transaction.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AccountsPayableListService {
  @Autowired
  SqlRunner sqlRunner;

  // 미지급현황 리스트 조회
  public List<Map<String, Object>> getPayableList(String start, String end, Integer company, String spjangcd) {

    MapSqlParameterSource paramMap = new MapSqlParameterSource();

    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    LocalDate startDate = LocalDate.parse(start, inputFormatter);
    LocalDate endDate = LocalDate.parse(end, inputFormatter);

    String formattedStart = startDate.format(dbFormatter);
    String formattedEnd = endDate.format(dbFormatter);

    YearMonth baseYm = YearMonth.from(startDate);
    String baseYmStr = baseYm.format(DateTimeFormatter.ofPattern("yyyyMM"));

    paramMap.addValue("start", formattedStart);
    paramMap.addValue("end", formattedEnd);
    paramMap.addValue("baseYm", baseYmStr);
    paramMap.addValue("spjangcd", spjangcd);

    if (company != null) {
      paramMap.addValue("company", company);
    }

    String sql = """
        WITH lastym AS (
            SELECT cltcd, MAX(yyyymm) AS yyyymm
            FROM tb_yearamt
            WHERE yyyymm < :baseYm
              AND ioflag = '1'
              AND spjangcd = :spjangcd
            GROUP BY cltcd
        ),
        last_amt AS (
            SELECT y.cltcd, y.yearamt, y.yyyymm
            FROM tb_yearamt y
            JOIN lastym m ON y.cltcd = m.cltcd AND y.yyyymm = m.yyyymm
            WHERE y.ioflag = '1'
              AND y.spjangcd = :spjangcd
        ),
        post_close_txns AS (
            SELECT
                c.id AS cltcd,
                SUM(COALESCE(i.totalamt, 0)) AS extra_purchase,
                SUM(COALESCE(b.accout, 0)) AS extra_payment
            FROM company c
            LEFT JOIN tb_invoicement i ON c.id = i.cltcd
                AND i.misdate BETWEEN 
                    TO_CHAR((SELECT TO_DATE(MAX(yyyymm), 'YYYYMM') + interval '1 month' FROM last_amt), 'YYYYMMDD')
                    AND TO_CHAR(TO_DATE(:start, 'YYYYMMDD') - interval '1 day', 'YYYYMMDD')
                AND i.spjangcd = :spjangcd
            LEFT JOIN tb_banktransit b ON c.id = b.cltcd
                AND b.trdate BETWEEN 
                    TO_CHAR((SELECT TO_DATE(MAX(yyyymm), 'YYYYMM') + interval '1 month' FROM last_amt), 'YYYYMMDD')
                    AND TO_CHAR(TO_DATE(:start, 'YYYYMMDD') - interval '1 day', 'YYYYMMDD')
                AND b.ioflag = '1'
                AND b.spjangcd = :spjangcd
            GROUP BY c.id
        ),
        uncalculated_txns AS (
            SELECT
                c.id AS cltcd,
                SUM(COALESCE(i.totalamt, 0)) AS total_purchase,
                SUM(COALESCE(b.accout, 0)) AS total_payment
            FROM company c
            LEFT JOIN tb_invoicement i ON c.id = i.cltcd
                AND i.misdate < :start
                AND i.spjangcd = :spjangcd
            LEFT JOIN tb_banktransit b ON c.id = b.cltcd
                AND b.trdate < :start
                AND b.ioflag = '1'
                AND b.spjangcd = :spjangcd
            WHERE NOT EXISTS (
                SELECT 1 FROM last_amt y WHERE y.cltcd = c.id
            )
            GROUP BY c.id
        ),
        final_prev_amt AS (
            SELECT
                y.cltcd,
                y.yearamt + COALESCE(p.extra_purchase, 0) - COALESCE(p.extra_payment, 0) AS prev_amt
            FROM last_amt y
            LEFT JOIN post_close_txns p ON y.cltcd = p.cltcd
            UNION
            SELECT
                u.cltcd,
                COALESCE(u.total_purchase, 0) - COALESCE(u.total_payment, 0)
            FROM uncalculated_txns u
        ),
        purchase_amt AS (
            SELECT cltcd, SUM(totalamt) AS purchase
            FROM tb_invoicement
            WHERE misdate BETWEEN :start AND :end
              AND spjangcd = :spjangcd
            GROUP BY cltcd
        ),
        payment_amt AS (
            SELECT cltcd, SUM(accout) AS payment
            FROM tb_banktransit
            WHERE trdate BETWEEN :start AND :end
              AND ioflag = '1'
              AND spjangcd = :spjangcd
            GROUP BY cltcd
        )
        SELECT
            m.id AS cltcd,
            m."Name" AS clt_name,
            COALESCE(f.prev_amt, 0) AS payable,
            COALESCE(p.purchase, 0) AS purchase,
            COALESCE(b.payment, 0) AS "AmountPaid",
            COALESCE(f.prev_amt, 0) + COALESCE(p.purchase, 0) - COALESCE(b.payment, 0) AS balance
        FROM company m
        LEFT JOIN final_prev_amt f ON m.id = f.cltcd
        LEFT JOIN purchase_amt p ON m.id = p.cltcd
        LEFT JOIN payment_amt b ON m.id = b.cltcd
        WHERE COALESCE(f.prev_amt, 0) + COALESCE(p.purchase, 0) - COALESCE(b.payment, 0) <> 0
        """;

    if (company != null) {
      sql += " AND m.id = :company ";
    }

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("미지급 현황 SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;
  }

  // 미지급현황 상세 리스트 조회
  public List<Map<String, Object>> getPayableDetailList(String start, String end, String company, String spjangcd) {
    MapSqlParameterSource paramMap = new MapSqlParameterSource();
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter dbFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    LocalDate startDate = LocalDate.parse(start, inputFormatter);
    LocalDate endDate = LocalDate.parse(end, inputFormatter);

    String formattedStart = startDate.format(dbFormatter);
    String formattedEnd = endDate.format(dbFormatter);
    String baseDate = startDate.minusDays(1).format(dbFormatter);
    String baseYm = YearMonth.from(startDate).format(DateTimeFormatter.ofPattern("yyyyMM"));

    paramMap.addValue("start", formattedStart);
    paramMap.addValue("end", formattedEnd);
    paramMap.addValue("baseDate", baseDate);
    paramMap.addValue("baseYm", baseYm);
    paramMap.addValue("company", Integer.valueOf(company));
    paramMap.addValue("spjangcd", spjangcd);

    String sql = """
        WITH lastym AS (
            SELECT cltcd, MAX(yyyymm) AS yyyymm
            FROM tb_yearamt
            WHERE yyyymm < :baseYm
              AND ioflag = '1'
              AND cltcd = :company
              AND spjangcd = :spjangcd
            GROUP BY cltcd
        ),
        last_amt AS (
            SELECT y.cltcd, y.yearamt, y.yyyymm
            FROM tb_yearamt y
            JOIN lastym m ON y.cltcd = m.cltcd AND y.yyyymm = m.yyyymm
            WHERE y.ioflag = '1'
              AND y.spjangcd = :spjangcd
        ),
        post_txns AS (
            SELECT
                c.id AS cltcd,
                SUM(COALESCE(i.totalamt, 0)) AS extra_purchase,
                SUM(COALESCE(b.accout, 0)) AS extra_payment
            FROM company c
            LEFT JOIN tb_invoicement i ON c.id = i.cltcd
                AND i.misdate BETWEEN 
                    TO_CHAR((SELECT TO_DATE(MAX(yyyymm), 'YYYYMM') + interval '1 month' FROM last_amt), 'YYYYMMDD')
                    AND TO_CHAR(TO_DATE(:start, 'YYYYMMDD') - interval '1 day', 'YYYYMMDD')
                AND i.spjangcd = :spjangcd
            LEFT JOIN tb_banktransit b ON c.id = b.cltcd
                AND b.trdate BETWEEN 
                    TO_CHAR((SELECT TO_DATE(MAX(yyyymm), 'YYYYMM') + interval '1 month' FROM last_amt), 'YYYYMMDD')
                    AND TO_CHAR(TO_DATE(:start, 'YYYYMMDD') - interval '1 day', 'YYYYMMDD')
                AND b.ioflag = '1'
                AND b.spjangcd = :spjangcd
            WHERE c.id = :company
            GROUP BY c.id
        ),
        uncalculated_txns AS (
            SELECT
                c.id AS cltcd,
                SUM(COALESCE(i.totalamt, 0)) AS total_purchase,
                SUM(COALESCE(b.accout, 0)) AS total_payment
            FROM company c
            LEFT JOIN tb_invoicement i ON c.id = i.cltcd
                AND i.misdate < :start
                AND i.spjangcd = :spjangcd
            LEFT JOIN tb_banktransit b ON c.id = b.cltcd
                AND b.trdate < :start
                AND b.ioflag = '1'
                AND b.spjangcd = :spjangcd
            WHERE NOT EXISTS (
                SELECT 1 FROM last_amt y WHERE y.cltcd = c.id
            ) AND c.id = :company
            GROUP BY c.id
        ),
        final_prev_amt AS (
            SELECT
                y.cltcd,
                y.yearamt + COALESCE(p.extra_purchase, 0) - COALESCE(p.extra_payment, 0) AS amount
            FROM last_amt y
            LEFT JOIN post_txns p ON y.cltcd = p.cltcd
            UNION
            SELECT
                u.cltcd,
                COALESCE(u.total_purchase, 0) - COALESCE(u.total_payment, 0)
            FROM uncalculated_txns u
        ),
        union_data_raw AS (
            -- 전잔액
            SELECT
                c.id AS cltcd,
                c."Name" AS comp_name,
                TO_DATE(:baseDate, 'YYYYMMDD') AS date,
                '전잔액' AS summary,
                f.amount AS amount,
                NULL::text AS itemnm,
                NULL::text AS misgubun,
                NULL::text AS iotype,
                NULL::text AS banknm,
                NULL::text AS accnum,
                NULL::text AS eumnum,
                NULL::text AS eumtodt,
                NULL::text AS tradenm,
                NULL::numeric AS accout,
                NULL::numeric AS totalamt,
                NULL::text AS memo,
                NULL::text AS remark1,
                0 AS remaksseq
            FROM company c
            JOIN final_prev_amt f ON c.id = f.cltcd
            WHERE c.id = :company AND c.spjangcd = :spjangcd
            UNION ALL
            -- 매입
            SELECT
                s.cltcd,
                c."Name" AS comp_name,
                TO_DATE(s.misdate, 'YYYYMMDD') AS date,
                '매입' AS summary,
                NULL::numeric AS amount,
                CONCAT(
                    MAX(CASE WHEN d.misseq::int = 1 THEN d.itemnm END),
                    CASE WHEN COUNT(DISTINCT d.itemnm) > 1 THEN ' 외 ' || (COUNT(DISTINCT d.itemnm) - 1) || '건' ELSE '' END
                ) AS itemnm,
                sc."Value" AS misgubun,
                NULL::text AS iotype,
                NULL::text AS banknm,
                NULL::text AS accnum,
                NULL::text AS eumnum,
                NULL::text AS eumtodt,
                NULL::text AS tradenm,
                NULL::numeric AS accout,
                s.totalamt,
                NULL::text AS memo,
                s.remark1,
                2 AS remaksseq
            FROM tb_invoicement s
            LEFT JOIN tb_invoicdetail d ON s.misdate = d.misdate AND s.misnum = d.misnum AND s.spjangcd = d.spjangcd
            LEFT JOIN sys_code sc ON sc."Code" = s.misgubun::text
            JOIN company c ON c.id = s.cltcd 
            WHERE s.misdate BETWEEN :start AND :end
              AND s.cltcd = :company
              AND s.spjangcd = :spjangcd
            GROUP BY s.cltcd, c."Name", s.misdate, s.misnum, s.totalamt, s.misgubun, sc."Value", s.remark1
            UNION ALL
            -- 지급
            SELECT
                b.cltcd,
                c."Name" AS comp_name,
                TO_DATE(b.trdate, 'YYYYMMDD') AS date,
                '지급' AS summary,
                NULL::numeric AS amount,
                NULL::text AS itemnm,
                NULL::text AS misgubun,
                sc."Value" AS iotype,
                b.banknm,
                b.accnum,
                b.eumnum,
                TO_CHAR(TO_DATE(NULLIF(b.eumtodt, ''), 'YYYYMMDD'), 'YYYY-MM-DD') AS eumtodt,
                tt.tradenm,
                b.accout,
                NULL::numeric AS totalamt,
                b.memo,
                b.remark1,
                1 AS remaksseq
            FROM tb_banktransit b
            JOIN company c ON c.id = b.cltcd 
            LEFT JOIN sys_code sc ON sc."Code" = b.iotype
            LEFT JOIN tb_trade tt ON tt.trid = b.trid AND tt.spjangcd = b.spjangcd
            WHERE TO_DATE(b.trdate, 'YYYYMMDD') BETWEEN TO_DATE(:start, 'YYYYMMDD') AND TO_DATE(:end, 'YYYYMMDD') 
              AND b.cltcd = :company
              AND b.spjangcd = :spjangcd
              AND b.ioflag = '1'
        ),
        union_data AS (
            SELECT * FROM union_data_raw
        )
        SELECT
            x.cltcd,
            x.comp_name,
            x.date,
            x.summary,
            COALESCE(x.amount, x.totalamt, x.accout) AS total_amount,
            SUM(
              COALESCE(x.amount, 0) + COALESCE(x.totalamt, 0) - COALESCE(x.accout, 0)
            ) OVER (
              PARTITION BY x.cltcd
              ORDER BY x.date, x.remaksseq, x.itemnm
              ROWS UNBOUNDED PRECEDING
            ) AS balance,        
            x.accout,
            x.totalamt,
            x.itemnm,
            x.misgubun,
            x.iotype,
            x.banknm,
            x.accnum,
            x.eumnum,
            x.eumtodt,
            x.tradenm,
            x.memo,
            x.remark1
        FROM union_data x
        ORDER BY x.cltcd, x.date, x.remaksseq
        """;

    List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
//    log.info("미수금 현황 상세 read SQL: {}", sql);
//    log.info("SQL Parameters: {}", paramMap.getValues());
    return items;
  }

}

package mes.app.mobile.Service;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class CommuteCurrentService {
    @Autowired
    SqlRunner sqlRunner;

    public List<Map<String, Object>> getUserInfo(String username, String workcd, String workDate) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("username", username);

        if(workDate != null) {
            String dayOnly = workDate.substring(8, 10);  // dd 부분만 추출
            String workYm  = workDate.substring(0, 7).replace("-", ""); // yyyyMM
            dicParam.addValue("workdate", dayOnly);
            dicParam.addValue("workym", workYm);
        }

        String sql = """
                SELECT
                t.workym,
                t.workday,
                t.personid,
                t.worknum,
                t.holiyn,
                t.workyn,
                t.workcd,
                sc."Value" as worknm,
                t.starttime,
                t.endtime,
                t.worktime,
                t.nomaltime,
                t.overtime,
                t.nighttime,
                t.holitime,
                t.jitime,
                t.jotime,
                t.yuntime,
                t.abtime,
                t.bantime,
                t.adttime01,
                t.adttime02,
                t.adttime03,
                t.adttime04,
                t.adttime05,
                t.adttime06,
                t.adttime07,
                t.remark,
                t.fixflag,
                a.first_name,
                TRIM(BOTH ', ' FROM (
                  CASE WHEN t.jitime = 1 THEN '지각, ' ELSE '' END ||
                  CASE WHEN t.jotime = 1 THEN '조퇴, ' ELSE '' END ||
                  CASE WHEN t.yuntime = 1 THEN '연차, ' ELSE '' END ||
                  CASE WHEN t.abtime = 1 THEN '결근, ' ELSE '' END ||
                  CASE WHEN t.bantime = 1 THEN '반차, ' ELSE '' END
                )) AS status_text
            FROM tb_pb201 t
            LEFT JOIN auth_user a ON a.personid = t.personid
            LEFT JOIN person p ON p.id = a.personid
            LEFT JOIN sys_code sc ON t.workcd = sc."Code" AND sc."CodeType" = 'class_work'
            WHERE 1=1
              AND a.username = :username
        		""";

        if (workDate != null && !workDate.isEmpty()) {
            sql += """
              AND t.workym = :workym
              AND t.workday = :workdate
              """;
        }

        sql += """
               ORDER BY t.workym DESC, t.workday DESC
               """;



        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

    public List<Map<String, Object>> getUserInfo2(String username, String workcd, String searchFromDate, String searchToDate) {

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("username", username);

        if(searchFromDate != null) {
            String fromDayOnly = searchFromDate.substring(8, 10);  // dd 부분만 추출
            String fromWorkYm  = searchFromDate.substring(0, 7).replace("-", ""); // yyyyMM
            dicParam.addValue("workFromDate", fromDayOnly);
            dicParam.addValue("workFromYm", fromWorkYm);
        }
        if(searchToDate != null) {
            String fromDayOnly = searchToDate.substring(8, 10);  // dd 부분만 추출
            String fromWorkYm  = searchToDate.substring(0, 7).replace("-", ""); // yyyyMM
            dicParam.addValue("workToDate", fromDayOnly);
            dicParam.addValue("workToYm", fromWorkYm);
        }

        String sql = """
                SELECT
                t.workym,
                t.workday,
                t.personid,
                t.worknum,
                t.holiyn,
                t.workyn,
                t.workcd,
                sc."Value" as worknm,
                t.starttime,
                t.endtime,
                t.worktime,
                t.nomaltime,
                t.overtime,
                t.nighttime,
                t.holitime,
                t.jitime,
                t.jotime,
                t.yuntime,
                t.abtime,
                t.bantime,
                t.adttime01,
                t.adttime02,
                t.adttime03,
                t.adttime04,
                t.adttime05,
                t.adttime06,
                t.adttime07,
                t.remark,
                t.fixflag,
                a.first_name
            FROM tb_pb201 t
            LEFT JOIN auth_user a ON a.personid = t.personid
            LEFT JOIN person p ON p.id = a.personid
            LEFT JOIN sys_code sc ON t.workcd = sc."Code" AND sc."CodeType" = 'class_work'
            WHERE 1=1
              AND a.username = :username
        		""";

        if (searchFromDate != null && !searchFromDate.isEmpty()) {
            String fromYmd = searchFromDate.replace("-", ""); // yyyyMMdd
            dicParam.addValue("fromYmd", fromYmd);
        }

        if (searchToDate != null && !searchToDate.isEmpty()) {
            String toYmd = searchToDate.replace("-", ""); // yyyyMMdd
            dicParam.addValue("toYmd", toYmd);
        }

        if (searchFromDate != null && !searchFromDate.isEmpty() &&
                searchToDate != null && !searchToDate.isEmpty()) {
            sql += """
                  AND (t.workym || LPAD(t.workday, 2, '0')) BETWEEN :fromYmd AND :toYmd
                  """;
        }

        if(workcd != null && !workcd.isEmpty()){
            dicParam.addValue("workcd", workcd);
            sql += """
                    AND t.workcd = :workcd
                    """;
        }

        sql += """
               ORDER BY t.workym DESC, t.workday DESC
               """;



        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

}

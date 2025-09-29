package mes.app.clock.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WorkManagementService {

  @Autowired
  SqlRunner sqlRunner;

  public List<Map<String, Object>> getWorkManagementList(String start, String end, String depart) {
    MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("date_from", start)
            .addValue("date_to", end);

    String dep = (depart == null) ? "" : depart.trim();
    Integer departId = null;
    if (!dep.isEmpty()) {
      try {
        int v = Integer.parseInt(dep);
        if (v != 0) departId = v;
      } catch (NumberFormatException ignore) {}
    }

    StringBuilder sql = new StringBuilder();
    sql.append("""
        select
            p.id as person_id,
            p."Name" as person_name,
            tp.workcd,
            fn_code_name('class_work', tp.workcd) as work,
            round(
                (
                    extract(epoch from (
                        to_timestamp(tp.workym || lpad(tp.workday, 2, '0') || tp.endtime, 'YYYYMMDDHH24:MI')
                      - to_timestamp(tp.workym || lpad(tp.workday, 2, '0') || tp.starttime, 'YYYYMMDDHH24:MI')
                    )) / 3600.0
                )::numeric
            , 2) as total_hours,
             to_date(tp.workym || lpad(tp.workday, 2, '0'), 'YYYYMMDD') as work_date
        from tb_pb201 tp
        join person p on p.id = tp.personid
        where to_date(tp.workym || lpad(tp.workday, 2, '0'), 'YYYYMMDD')
              between cast(:date_from as date) and cast(:date_to as date)
    """);

    if (departId != null) {
      sql.append("  and p.id = :depart_id\n");
      params.addValue("depart_id", departId, java.sql.Types.INTEGER);
    } else if (!dep.isEmpty()) {
      sql.append("  and p.\"Name\" ilike :depart_like\n");
      params.addValue("depart_like", "%" + dep + "%", java.sql.Types.VARCHAR);
    }

    sql.append(" order by p.\"Name\", tp.workym, tp.workday, tp.workcd ");

    return sqlRunner.getRows(sql.toString(), params);
  }



  public List<Map<String, Object>> defectsList(String searchDate, Integer personId) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    StringBuilder sql = new StringBuilder();
    params.addValue("personId", personId);

    sql.append("""
       select 
         tp.id,
         tp.workcd as code,
         sc."Value" as value,
         tp.starttime,
         tp.endtime
       from tb_pb201 tp
       left join sys_code sc
         on sc."Code" = tp.workcd
        and sc."CodeType" = 'class_work'
       where 1=1
       and tp.personid = :personId 
    """);

    if (searchDate != null && !searchDate.isEmpty()) {
      sql.append(" and (tp.workym || lpad(tp.workday, 2, '0')) = :workDate \n");
      params.addValue("workDate", searchDate.replace("-", "")); // YYYYMMDD
    }

    sql.append(" order by tp.id desc ");

    return sqlRunner.getRows(sql.toString(), params);
  }

}

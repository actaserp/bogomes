package mes.app.clock.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.sql.Types;
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
        .addValue("date_to",   end);

    // depart 파싱: 비었으면 필터 없음, 숫자면 id=조건, 그 외는 Name ilike 조건
    String dep = (depart == null) ? "" : depart.trim();
    Integer departId = null;
    if (!dep.isEmpty()) {
      try {
        int v = Integer.parseInt(dep);
        if (v != 0) departId = v; // "0"은 필터 없음으로 간주
      } catch (NumberFormatException ignore) { /* 숫자 아님 */ }
    }

    StringBuilder sql = new StringBuilder();
    sql.append("""
    with base as (
      select
        p.id as person_id,
        p."Name" as person_name,
        p."Code" as person_code,
        p."ShiftCode" as shift_name,
        tp.workcd,
        fn_code_name('class_work', tp.workcd) as work,
        to_date(tp.workym::text || lpad(tp.workday::text, 2, '0'), 'YYYYMMDD') as work_date,
        nullif(tp.starttime, '')::time as starttime,  -- varchar(5) → time
        nullif(tp.endtime,   '')::time as endtime
      from person p
      join tb_pb201 tp on p.id = tp.personid
      where to_date(tp.workym::text || lpad(tp.workday::text, 2, '0'), 'YYYYMMDD')
            between cast(:date_from as date) and cast(:date_to as date)
  """);

    // 동적 필터 추가
    if (departId != null) {
      sql.append("        and p.id = :depart_id\n");
      params.addValue("depart_id", departId, java.sql.Types.INTEGER);
    } else if (!dep.isEmpty()) {
      sql.append("        and p.\"Name\" ilike :depart_like\n");
      params.addValue("depart_like", "%" + dep + "%", java.sql.Types.VARCHAR);
    }

    sql.append("""
    ),
    seg as (
      select
        *,
        case when starttime is not null then (work_date + starttime) end as start_ts,
        case
          when starttime is not null and endtime is not null then
            (work_date + endtime)
            + case when endtime < starttime then interval '1 day' else interval '0' end
        end as end_ts
      from base
    ),
    seg2 as (
      select
        person_id, person_name, person_code, shift_name,
        workcd, work, work_date,
        start_ts, end_ts,
        case when start_ts is not null and end_ts is not null
          then greatest(extract(epoch from (end_ts - start_ts))::numeric, 0::numeric)
          else 0::numeric
        end as seg_seconds
      from seg
    )
    select
      person_id, person_name, person_code, shift_name,
      workcd, work, work_date,
      to_char(min(start_ts), 'HH24:MI') as first_in,
      to_char(max(end_ts),   'HH24:MI') as last_out,
      to_char(make_interval(secs => sum(seg_seconds)::double precision), 'HH24:MI') as total_worktime, -- "06:30"
      round(sum(seg_seconds) / 3600, 2) as total_hours                                                    -- 6.50
    from seg2
    group by
      person_id, person_name, person_code, shift_name,
      workcd, work, work_date
    order by person_name, work_date, work
  """);

//    log.info("작업자근무관리 read SQL: {}", sql.toString());
//    log.info("작업자근무관리 Parameters: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }

}

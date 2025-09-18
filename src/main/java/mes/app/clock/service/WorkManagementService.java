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
    -- 점심 구간(12:00~13:00) 정의
    lunch as (
      select
        person_id,
        work_date,
        (work_date + time '12:00') as lunch_start,
        (work_date + time '13:00') as lunch_end
      from base
    ),
    seg2 as (
      select
        s.person_id, s.person_name, s.person_code, s.shift_name,
        s.workcd, s.work, s.work_date,
        s.start_ts, s.end_ts,
        -- 원래 구간 초
        case when s.start_ts is not null and s.end_ts is not null
          then greatest(extract(epoch from (s.end_ts - s.start_ts))::numeric, 0::numeric)
          else 0::numeric
        end as seg_seconds,
        -- 점심 겹침 초(없으면 0)
        greatest(
          extract(
            epoch from (
              least(s.end_ts, l.lunch_end) - greatest(s.start_ts, l.lunch_start)
            )
          )::numeric,
          0::numeric
        ) as lunch_overlap_seconds
      from seg s
      left join lunch l
        on l.person_id = s.person_id
       and l.work_date = s.work_date
    )
    select
      person_id, person_name, person_code, shift_name,
      workcd, work, work_date,
      to_char(min(start_ts), 'HH24:MI') as first_in,
      to_char(max(end_ts),   'HH24:MI') as last_out,
      to_char(make_interval(secs => sum( (seg_seconds - lunch_overlap_seconds) )::double precision), 'HH24:MI') as total_worktime,
      round(sum(seg_seconds - lunch_overlap_seconds) / 3600, 2) as total_hours
    from seg2
    group by
      person_id, person_name, person_code, shift_name,
      workcd, work, work_date
    order by person_name, work_date, work
    """);

    // log.info("작업자근무관리 read SQL: {}", sql.toString());
    // log.info("작업자근무관리 Parameters: {}", params.getValues());
    return sqlRunner.getRows(sql.toString(), params);
  }


  public List<Map<String, Object>> defectsList() {
    MapSqlParameterSource params = new MapSqlParameterSource();
    String sql= """
       select 
       "Code" as code,
       "Value" as value
       from sys_code where "CodeType" ='class_work'; 
        """;
    return sqlRunner.getRows(sql.toString(), params);
  }
}

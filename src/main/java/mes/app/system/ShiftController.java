package mes.app.system;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import javassist.runtime.Desc;
import mes.app.util.UtilClass;
import mes.domain.entity.WorkCategory;
import mes.domain.entity.WorkCategoryId;
import mes.domain.repository.WorkCategoryRepository;
import mes.domain.repository.commute.TB_PB201Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mes.app.system.service.ShiftService;
import mes.domain.entity.Shift;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.ShiftRepository;

@RestController
@RequestMapping("/api/system/shift")
public class ShiftController {
	
	@Autowired
	ShiftRepository shiftRepository;
	
	@Autowired
	private ShiftService shiftService;

	@Autowired
	private WorkCategoryRepository workCategoryRepository;

	@Autowired
	private TB_PB201Repository tbPb201Repository;
	
	@GetMapping("/read")
	public AjaxResult getShiftList(
			@RequestParam(value="shift_name", required=false) String shift_name,
			@RequestParam(value ="spjangcd") String spjangcd,
			HttpServletRequest request) {
		
		List<Map<String, Object>> items = this.shiftService.getShiftList(shift_name,spjangcd);
   		
        AjaxResult result = new AjaxResult();
        result.data = items;        				
        
		return result;
	}
	
	
	@GetMapping("/detail")
	public AjaxResult getWorkCodeList(
			@RequestParam("job_res_id") int job_res_id,
			HttpServletRequest request) {

        AjaxResult result = new AjaxResult();

        result.data = shiftService.findByIdJobResId(job_res_id);
        
		return result;
	}

	@PostMapping("/save")
	@Transactional
	public AjaxResult saveShift(
			@RequestParam(value="jumun_num") String jumun_num,
			@RequestParam(value="work_order") String work_order,
			@RequestParam(value="job_res_id") int job_res_id,
			@RequestParam(value="ProjectName") String ProjectName,
			@RequestParam("work_type_json") String work_type_json,
			@RequestParam(value="StartTime") String StartTime,
			@RequestParam(value="EndTime") String EndTime,
			@RequestParam(value="Description", required=false) String Description,
			@RequestParam(value ="spjangcd") String spjangcd,
			@RequestParam(value ="company_id") Integer company_id,
			@RequestParam(value ="save_flag") boolean save_flag,
			HttpServletRequest request,
			Authentication auth) {
		
		User user = (User)auth.getPrincipal();

		AjaxResult result = new AjaxResult();


		//json파싱
		List<Map<String, String>> workList = UtilClass.parseWorkTypeJson(work_type_json);
		//작업지시id & WorkCode 조합이 중복되는지 체크
		/*if(!shiftService.validateDuplicate(job_res_id, workList)){
			result.message = "같은 작업에 대해 중복되는 코드가 존재합니다.";
			result.success = false;
			return result;
		}*/

		if(!save_flag){
			/***추가***/

			// 같은 작업지시 추가 -> 안됨
			//중복된 작업지시 거부 (job_res_id 로 판정)
			List<WorkCategory> byIdJobResId = workCategoryRepository.findByIdJobResId(job_res_id);

			if(!byIdJobResId.isEmpty()){
				result.success = false;
				result.message = "이미 등록된 작업지시 프로젝트 입니다.";
				return result;
			}

			for(Map<String, String> work : workList){
				//비지니스 로직 시작
				WorkCategory entity = new WorkCategory();

				WorkCategoryId pk = new WorkCategoryId(job_res_id, work.get("code"));
				entity.setId(pk);

				entity.setStartTime(StartTime);
				entity.setEndTime(EndTime);
				entity.setWorkName(work.get("name"));
				entity.setJobResNum(work_order);
				entity.setJumunNum(jumun_num);
				entity.setDescription(Description);
				entity.setProjectName(ProjectName);
				entity.setSpjangcd(spjangcd);
				entity.setCompany_id(company_id);

				workCategoryRepository.save(entity);
			}

			result.message = "저장되었습니다.";
			return result;
		}else{
			/***수정***/
			// 1) 기존 데이터 조회
			List<WorkCategory> oldList = workCategoryRepository.findByIdJobResId(job_res_id);

			// 2) Map 형태로 변환
			Map<String, WorkCategory> oldMap = new HashMap<>();
			for(WorkCategory wc : oldList){
				oldMap.put(wc.getId().getWorkCode(), wc);

			}

			// 3) 클라이언트 코드 목록 set 으로 추출
			Set<String> clientCodes = workList.stream()
					.map(w -> w.get("code"))
					.collect(Collectors.toSet());


			// 4) 기존에는 있지만 클라이언트에는 없는 항목 → 삭제 후보
			try {

				for (WorkCategory oldItem : oldList) {
					String oldCode = oldItem.getId().getWorkCode();

					if (!clientCodes.contains(oldCode)) {

						int refCount = tbPb201Repository.countByJobResIdAndWorkcd(job_res_id, oldCode);

						if (refCount > 0) {
							// tb_pb201에 사용 중이면 즉시 예외
							throw new RuntimeException(
									"근무코드 '" + oldCode + "' 는 근무일지에 사용 중이므로 삭제할 수 없습니다."
							);
						}

						workCategoryRepository.delete(oldItem);
					}
				}

			} catch (RuntimeException e) {
				AjaxResult error = new AjaxResult();
				error.success = false;
				error.message = e.getMessage();
				return error;   // ← 바로 반환 (트랜잭션 롤백)
			}

			// 5) 클라이언트 데이터를 기준으로 insert 또는 update 처리
			for(Map<String, String> work : workList){

				String code = work.get("code");
				String name = work.get("name");

				WorkCategory entity;

				if(oldMap.containsKey(code)){
					//update
					entity = oldMap.get(code);
				}else{
					//insert

					entity = new WorkCategory();
					WorkCategoryId pk = new WorkCategoryId(job_res_id, code);
					entity.setId(pk);
				}


				entity.setStartTime(StartTime);
				entity.setEndTime(EndTime);
				entity.setWorkName(work.get("name"));
				entity.setJobResNum(work_order);
				entity.setJumunNum(jumun_num);
				entity.setDescription(Description);
				entity.setProjectName(ProjectName);
				entity.setCompany_id(company_id);
				entity.setSpjangcd(spjangcd);

				workCategoryRepository.save(entity);
			}

			result.message = "수정되었습니다.";
			result.success = true;
			return result;
		}

	}
	
	@PostMapping("/delete")
	@Transactional
	public AjaxResult deleteShift(@RequestParam("job_res_id") int job_res_id) {

		AjaxResult result = new AjaxResult();

		//해당 코드로 근무등록 흔적있으면 노 삭제
		List<Map<String, Object>> codeInWorkLog = shiftService.getCodeInWorkLog(job_res_id);

		if(!codeInWorkLog.isEmpty()){
			String code = (String) codeInWorkLog.get(0).get("code");
			result.success = false;
            result.message = "근무코드 '" + code + "' 는 근무일지에 사용 중이므로 삭제할 수 없습니다.";
			return result;
		}

		this.workCategoryRepository.deleteByJobResId(job_res_id);

		return result;
	}

	@GetMapping("/job_res_list")
	public AjaxResult getJobResList(@RequestParam String date_from,
									@RequestParam String date_to
									){

		AjaxResult result = new AjaxResult();

        result.data = this.shiftService.getJob_res_List(date_from, date_to);
		return result;
	}

	public OffsetDateTime convertToOffsetDateTime(String timeStr) {

		if (timeStr == null || timeStr.isBlank()) return null;

		// 1) 시간 파싱 (HH:mm)
		LocalTime localTime = LocalTime.parse(timeStr);

		// 2) 오늘 날짜 가져오기
		LocalDate today = LocalDate.now();

		// 3) OffsetDateTime으로 변환 (기본 시스템 타임존 사용)
		return OffsetDateTime.of(today, localTime, ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
	}
}

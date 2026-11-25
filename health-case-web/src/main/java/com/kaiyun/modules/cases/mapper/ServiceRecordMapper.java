package com.kaiyun.modules.cases.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kaiyun.modules.cases.entity.ServiceRecord;
import com.kaiyun.common.pagination.AdminListQuery;
import com.kaiyun.modules.cases.vo.AdminServiceRecordListVO;
import com.kaiyun.modules.cases.vo.CaseAttachmentSummaryVO;
import com.kaiyun.modules.cases.vo.ClaimPushCaseVO;
import com.kaiyun.modules.cases.vo.PaidCaseVO;
import com.kaiyun.modules.cases.vo.ServiceRecordDetailVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ServiceRecordMapper extends BaseMapper<ServiceRecord> {
    /** 关联详情 SQL 统一维护在 mapper/cases/ServiceRecordMapper.xml。 */
    ServiceRecordDetailVO selectDetailById(@Param("id") Long id);

    @Select("SELECT id, status, process_remark FROM service_record WHERE id = #{id} FOR UPDATE")
    ServiceRecord selectProgressForUpdate(@Param("id") Long id);

    List<AdminServiceRecordListVO> selectAdminList();
    long countAdminPage(@Param("query") AdminListQuery query);
    List<AdminServiceRecordListVO> selectAdminPage(@Param("query") AdminListQuery query, @Param("offset") long offset);
    long countPaidCases(@Param("query") AdminListQuery query);
    List<PaidCaseVO> selectPaidCases(@Param("query") AdminListQuery query, @Param("offset") long offset);

    @Select("SELECT r.id, r.name, r.case_title, r.create_time, COUNT(f.id) AS attachment_count "
            + "FROM service_record r JOIN service_record_image f ON f.service_record_id = r.id "
            + "GROUP BY r.id, r.name, r.case_title, r.create_time ORDER BY r.create_time DESC, r.id DESC")
    List<CaseAttachmentSummaryVO> selectCasesWithAttachments();

    @Select("SELECT r.id, r.case_title, r.status, r.create_time, p.policy_no, r.name AS patient_name, "
            + "COALESCE(NULLIF(p.insured_name, ''), NULLIF(i.name, '')) AS insured_name, "
            + "t.task_id AS push_task_id, t.status AS push_status, t.attempts AS push_attempts, t.last_error AS push_error, t.trigger_type AS push_trigger, "
            + "t.case_status AS push_case_status, EXISTS (SELECT 1 FROM case_push_task a WHERE a.case_id=r.id "
            + "AND a.status IN ('QUEUED','RUNNING','RETRY_WAIT')) AS active_push "
            + "FROM service_record r LEFT JOIN service_insured i ON i.id = r.insured_id "
            + "LEFT JOIN service_policy p ON p.id = r.policy_id "
            + "LEFT JOIN case_push_task t ON t.task_order = (SELECT MAX(h.task_order) FROM case_push_task h WHERE h.case_id=r.id) "
            + "ORDER BY r.create_time DESC, r.id DESC")
    List<ClaimPushCaseVO> selectClaimPushCases();
}

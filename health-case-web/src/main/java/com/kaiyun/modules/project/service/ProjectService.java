package com.kaiyun.modules.project.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.modules.project.dto.ManagementInputs.ProjectInput;
import com.kaiyun.modules.project.dto.ManagementQuery;
import com.kaiyun.modules.project.vo.ManagementViews.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static com.kaiyun.modules.project.service.ManagementValidation.*;

@Service
public class ProjectService {
    private static final String BASE=" FROM service_project j JOIN insurance_company i ON i.id=j.insurer_id";
    private static final String SELECT="SELECT j.*,i.name AS insurer_name,"
            +"(SELECT COUNT(*) FROM service_policy p WHERE p.project_id=j.id) AS policy_count,"
            +"(SELECT COUNT(*) FROM service_record r JOIN service_policy p ON p.id=r.policy_id WHERE p.project_id=j.id) AS case_count";
    private final JdbcTemplate jdbc;
    private final InsuranceCompanyService companies;
    public ProjectService(JdbcTemplate jdbc,InsuranceCompanyService companies){this.jdbc=jdbc;this.companies=companies;}

    public AdminListPage<Project> page(ManagementQuery query){
        query.validate();
        String where=" WHERE (LOCATE(LOWER(?),LOWER(j.name))>0 OR LOCATE(LOWER(?),LOWER(j.code))>0)";
        List<Object> args=new ArrayList<>(Arrays.asList(query.getKeyword(),query.getKeyword()));
        if(query.getInsurerId()!=null){where+=" AND j.insurer_id=?";args.add(query.getInsurerId());}
        if(!query.getStatus().isEmpty()){option(query.getStatus(),"DRAFT","ACTIVE","PAUSED","ENDED");where+=" AND j.status=?";args.add(query.getStatus());}
        long total=jdbc.queryForObject("SELECT COUNT(*)"+BASE+where,Long.class,args.toArray());
        int page=query.pageWithin(total);args.add(query.getSize());args.add((page-1L)*query.getSize());
        return new AdminListPage<>(page,total,jdbc.query(SELECT+BASE+where+" ORDER BY j.id DESC LIMIT ? OFFSET ?",new BeanPropertyRowMapper<>(Project.class),args.toArray()));
    }
    public Project detail(long id){
        List<Project> rows=jdbc.query(SELECT+BASE+" WHERE j.id=?",new BeanPropertyRowMapper<>(Project.class),id);
        if(rows.isEmpty())throw new BusinessException(404,"项目不存在");
        return rows.get(0);
    }
    @Transactional
    public Project save(Long id,ProjectInput input){
        String code=code(input.getCode()),name=required(input.getName(),120,"项目名称");
        if(input.getInsurerId()==null)throw bad("请选择保险公司");
        Company insurer=companies.detail(input.getInsurerId());
        String status=option(input.getStatus(),"DRAFT","ACTIVE","PAUSED","ENDED");
        if("ACTIVE".equals(status)&&!insurer.isEnabled())throw bad("启用项目之前请先启用所属保险公司");
        if(input.getStartDate()!=null&&input.getEndDate()!=null&&input.getEndDate().isBefore(input.getStartDate()))throw bad("结束日期不能早于开始日期");
        for(LocalDate value:new LocalDate[]{input.getStartDate(),input.getEndDate()})if(value!=null&&(value.getYear()<1000||value.getYear()>9999))throw bad("项目日期超出支持范围");
        String owner=text(input.getOwnerName(),60),phone=phone(input.getOwnerPhone());
        String description=text(input.getDescription(),2000),requirements=text(input.getDocumentRequirements(),2000);
        if(id==null){
            GeneratedKeyHolder key=new GeneratedKeyHolder();
            jdbc.update(connection->{
                PreparedStatement statement=connection.prepareStatement("INSERT INTO service_project(code,name,insurer_id,status,owner_name,owner_phone,start_date,end_date,description,document_requirements) VALUES(?,?,?,?,?,?,?,?,?,?)",new String[]{"id"});
                Object[] values={code,name,input.getInsurerId(),status,owner,phone,input.getStartDate(),input.getEndDate(),description,requirements};
                for(int index=0;index<values.length;index++)statement.setObject(index+1,values[index]);
                return statement;
            },key);
            id=Objects.requireNonNull(key.getKey()).longValue();
        }else{
            List<Project> rows=jdbc.query("SELECT * FROM service_project WHERE id=? FOR UPDATE",new BeanPropertyRowMapper<>(Project.class),id);
            if(rows.isEmpty())throw new BusinessException(404,"项目不存在");
            Project current=rows.get(0);version(input.getVersion(),current.getVersion());
            if(!code.equals(current.getCode()))throw bad("项目编码不可修改");
            if(!input.getInsurerId().equals(current.getInsurerId())&&jdbc.queryForObject("SELECT COUNT(*) FROM service_policy WHERE project_id=?",Long.class,id)>0)throw bad("项目已有保单，不能更换所属保险公司");
            jdbc.update("UPDATE service_project SET name=?,insurer_id=?,status=?,owner_name=?,owner_phone=?,start_date=?,end_date=?,description=?,document_requirements=?,version=version+1,update_time=CURRENT_TIMESTAMP WHERE id=?",
                    name,input.getInsurerId(),status,owner,phone,input.getStartDate(),input.getEndDate(),description,requirements,id);
        }
        return detail(id);
    }
    /** 导入及新案件提交只允许有效期内、运行中且公司启用的项目。 */
    public Project requireActive(long id){
        Project project=detail(id);LocalDate today=LocalDate.now();
        if(!"ACTIVE".equals(project.getStatus())||!companies.detail(project.getInsurerId()).isEnabled()
                ||project.getStartDate()!=null&&today.isBefore(project.getStartDate())||project.getEndDate()!=null&&today.isAfter(project.getEndDate())){
            throw bad("所属项目尚未运行、已暂停或不在有效期内");
        }
        return project;
    }
    public List<Project> options(){
        return jdbc.query("SELECT j.id,j.code,j.name,j.insurer_id,i.name AS insurer_name,j.status"+BASE
                +" WHERE j.status='ACTIVE' AND i.enabled=TRUE AND (j.start_date IS NULL OR j.start_date<=CURRENT_DATE)"
                +" AND (j.end_date IS NULL OR j.end_date>=CURRENT_DATE) ORDER BY j.name,j.id LIMIT 500",new BeanPropertyRowMapper<>(Project.class));
    }
    public Project projectForPolicy(long policyId){
        List<Long> ids=jdbc.query("SELECT project_id FROM service_policy WHERE id=?",(row,index)->row.getObject(1,Long.class),policyId);
        return ids.isEmpty()||ids.get(0)==null?null:detail(ids.get(0));
    }
    public void validateSubmission(long policyId){
        // 调用方的案件事务持有保单锁，与管理端关联项目操作串行，避免已有案件被改换归属。
        List<Long> ids=jdbc.query("SELECT project_id FROM service_policy WHERE id=? FOR UPDATE",(row,index)->row.getObject(1,Long.class),policyId);
        if(!ids.isEmpty()&&ids.get(0)!=null)requireActive(ids.get(0));
    }
    public Project lockActive(long id){
        jdbc.queryForList("SELECT id FROM service_project WHERE id=? FOR UPDATE",id);
        return requireActive(id);
    }
    public DeliveryTarget deliveryForCase(long caseId){
        List<Long> ids=jdbc.query("SELECT p.project_id FROM service_record r LEFT JOIN service_policy p ON p.id=r.policy_id WHERE r.id=?",(row,index)->row.getObject(1,Long.class),caseId);
        if(ids.isEmpty()||ids.get(0)==null)return null;
        Project project=requireActive(ids.get(0));
        return companies.deliveryTarget(project.getInsurerId());
    }
    public void validatePush(List<Long> ids){for(Long id:ids)deliveryForCase(id);}
    public AdminListPage<LinkedPolicy> policies(long id,ManagementQuery query,boolean available){
        detail(id);query.validate();
        String where=available?" WHERE p.project_id IS NULL AND NOT EXISTS(SELECT 1 FROM service_record r WHERE r.policy_id=p.id)":" WHERE p.project_id=?";
        List<Object> args=new ArrayList<>();if(!available)args.add(id);
        where+=" AND (LOCATE(LOWER(?),LOWER(p.policy_no))>0 OR LOCATE(LOWER(?),LOWER(p.product_name))>0)";args.add(query.getKeyword());args.add(query.getKeyword());
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM service_policy p"+where,Long.class,args.toArray());
        int page=query.pageWithin(total);args.add(query.getSize());args.add((page-1L)*query.getSize());
        List<LinkedPolicy> rows=jdbc.query("SELECT p.id,p.policy_no,p.product_name,p.insured_name,p.status FROM service_policy p"+where+" ORDER BY p.id DESC LIMIT ? OFFSET ?",new BeanPropertyRowMapper<>(LinkedPolicy.class),args.toArray());
        return new AdminListPage<>(page,total,rows);
    }
    public AdminListPage<LinkedCase> cases(long id,ManagementQuery query){
        detail(id);query.validate();
        String from=" FROM service_record r JOIN service_policy p ON p.id=r.policy_id WHERE p.project_id=?";
        long total=jdbc.queryForObject("SELECT COUNT(*)"+from,Long.class,id);int page=query.pageWithin(total);
        List<LinkedCase> rows=jdbc.query("SELECT r.id,r.name,r.case_title,r.status,p.policy_no,r.create_time"+from+" ORDER BY r.create_time DESC,r.id DESC LIMIT ? OFFSET ?",new BeanPropertyRowMapper<>(LinkedCase.class),id,query.getSize(),(page-1L)*query.getSize());
        return new AdminListPage<>(page,total,rows);
    }
    @Transactional
    public int assign(long projectId,List<Long> rawIds){
        lockActive(projectId);
        if(rawIds==null||rawIds.isEmpty()||rawIds.size()>100||rawIds.stream().anyMatch(id->id==null||id<=0))throw bad("请选择1至100份有效保单");
        List<Long> ids=rawIds.stream().distinct().sorted().collect(Collectors.toList());
        String placeholders=ids.stream().map(id->"?").collect(Collectors.joining(","));
        // 锁定保单，避免并发关联覆盖；已有案件的归属保持稳定。
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,project_id FROM service_policy WHERE id IN ("+placeholders+") FOR UPDATE",ids.toArray());
        if(rows.size()!=ids.size())throw bad("部分保单不存在，请刷新列表");
        for(Map<String,Object> row:rows){
            Number current=(Number)row.get("project_id");
            if(current!=null&&current.longValue()==projectId)continue;
            if(current!=null)throw bad("保单已属于其他项目");
            // 使用锁定读查看最新提交，避免 MySQL 可重复读快照漏掉刚刚并发提交的案件。
            if(!jdbc.queryForList("SELECT id FROM service_record WHERE policy_id=? LIMIT 1 FOR UPDATE",row.get("id")).isEmpty())throw bad("已有案件的保单不能修改所属项目");
            jdbc.update("UPDATE service_policy SET project_id=? WHERE id=?",projectId,row.get("id"));
        }
        return ids.size();
    }
}

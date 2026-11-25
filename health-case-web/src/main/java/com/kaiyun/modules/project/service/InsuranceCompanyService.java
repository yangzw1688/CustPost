package com.kaiyun.modules.project.service;

import com.kaiyun.common.exception.BusinessException;
import com.kaiyun.common.pagination.AdminListPage;
import com.kaiyun.infrastructure.storage.ConfigurationSecretCodec;
import com.kaiyun.modules.project.dto.ManagementInputs.*;
import com.kaiyun.modules.project.dto.ManagementQuery;
import com.kaiyun.modules.project.vo.ManagementViews.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.PreparedStatement;
import java.util.*;
import static com.kaiyun.modules.project.service.ManagementValidation.*;

@Service
public class InsuranceCompanyService {
    private static final String SELECT = "SELECT c.*, COALESCE(n.status,'NOT_CONFIGURED') AS connection_status, "
            + "(SELECT COUNT(*) FROM service_project p WHERE p.insurer_id=c.id) AS project_count FROM insurance_company c "
            + "LEFT JOIN insurer_connection n ON n.company_id=c.id AND n.environment=c.active_environment";
    private final JdbcTemplate jdbc;
    private final ConfigurationSecretCodec secrets;
    public InsuranceCompanyService(JdbcTemplate jdbc, ConfigurationSecretCodec secrets) { this.jdbc = jdbc; this.secrets = secrets; }

    public AdminListPage<Company> page(ManagementQuery query) {
        query.validate();
        String where = " WHERE (LOCATE(LOWER(?),LOWER(c.name))>0 OR LOCATE(LOWER(?),LOWER(c.code))>0)";
        List<Object> args = new ArrayList<>(Arrays.asList(query.getKeyword(), query.getKeyword()));
        if (!query.getStatus().isEmpty()) {
            option(query.getStatus(), "ENABLED", "DISABLED");
            where += " AND c.enabled=?"; args.add("ENABLED".equals(query.getStatus()));
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM insurance_company c" + where, Long.class, args.toArray());
        int page = query.pageWithin(total);
        args.add(query.getSize()); args.add((page - 1L) * query.getSize());
        return new AdminListPage<>(page, total, jdbc.query(SELECT + where + " ORDER BY c.id DESC LIMIT ? OFFSET ?",
                new BeanPropertyRowMapper<>(Company.class), args.toArray()));
    }
    public Company detail(long id) {
        List<Company> rows = jdbc.query(SELECT + " WHERE c.id=?", new BeanPropertyRowMapper<>(Company.class), id);
        if (rows.isEmpty()) throw new BusinessException(404, "保险公司不存在");
        return rows.get(0);
    }
    @Transactional
    public Company save(Long id, CompanyInput input) {
        String code = code(input.getCode()), name = required(input.getName(), 120, "保险公司名称");
        String shortName = text(input.getShortName(), 80), contact = text(input.getContactName(), 60);
        String phone = phone(input.getContactPhone()), address = text(input.getAddress(), 240), remarks = text(input.getRemarks(), 1000);
        String env = environment(input.getActiveEnvironment());
        if (input.getEnabled() == null) throw bad("请选择启用状态");
        if (id == null) {
            GeneratedKeyHolder key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO insurance_company(code,name,short_name,contact_name,"
                        + "contact_phone,address,remarks,enabled,active_environment) VALUES(?,?,?,?,?,?,?,?,?)", new String[]{"id"});
                Object[] values = {code,name,shortName,contact,phone,address,remarks,input.getEnabled(),env};
                for (int index=0;index<values.length;index++) statement.setObject(index+1, values[index]);
                return statement;
            }, key);
            id = Objects.requireNonNull(key.getKey()).longValue();
            for (String value : new String[]{"TEST","PROD"}) jdbc.update("INSERT INTO insurer_connection(company_id,environment) VALUES(?,?)", id, value);
        } else {
            List<Company> current = jdbc.query("SELECT * FROM insurance_company WHERE id=? FOR UPDATE", new BeanPropertyRowMapper<>(Company.class), id);
            if (current.isEmpty()) throw new BusinessException(404, "保险公司不存在");
            version(input.getVersion(), current.get(0).getVersion());
            if (!code.equals(current.get(0).getCode())) throw bad("保险公司编码不可修改");
            jdbc.update("UPDATE insurance_company SET name=?,short_name=?,contact_name=?,contact_phone=?,address=?,remarks=?,enabled=?,"
                    + "active_environment=?,version=version+1,update_time=CURRENT_TIMESTAMP WHERE id=?", name,shortName,contact,phone,address,remarks,input.getEnabled(),env,id);
        }
        return detail(id);
    }
    public List<Connection> connections(long id) {
        detail(id);
        // 密文和原始令牌均不参与查询响应映射。
        return jdbc.query("SELECT environment,status,endpoint,auth_type,(token_ciphertext<>'') AS token_configured,version,update_time "
                + "FROM insurer_connection WHERE company_id=? ORDER BY environment DESC", new BeanPropertyRowMapper<>(Connection.class), id);
    }
    @Transactional
    public Connection saveConnection(long id, String env, ConnectionInput input) {
        environment(env); detail(id);
        String status = option(input.getStatus(), "NOT_CONFIGURED", "TESTING", "CONNECTED", "SUSPENDED");
        String endpoint = endpoint(input.getEndpoint()), auth = option(input.getAuthType(), "NONE", "BEARER");
        Map<String,Object> current = jdbc.queryForMap("SELECT version,token_ciphertext FROM insurer_connection WHERE company_id=? AND environment=? FOR UPDATE", id, env);
        version(input.getVersion(), ((Number) current.get("version")).longValue());
        String cipher = Objects.toString(current.get("token_ciphertext"), "");
        if ("NONE".equals(auth)) cipher = "";
        else if (input.getToken() != null && !input.getToken().trim().isEmpty()) {
            String token = input.getToken().trim();
            if (token.length()>512 || !token.matches("[A-Za-z0-9._~+/-]+=*")) throw bad("请填写有效的 Bearer 令牌");
            cipher = secrets.encrypt(token);
        }
        if ("CONNECTED".equals(status) && (endpoint.isEmpty() || "BEARER".equals(auth) && cipher.isEmpty())) {
            throw bad("标记为已对接前须填写接收地址及所选鉴权方式需要的令牌");
        }
        jdbc.update("UPDATE insurer_connection SET status=?,endpoint=?,auth_type=?,token_ciphertext=?,version=version+1,update_time=CURRENT_TIMESTAMP "
                + "WHERE company_id=? AND environment=?",status,endpoint,auth,cipher,id,env);
        return connections(id).stream().filter(value -> value.getEnvironment().equals(env)).findFirst().get();
    }
    public AdminListPage<PushRecord> pushRecords(long id, ManagementQuery query) {
        detail(id); query.validate();
        String from = " FROM case_push_task t JOIN service_record r ON r.id=t.case_id JOIN service_policy p ON p.id=r.policy_id "
                + "JOIN service_project j ON j.id=p.project_id WHERE j.insurer_id=?";
        long total = jdbc.queryForObject("SELECT COUNT(*)"+from, Long.class,id);
        int page=query.pageWithin(total);
        List<PushRecord> rows=jdbc.query("SELECT t.case_id,t.task_id,j.name AS project_name,t.status,t.attempts,t.last_error,t.updated_at"+from
                +" ORDER BY t.updated_at DESC,t.task_order DESC LIMIT ? OFFSET ?",new BeanPropertyRowMapper<>(PushRecord.class),id,query.getSize(),(page-1L)*query.getSize());
        return new AdminListPage<>(page,total,rows);
    }
    /** 由项目归属选择接收方；仅在工作器发起请求时解密。 */
    public DeliveryTarget deliveryTarget(long companyId) {
        Company company=detail(companyId);
        if (!company.isEnabled()) throw bad("所属保险公司已停用");
        List<Map<String,Object>> connections=jdbc.queryForList("SELECT status,endpoint,auth_type,token_ciphertext FROM insurer_connection WHERE company_id=? AND environment=?",companyId,company.getActiveEnvironment());
        if (connections.isEmpty()) throw bad("保险公司当前环境尚未配置接收方");
        Map<String,Object> config=connections.get(0);
        if (!"CONNECTED".equals(config.get("status"))) throw bad("保险公司当前环境尚未完成对接或已暂停");
        String url=endpoint(Objects.toString(config.get("endpoint"),""));
        if (url.isEmpty()) throw bad("保险公司未配置接收地址");
        String auth="";
        if ("BEARER".equals(config.get("auth_type"))) {
            String cipher=Objects.toString(config.get("token_ciphertext"),"");
            if (cipher.isEmpty()) throw bad("保险公司未配置接收令牌");
            auth="Bearer "+secrets.decrypt(cipher);
        }
        return new DeliveryTarget(url,auth,companyId+"/"+company.getActiveEnvironment());
    }
}

package com.kaiyun.modules.auth.service;

import com.kaiyun.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class AdminPermissionService {
    private static final String GRANT_COLUMNS = "phone, super_admin, policy_transfer, ui_management, "
            + "dashboard_access, case_access, policy_access, project_access, platform_access, user_access";
    private static final String REQUEST_ADMIN = AdminPermissionService.class.getName() + ".grant";
    private static final String VERIFIED_ADMIN = AdminPermissionService.class.getName() + ".verified";
    private static final int MIN_ROOT_PASSWORD_LENGTH = 8;
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final LoginService login;
    private final String rootPhone;
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();
    public AdminPermissionService(LoginService login, JdbcTemplate jdbc,
            @Value("${app.admin.root-phone:}") String rootPhone) {
        this.login = login;
        this.rootPhone = rootPhone.trim();
        if (!this.rootPhone.isEmpty() && !this.rootPhone.matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("主超级管理员手机号格式不正确");
        }
        this.jdbc = jdbc;
    }

    public Grant login(String phone, String password, HttpServletRequest request) {
        login.validatePhone(phone);
        Account account = find(phone);
        int minLength = rootPhone.equals(phone) ? MIN_ROOT_PASSWORD_LENGTH : MIN_PASSWORD_LENGTH;
        if (account == null || !isValidPassword(password, minLength) || !matches(password, account.passwordHash)) {
            throw badCredentials();
        }
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(VERIFIED_ADMIN, new AdminSession(phone, account.passwordHash));
        return account.grant;
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.removeAttribute(VERIFIED_ADMIN);
    }

    public Grant requireAdmin(HttpServletRequest request) {
        Object cached = request.getAttribute(REQUEST_ADMIN);
        if (cached instanceof Grant) return (Grant) cached;
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(VERIFIED_ADMIN);
        if (value instanceof AdminSession) {
            AdminSession admin = (AdminSession) value;
            Account account = find(admin.phone);
            if (account != null && equal(account.passwordHash, admin.marker)) {
                request.setAttribute(REQUEST_ADMIN, account.grant);
                return account.grant;
            }
        }
        throw new BusinessException(401, "请使用手机号和管理员密码登录管理端");
    }

    public void requireModule(HttpServletRequest request, String module) {
        if (!requireAdmin(request).modules.allows(module)) {
            throw new BusinessException(403, "当前账号无此模块权限，请联系超级管理员");
        }
    }

    public String requirePolicyTransfer(HttpServletRequest request) {
        Grant admin = requireAdmin(request);
        if (!admin.policyTransfer) throw new BusinessException(403, "当前账号无保单导入与导出权限，请联系超级管理员");
        return admin.phone;
    }

    public void requireUiManagement(HttpServletRequest request) {
        Grant admin = requireAdmin(request);
        requireModule(request, AdminModules.PLATFORM);
        if (!admin.uiManagement) {
            throw new BusinessException(403, "当前账号无用户端页面管理权限，请联系超级管理员");
        }
    }

    public List<Grant> list(HttpServletRequest request) {
        requireRoot(request);
        List<Grant> grants = jdbc.query("SELECT " + GRANT_COLUMNS + " FROM admin_permission WHERE phone <> ? ORDER BY phone",
                (row, index) -> readGrant(row), rootPhone);
        if (!rootPhone.isEmpty()) grants.add(0, rootGrant());
        return grants;
    }

    @Transactional
    public void grant(HttpServletRequest request, String phone, AdminModules modules, boolean policyTransfer, boolean uiManagement,
            String password, boolean superAdmin, boolean createOnly) {
        Grant operator = requireRoot(request);
        validateTarget(phone);
        Account existing = find(phone, true);
        if (existing != null && createOnly) throw new BusinessException(409, "该手机号已有管理员账号，请通过列表编辑");
        if (existing != null) requireRoleManagement(operator, superAdmin || existing.grant.superAdmin);
        if (superAdmin) {
            modules = AdminModules.ALL;
            policyTransfer = true;
            uiManagement = true;
        } else if (modules == null) {
            throw new BusinessException(400, "请提交模块权限设置");
        }
        policyTransfer = modules.isPolicy() && policyTransfer;
        uiManagement = modules.isPlatform() && uiManagement;
        String passwordHash;
        if (password == null || password.isEmpty()) {
            if (existing == null) throw new BusinessException(400, "新增管理员须设置专用密码");
            passwordHash = existing.passwordHash;
        } else {
            validatePassword(password);
            Account root = find(rootPhone);
            if (root != null && matches(password, root.passwordHash)) {
                throw new BusinessException(400, "请为该账号设置独立的管理员密码");
            }
            passwordHash = encode(password);
        }
        if (existing == null) {
            try {
                jdbc.update("INSERT INTO admin_permission (" + GRANT_COLUMNS + ", password_hash) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        phone, superAdmin, policyTransfer, uiManagement, modules.isDashboard(), modules.isCases(),
                        modules.isPolicy(), modules.isProject(), modules.isPlatform(), modules.isUsers(), passwordHash);
            } catch (DuplicateKeyException error) {
                throw new BusinessException(409, "该手机号已有管理员账号，请刷新后编辑");
            }
        } else {
            jdbc.update("UPDATE admin_permission SET super_admin = ?, policy_transfer = ?, ui_management = ?, dashboard_access = ?, "
                            + "case_access = ?, policy_access = ?, project_access = ?, platform_access = ?, user_access = ?, "
                            + "password_hash = ? WHERE phone = ?",
                    superAdmin, policyTransfer, uiManagement, modules.isDashboard(), modules.isCases(),
                    modules.isPolicy(), modules.isProject(), modules.isPlatform(), modules.isUsers(), passwordHash, phone);
        }
    }

    /** 仅为新数据库创建初始账号。已有密码由数据库记录决定。 */
    public void initializeRoot(String password) {
        if (password.isEmpty()) return;
        if (rootPhone.isEmpty()) throw new IllegalStateException("请配置主超级管理员手机号 ADMIN_ROOT_PHONE");
        if (find(rootPhone) != null) return;
        if (!isValidPassword(password, MIN_ROOT_PASSWORD_LENGTH)) {
            throw new IllegalStateException("超级管理员初始密码须为 8 至 128 个字符");
        }
        jdbc.update("INSERT INTO admin_permission (phone, policy_transfer, ui_management, password_hash) "
                + "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE phone = VALUES(phone)",
                rootPhone, true, true, encode(password));
    }

    public void changeSuperAdminPassword(HttpServletRequest request, String currentPassword, String newPassword) {
        Grant admin = requireRoot(request);
        int minLength = admin.primaryAdmin ? MIN_ROOT_PASSWORD_LENGTH : MIN_PASSWORD_LENGTH;
        if (!isValidPassword(newPassword, minLength)) {
            throw new BusinessException(400, "新密码须为 " + minLength + " 至 128 个字符");
        }
        Account account = find(admin.phone);
        if (account == null || !isValidPassword(currentPassword, minLength) || !matches(currentPassword, account.passwordHash)) {
            throw new BusinessException(403, "当前密码不正确");
        }
        if (jdbc.update("UPDATE admin_permission SET password_hash = ? WHERE phone = ? AND password_hash = ?"
                        + (admin.primaryAdmin ? "" : " AND super_admin = TRUE"),
                encode(newPassword), admin.phone, account.passwordHash) != 1) {
            throw new BusinessException(409, "密码已发生变化，请重新登录");
        }
        logout(request);
    }

    @Transactional
    public void revoke(HttpServletRequest request, String phone) {
        Grant operator = requireRoot(request);
        validateTarget(phone);
        Account existing = find(phone, true);
        requireRoleManagement(operator, existing != null && existing.grant.superAdmin);
        jdbc.update("DELETE FROM admin_permission WHERE phone = ?", phone);
    }

    private Grant requireRoot(HttpServletRequest request) {
        Grant admin = requireAdmin(request);
        if (!admin.superAdmin) throw new BusinessException(403, "只有超级管理员可以管理权限");
        return admin;
    }

    private static void requireRoleManagement(Grant operator, boolean managesSuperAdmin) {
        if (managesSuperAdmin && !operator.primaryAdmin) {
            throw new BusinessException(403, "只有主超级管理员可以调整现有账号的超级管理员角色或撤销超级管理员");
        }
    }

    private void validateTarget(String phone) {
        login.validatePhone(phone);
        if (rootPhone.equals(phone)) throw new BusinessException(400, "超级管理员的权限固定");
    }

    private Account find(String phone) {
        return find(phone, false);
    }

    private Account find(String phone, boolean lock) {
        return jdbc.query("SELECT " + GRANT_COLUMNS + ", password_hash FROM admin_permission WHERE phone = ?"
                        + (lock ? " FOR UPDATE" : ""),
                row -> row.next() ? new Account(readGrant(row), row.getString("password_hash")) : null, phone);
    }

    private Grant readGrant(ResultSet row) throws SQLException {
        String phone = row.getString("phone");
        if (rootPhone.equals(phone)) return rootGrant();
        if (row.getBoolean("super_admin")) return new Grant(phone, true, false, true, true, AdminModules.ALL);
        boolean transfer = row.getBoolean("policy_transfer");
        boolean ui = row.getBoolean("ui_management");
        // 未配置模块的旧账号沿用已有专项授权，其余模块由超级管理员显式开启。
        AdminModules modules = new AdminModules(
                allowed(row, "dashboard_access", false), allowed(row, "case_access", false),
                allowed(row, "policy_access", transfer), allowed(row, "project_access", false),
                allowed(row, "platform_access", ui), allowed(row, "user_access", false));
        return new Grant(phone, false, false, modules.isPolicy() && transfer, modules.isPlatform() && ui, modules);
    }

    private static boolean allowed(ResultSet row, String column, boolean fallback) throws SQLException {
        boolean enabled = row.getBoolean(column);
        return row.wasNull() ? fallback : enabled;
    }

    private Grant rootGrant() {
        return new Grant(rootPhone, true, true, true, true, AdminModules.ALL);
    }

    private static BusinessException badCredentials() {
        return new BusinessException(403, "管理员账号或密码不正确");
    }

    private static void validatePassword(String password) {
        if (!isValidPassword(password, MIN_PASSWORD_LENGTH)) {
            throw new BusinessException(400, "管理员密码须为 12 至 128 个字符");
        }
    }

    private static boolean isValidPassword(String password, int minLength) {
        return password != null && password.length() >= minLength && password.length() <= MAX_PASSWORD_LENGTH
                && !password.trim().isEmpty();
    }

    private static boolean equal(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return hash(password, salt);
    }

    private static boolean matches(String password, String encoded) {
        int separator = encoded.indexOf(':');
        if (separator <= 0) return false;
        try {
            byte[] salt = Base64.getDecoder().decode(encoded.substring(0, separator));
            return salt.length == 16 && equal(encoded, hash(password, salt));
        } catch (IllegalArgumentException invalidHash) {
            return false;
        }
    }

    private static String hash(String password, byte[] salt) {
        PBEKeySpec input = new PBEKeySpec(password.toCharArray(), salt, 600_000, 256);
        try {
            byte[] digest = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(input).getEncoded();
            Base64.Encoder encoder = Base64.getEncoder();
            return encoder.encodeToString(salt) + ":" + encoder.encodeToString(digest);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("管理员密码验证不可用", error);
        } finally {
            input.clearPassword();
        }
    }

    @lombok.Value
    public static class Grant {
        String phone;
        boolean superAdmin;
        boolean primaryAdmin;
        boolean policyTransfer;
        boolean uiManagement;
        AdminModules modules;
    }

    @lombok.Value
    private static class AdminSession implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        String phone;
        String marker;
    }

    @lombok.Value
    private static class Account {
        Grant grant;
        String passwordHash;
    }
}

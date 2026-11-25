package com.kaiyun.modules.policy.service;

import com.kaiyun.modules.policy.entity.ServicePolicy;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

@Getter
enum PolicyImportField {
    POLICY_NO("policyNo", "保单号", true, ServicePolicy::getPolicyNo, "保单编号", "保险单号"),
    PRODUCT_NAME("productName", "保险产品", true, ServicePolicy::getProductName, "保险产品名称", "产品名称", "险种"),
    APPLICANT_NAME("applicantName", "投保人姓名", false, ServicePolicy::getApplicantName, "投保人"),
    APPLICANT_PHONE("applicantPhone", "投保人手机号", false, ServicePolicy::getApplicantPhone, "投保人电话", "投保人联系电话"),
    APPLICANT_ID_TYPE("applicantIdType", "投保人证件类型", false, ServicePolicy::getApplicantIdType),
    APPLICANT_ID_NO("applicantIdNo", "投保人证件号", false, ServicePolicy::getApplicantIdNo, "投保人证件号码"),
    INSURED_NAME("insuredName", "被保人姓名", false, ServicePolicy::getInsuredName, "被保险人姓名", "被保险人", "被保人"),
    INSURED_PHONE("insuredPhone", "被保人手机号", false, ServicePolicy::getInsuredPhone, "被保险人手机号", "被保人电话", "被保人联系电话"),
    INSURED_ID_TYPE("insuredIdType", "被保人证件类型", false, ServicePolicy::getInsuredIdType, "被保险人证件类型"),
    INSURED_ID_NO("insuredIdNo", "被保人证件号", false, ServicePolicy::getInsuredIdNo, "被保险人证件号", "被保人证件号码"),
    PREMIUM("premium", "保费", false, ServicePolicy::getPremium, "保费金额", "保费(元)"),
    STATUS("status", "保单状态", true, ServicePolicy::getStatus, "状态"),
    START_DATE("startDate", "生效日期", false, ServicePolicy::getStartDate, "保障开始日期", "起保日期"),
    END_DATE("endDate", "失效日期", false, ServicePolicy::getEndDate, "保障结束日期", "到期日期");

    private final String key;
    private final String label;
    private final boolean required;
    @Getter(AccessLevel.NONE)
    private final Function<ServicePolicy, ?> accessor;
    @Getter(AccessLevel.NONE)
    private final String[] aliases;

    PolicyImportField(String key, String label, boolean required, Function<ServicePolicy, ?> accessor, String... aliases) {
        this.key = key;
        this.label = label;
        this.required = required;
        this.accessor = accessor;
        this.aliases = aliases;
    }

    Object read(ServicePolicy policy) {
        return accessor.apply(policy);
    }

    boolean matches(String header) {
        String normalized = normalize(header);
        return Stream.concat(Stream.of(key, label), Stream.of(aliases)).map(PolicyImportField::normalize)
                .anyMatch(normalized::equals);
    }

    private static String normalize(String value) {
        return value.replaceAll("[\\s_（）()]+", "").toLowerCase(Locale.ROOT);
    }
}

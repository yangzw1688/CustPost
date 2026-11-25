package com.kaiyun.modules.auth.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class AdminModules {
    public static final String DASHBOARD = "dashboard";
    public static final String CASES = "cases";
    public static final String POLICY = "policy";
    public static final String PROJECT = "project";
    public static final String PLATFORM = "platform";
    public static final String USERS = "users";
    public static final AdminModules ALL = new AdminModules(true, true, true, true, true, true);

    boolean dashboard;
    boolean cases;
    boolean policy;
    boolean project;
    boolean platform;
    boolean users;

    @JsonCreator
    public AdminModules(@JsonProperty(value = DASHBOARD, required = true) boolean dashboard,
            @JsonProperty(value = CASES, required = true) boolean cases,
            @JsonProperty(value = POLICY, required = true) boolean policy,
            @JsonProperty(value = PROJECT, required = true) boolean project,
            @JsonProperty(value = PLATFORM, required = true) boolean platform,
            @JsonProperty(value = USERS, required = true) boolean users) {
        this.dashboard = dashboard;
        this.cases = cases;
        this.policy = policy;
        this.project = project;
        this.platform = platform;
        this.users = users;
    }

    public boolean allows(String module) {
        switch (module) {
            case DASHBOARD: return dashboard;
            case CASES: return cases;
            case POLICY: return policy;
            case PROJECT: return project;
            case PLATFORM: return platform;
            case USERS: return users;
            default: throw new IllegalArgumentException("Unknown admin module");
        }
    }
}

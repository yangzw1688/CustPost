package com.kaiyun.common.pagination;

import lombok.Value;

import java.util.List;

@Value
public class AdminListPage<T> {
    int page;
    long total;
    List<T> rows;
}

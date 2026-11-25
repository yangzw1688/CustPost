package com.kaiyun.common.exception;

import com.kaiyun.common.web.Result;
import com.kaiyun.infrastructure.logging.PlatformOperationLog;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> business(BusinessException exception, HttpServletRequest request) {
        // 只记录已允许返回给页面的业务提示，不保存异常堆栈或请求参数。
        PlatformOperationLog.failure(request, exception.getMessage());
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> duplicate(DuplicateKeyException exception) {
        return error(409, "配置标识已存在，请使用其他标识");
    }

    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Result<Void>> invalidParameters(Exception exception) {
        return error(400, "请求参数不正确，请检查后重试");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return error(413, "上传文件过大，单个文件不能超过10MB，整次上传不能超过100MB");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<Void>> invalidUpload(MultipartException exception) {
        return error(400, "上传内容不完整，请重新选择附件后提交");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> unsupportedMethod(HttpRequestMethodNotSupportedException exception) {
        return error(405, "请求方式不支持");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> unsupportedContentType(HttpMediaTypeNotSupportedException exception) {
        return error(415, "请求内容类型不支持");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> unexpected(Exception exception) {
        // 详细异常仅写入日志，避免向页面泄露 SQL、内部路径或连接信息。
        log.error("API 请求处理失败", exception);
        return error(500, "服务暂时不可用，请稍后重试");
    }

    private ResponseEntity<Result<Void>> error(int status, String message) {
        return ResponseEntity.status(status).body(Result.error(status, message));
    }
}

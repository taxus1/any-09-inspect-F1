package com.somepro.common.exception;

import com.somepro.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

/**
 * 全局异常处理（WebFlux 版）：所有异常收口成统一 Result，不要在每个 Service 里自己 try-catch 吞掉。
 * 校验失败（@Valid）在 WebFlux 下抛出 WebExchangeBindException。
 *
 * ⚠️ 每个分支都必须打日志：框架只看得到 Result 里的 message，不打日志的话
 * 系统级故障（如拿不到 DB 连接）在服务端日志里不留任何痕迹，排查极困难。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Mono<Result<Void>> handleBiz(BizException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        return Mono.just(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<Result<Void>> handleValid(WebExchangeBindException e) {
        FieldError error = e.getFieldErrors().stream().findFirst().orElse(null);
        String msg = error == null ? "参数校验失败" : error.getDefaultMessage();
        log.warn("参数校验失败 field={} msg={}", error == null ? "-" : error.getField(), msg);
        return Mono.just(Result.fail(msg));
    }

    /**
     * 请求体解析失败：JSON 格式错误、日期等字段类型对不上（如 commissionDate 传了 "abc"）。
     * 兜底成统一结构，避免把 Jackson 的原始英文堆栈信息直接抛给前端。
     */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<Result<Void>> handleInput(ServerWebInputException e) {
        log.warn("请求参数解析失败 reason={}", e.getReason());
        return Mono.just(Result.fail("请求参数格式有误：" + e.getReason()));
    }

    /**
     * 唯一索引冲突：应用层查重之外的最后一道防线（并发登记/改号时可能撞上
     * uk_unit_code / uk_reg_code）。MySQL 的 DuplicateKeyException 是其 JDBC 子类。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Mono<Result<Void>> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一索引冲突 {}", e.getMessage());
        return Mono.just(Result.fail("编号已存在，不能重复（可能刚被其他人登记或修改）"));
    }

    @ExceptionHandler(Exception.class)
    public Mono<Result<Void>> handleOther(Exception e) {
        log.error("未处理的系统异常（多为配置或依赖故障）", e);
        return Mono.just(Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统异常：" + e.getMessage()));
    }
}

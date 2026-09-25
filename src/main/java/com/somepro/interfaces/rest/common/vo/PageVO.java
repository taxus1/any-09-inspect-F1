package com.somepro.interfaces.rest.common.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 从 demo 模块里那份同名 VO 泛化为各业务模块共用：
 * 领域层 {@code PageResult} 只有 content/total/pageNum/pageSize（刻意不引 Jackson），
 * 派生字段 totalPages 放在接口层，方便前端直接渲染分页器。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}

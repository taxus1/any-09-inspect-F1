package com.somepro.interfaces.rest.common.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 对外分页返回对象（VO，用户接口层）—— 不可变 record。
 *
 * 与 demo 模块下的 PageVO 同构：PageResult 不带 totalPages（不把 Jackson 引进领域层），
 * 派生字段在接口层补。各业务模块共用这一个。
 */
public record PageVO<T>(List<T> content, long total, int pageNum, int pageSize, int totalPages)
        implements Serializable {
}

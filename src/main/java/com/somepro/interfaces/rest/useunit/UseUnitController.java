package com.somepro.interfaces.rest.useunit;

import com.somepro.application.useunit.UseUnitAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.useunit.converter.UseUnitVoConverter;
import com.somepro.interfaces.rest.useunit.vo.SaveUnitRequest;
import com.somepro.interfaces.rest.useunit.vo.UseUnitVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 使用单位档案接口（用户接口层）：只做协议适配（参数解析、VO 转换、返回包装），编排交给应用层。
 *
 * - POST   /api/units          登记单位（编号如 SY-0031）
 * - PUT    /api/units/{id}     修改档案（支持编号纠错）/ 改状态（ACTIVE/CANCELLED）
 * - GET    /api/units          分页：unitName / unitCode 都可模糊匹配，都不填全量翻页
 * - GET    /api/units/{id}     详情
 * - DELETE /api/units/{id}     删除（名下还有设备时被应用层拒绝）
 *
 * 统一返回 Mono<Result<T>>；不直接返回领域对象，一律经 Converter 转 VO。
 */
@RestController
@RequestMapping("/api/units")
public class UseUnitController {

    private final UseUnitAppService useUnitAppService;

    public UseUnitController(UseUnitAppService useUnitAppService) {
        this.useUnitAppService = useUnitAppService;
    }

    @PostMapping
    public Mono<Result<UseUnitVO>> create(@Valid @RequestBody SaveUnitRequest request) {
        return useUnitAppService.register(request.unitCode(), request.unitName(), request.creditCode(),
                        request.district(), request.contactName(), request.contactPhone(), request.status())
                .map(UseUnitVoConverter::toVo)
                .map(Result::ok);
    }

    @PutMapping("/{id}")
    public Mono<Result<UseUnitVO>> update(@PathVariable Long id,
                                          @Valid @RequestBody SaveUnitRequest request) {
        return useUnitAppService.update(id, request.unitCode(), request.unitName(), request.creditCode(),
                        request.district(), request.contactName(), request.contactPhone(), request.status())
                .map(UseUnitVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping
    public Mono<Result<PageVO<UseUnitVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "20") int pageSize,
                                                @RequestParam(required = false) String unitName,
                                                @RequestParam(required = false) String unitCode) {
        return useUnitAppService.page(pageNum, pageSize, unitName, unitCode)
                .map(UseUnitVoConverter::toPageVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<UseUnitVO>> detail(@PathVariable Long id) {
        return useUnitAppService.detail(id)
                .map(UseUnitVoConverter::toVo)
                .map(Result::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return useUnitAppService.delete(id).then(Mono.just(Result.ok()));
    }
}

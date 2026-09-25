package com.somepro.interfaces.rest.unit;

import com.somepro.application.unit.UseUnitAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.unit.converter.UseUnitVoConverter;
import com.somepro.interfaces.rest.unit.request.UnitCreateRequest;
import com.somepro.interfaces.rest.unit.request.UnitUpdateRequest;
import com.somepro.interfaces.rest.unit.vo.UseUnitVO;
import com.somepro.interfaces.rest.common.vo.PageVO;
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
 * 使用单位登记接口（用户接口层）：只做协议适配与 VO 转换，编排交给应用层。
 *
 * - 编号 / 名称都可模糊搜，也可都不填翻全表，pageNum/pageSize 透传不写死
 * - 编号填错了走 PUT 整体改
 * - 单位不再用了走 DELETE（名下还有设备会被业务拦下）
 */
@RestController
@RequestMapping("/api/units")
public class UseUnitController {

    private final UseUnitAppService useUnitAppService;

    public UseUnitController(UseUnitAppService useUnitAppService) {
        this.useUnitAppService = useUnitAppService;
    }

    @PostMapping
    public Mono<Result<UseUnitVO>> register(@Valid @RequestBody UnitCreateRequest request) {
        return useUnitAppService.register(
                        request.unitCode(), request.unitName(), request.creditCode(),
                        request.district(), request.contactName(), request.contactPhone(),
                        request.status())
                .map(UseUnitVoConverter::toVo)
                .map(Result::ok);
    }

    @PutMapping("/{id}")
    public Mono<Result<UseUnitVO>> modify(@PathVariable Long id,
                                          @Valid @RequestBody UnitUpdateRequest request) {
        return useUnitAppService.modify(
                        id, request.unitCode(), request.unitName(), request.creditCode(),
                        request.district(), request.contactName(), request.contactPhone(),
                        request.status())
                .map(UseUnitVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<UseUnitVO>> detail(@PathVariable Long id) {
        return useUnitAppService.detail(id)
                .map(UseUnitVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping
    public Mono<Result<PageVO<UseUnitVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String unitCode,
            @RequestParam(required = false) String unitName,
            @RequestParam(required = false) String status) {
        return useUnitAppService.page(pageNum, pageSize, unitCode, unitName, status)
                .map(UseUnitVoConverter::toPageVo)
                .map(Result::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return useUnitAppService.delete(id).thenReturn(Result.ok());
    }
}

package com.somepro.interfaces.rest.equipment;

import com.somepro.application.equipment.EquipmentAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.equipment.converter.EquipmentVoConverter;
import com.somepro.interfaces.rest.equipment.vo.EquipmentVO;
import com.somepro.interfaces.rest.equipment.vo.SaveEquipmentRequest;
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
 * 特种设备档案接口（用户接口层）：只做协议适配，编排交给应用层。
 *
 * - POST   /api/equipments        登记设备（注册代码如 SB-2026-0031，挂在一个单位名下）
 * - PUT    /api/equipments/{id}   修改档案（支持注册代码纠错、改挂单位）
 * - GET    /api/equipments        分页：regCode 模糊匹配；可按 unitId / deviceType / status 过滤；都不填全量翻页
 * - GET    /api/equipments/{id}   详情（带归属单位名称）
 * - DELETE /api/equipments/{id}   删除
 *
 * 统一返回 Mono<Result<T>>；不直接返回领域对象，一律经 Converter 转 VO。
 */
@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentAppService equipmentAppService;

    public EquipmentController(EquipmentAppService equipmentAppService) {
        this.equipmentAppService = equipmentAppService;
    }

    @PostMapping
    public Mono<Result<EquipmentVO>> create(@Valid @RequestBody SaveEquipmentRequest request) {
        return equipmentAppService.register(request.regCode(), request.deviceType(), request.unitId(),
                        request.installAddr(), request.commissionDate(), request.periodMonth(),
                        request.nextInspectDate(), request.status())
                .map(EquipmentVoConverter::toVo)
                .map(Result::ok);
    }

    @PutMapping("/{id}")
    public Mono<Result<EquipmentVO>> update(@PathVariable Long id,
                                            @Valid @RequestBody SaveEquipmentRequest request) {
        return equipmentAppService.update(id, request.regCode(), request.deviceType(), request.unitId(),
                        request.installAddr(), request.commissionDate(), request.periodMonth(),
                        request.nextInspectDate(), request.status())
                .map(EquipmentVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping
    public Mono<Result<PageVO<EquipmentVO>>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) String regCode,
                                                  @RequestParam(required = false) Long unitId,
                                                  @RequestParam(required = false) String deviceType,
                                                  @RequestParam(required = false) String status) {
        return equipmentAppService.page(pageNum, pageSize, regCode, unitId, deviceType, status)
                .map(EquipmentVoConverter::toPageVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<EquipmentVO>> detail(@PathVariable Long id) {
        return equipmentAppService.detail(id)
                .map(EquipmentVoConverter::toVo)
                .map(Result::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<Result<Void>> delete(@PathVariable Long id) {
        return equipmentAppService.delete(id).then(Mono.just(Result.ok()));
    }
}

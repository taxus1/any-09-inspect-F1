package com.somepro.interfaces.rest.equipment;

import com.somepro.application.equipment.EquipmentAppService;
import com.somepro.common.Result;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.equipment.converter.EquipmentVoConverter;
import com.somepro.interfaces.rest.equipment.request.EquipmentCreateRequest;
import com.somepro.interfaces.rest.equipment.request.EquipmentUpdateRequest;
import com.somepro.interfaces.rest.equipment.vo.EquipmentVO;
import jakarta.validation.Valid;
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
 * 设备登记接口（用户接口层）：一台设备挂在一个单位名下（unitId 必填）。
 *
 * - 可按注册代码、按归属单位名称模糊搜，也可都不填翻全表
 * - 可再带 unitId 只看某一家的家底
 * - 注册代码填错了走 PUT 整体改；停用 / 封存 / 报废都是改 status，没有物理删除
 */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentAppService equipmentAppService;

    public EquipmentController(EquipmentAppService equipmentAppService) {
        this.equipmentAppService = equipmentAppService;
    }

    @PostMapping
    public Mono<Result<EquipmentVO>> register(@Valid @RequestBody EquipmentCreateRequest request) {
        return equipmentAppService.register(
                        request.regCode(), request.deviceType(), request.unitId(),
                        request.installAddr(), request.commissionDate(),
                        request.periodMonth(), request.nextInspectDate(), request.status())
                .map(EquipmentVoConverter::toVo)
                .map(Result::ok);
    }

    @PutMapping("/{id}")
    public Mono<Result<EquipmentVO>> modify(@PathVariable Long id,
                                            @Valid @RequestBody EquipmentUpdateRequest request) {
        return equipmentAppService.modify(
                        id, request.regCode(), request.deviceType(), request.unitId(),
                        request.installAddr(), request.commissionDate(),
                        request.periodMonth(), request.nextInspectDate(), request.status())
                .map(EquipmentVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping("/{id}")
    public Mono<Result<EquipmentVO>> detail(@PathVariable Long id) {
        return equipmentAppService.detail(id)
                .map(EquipmentVoConverter::toVo)
                .map(Result::ok);
    }

    @GetMapping
    public Mono<Result<PageVO<EquipmentVO>>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String regCode,
            @RequestParam(required = false) String unitName,
            @RequestParam(required = false) Long unitId) {
        return equipmentAppService.page(pageNum, pageSize, regCode, unitName, unitId)
                .map(EquipmentVoConverter::toPageVo)
                .map(Result::ok);
    }
}

package com.somepro.application;

import com.somepro.application.equipment.EquipmentAppService;
import com.somepro.application.unit.UseUnitAppService;
import com.somepro.common.exception.BizException;
import com.somepro.domain.equipment.model.DeviceType;
import com.somepro.domain.equipment.model.Equipment;
import com.somepro.domain.equipment.model.EquipmentStatus;
import com.somepro.domain.equipment.repository.EquipmentRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.domain.unit.model.UnitStatus;
import com.somepro.domain.unit.model.UseUnit;
import com.somepro.domain.unit.repository.UseUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 应用服务纯单测：仓储全 mock，只验证编排逻辑——编号查重、归属校验、删除拦截、枚举码解析。
 */
class AppServiceTest {

    private UseUnitRepository unitRepository;
    private EquipmentRepository equipmentRepository;
    private UseUnitAppService unitAppService;
    private EquipmentAppService equipmentAppService;

    @BeforeEach
    void setUp() {
        unitRepository = mock(UseUnitRepository.class);
        equipmentRepository = mock(EquipmentRepository.class);
        unitAppService = new UseUnitAppService(unitRepository, equipmentRepository);
        equipmentAppService = new EquipmentAppService(equipmentRepository, unitRepository);
        // 未显式打桩的查询一律视为「查不到」，避免 Mockito 返回 null 触发 NPE
        when(unitRepository.findById(anyLong())).thenReturn(Mono.empty());
        when(unitRepository.findByCode(any())).thenReturn(Mono.empty());
        when(equipmentRepository.findById(anyLong())).thenReturn(Mono.empty());
        when(equipmentRepository.findByRegCode(any())).thenReturn(Mono.empty());
        org.mockito.Mockito.doAnswer(inv -> Mono.just(inv.<UseUnit>getArgument(0))).when(unitRepository).save(any());
        org.mockito.Mockito.doAnswer(inv -> Mono.just(inv.<Equipment>getArgument(0))).when(equipmentRepository).save(any());
    }

    // ---------------- 单位 ----------------

    @Test
    void register_unit_with_duplicate_code_fails_with_friendly_message() {
        when(unitRepository.findByCode("SY-0001"))
                .thenReturn(Mono.just(UseUnit.register("SY-0001", "已存在单位", null,
                        null, null, null, UnitStatus.ACTIVE)));

        Throwable thrown = catchThrowable(() -> unitAppService.register(
                "SY-0001", "新单位", null, null, null, null, null).block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("编号已存在");
        verify(unitRepository, never()).save(any());
    }

    @Test
    void register_unit_then_default_status_active_and_saved() {
        when(unitRepository.findByCode(any())).thenReturn(Mono.empty());

        UseUnit saved = unitAppService.register(" SY-0001 ", "  新单位  ", null,
                null, null, null, null).block();
        assertThat(saved.getUnitCode()).isEqualTo("SY-0001");
        assertThat(saved.getUnitName()).isEqualTo("新单位");
        assertThat(saved.getStatus()).isEqualTo(UnitStatus.ACTIVE);
    }

    @Test
    void modify_unknown_unit_fails() {
        when(unitRepository.findById(99L)).thenReturn(Mono.empty());
        Throwable thrown = catchThrowable(() -> unitAppService.modify(
                99L, "SY-0001", "名", null, null, null, null, "ACTIVE").block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("单位不存在");
    }

    @Test
    void modify_unit_code_occupied_by_other_fails() {
        UseUnit self = UseUnit.register("SY-0001", "本单位", null, null, null, null, UnitStatus.ACTIVE);
        self.setId(1L);
        UseUnit other = UseUnit.register("SY-0002", "别的单位", null, null, null, null, UnitStatus.ACTIVE);
        other.setId(2L);
        when(unitRepository.findById(1L)).thenReturn(Mono.just(self));
        when(unitRepository.findByCode("SY-0002")).thenReturn(Mono.just(other));

        Throwable thrown = catchThrowable(() -> unitAppService.modify(
                1L, "SY-0002", "本单位", null, null, null, null, "ACTIVE").block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("已被占用");
        verify(unitRepository, never()).save(any());
    }

    @Test
    void modify_unit_keeps_same_code_allowed() {
        UseUnit self = UseUnit.register("SY-0001", "旧名", null, null, null, null, UnitStatus.ACTIVE);
        self.setId(1L);
        when(unitRepository.findById(1L)).thenReturn(Mono.just(self));
        org.mockito.Mockito.doAnswer(inv -> Mono.just(inv.<UseUnit>getArgument(0))).when(unitRepository).save(any());

        UseUnit updated = unitAppService.modify(1L, "SY-0001", "新名", null,
                null, null, null, "CANCELLED").block();
        assertThat(updated.getUnitName()).isEqualTo("新名");
        assertThat(updated.getStatus()).isEqualTo(UnitStatus.CANCELLED);
    }

    @Test
    void delete_unit_with_equipment_is_blocked() {
        UseUnit unit = UseUnit.register("SY-0001", "有家底的单位", null, null, null, null,
                UnitStatus.ACTIVE);
        unit.setId(1L);
        when(unitRepository.findById(1L)).thenReturn(Mono.just(unit));
        when(equipmentRepository.countByUnit(1L)).thenReturn(Mono.just(3L));

        Throwable thrown = catchThrowable(() -> unitAppService.delete(1L).block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("3 台设备");
        verify(unitRepository, never()).softDelete(anyLong());
    }

    @Test
    void delete_unit_without_equipment_succeeds_even_if_cancelled() {
        UseUnit unit = UseUnit.register("SY-0001", "已注销单位", null, null, null, null,
                UnitStatus.CANCELLED);
        unit.setId(1L);
        when(unitRepository.findById(1L)).thenReturn(Mono.just(unit));
        when(equipmentRepository.countByUnit(1L)).thenReturn(Mono.just(0L));
        when(unitRepository.softDelete(1L)).thenReturn(Mono.empty());

        unitAppService.delete(1L).block();
        verify(unitRepository).softDelete(1L);
    }

    @Test
    void invalid_status_code_fails() {
        Throwable thrown = catchThrowable(() -> unitAppService.register(
                "SY-0001", "名", null, null, null, null, "FOO").block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("ACTIVE");
    }

    @Test
    void page_parses_status_and_passes_keywords() {
        PageResult<UseUnit> result = new PageResult<>(List.of(), 0, 1, 20);
        when(unitRepository.page(eq(2), eq(10), eq("SY"), eq("电梯"), eq(UnitStatus.ACTIVE)))
                .thenReturn(Mono.just(result));

        PageResult<UseUnit> page = unitAppService.page(2, 10, "SY", "电梯", "ACTIVE").block();
        assertThat(page.total()).isZero();
        verify(unitRepository).page(2, 10, "SY", "电梯", UnitStatus.ACTIVE);
    }

    // ---------------- 设备 ----------------

    @Test
    void register_equipment_requires_existing_unit() {
        when(unitRepository.findById(anyLong())).thenReturn(Mono.empty());

        Throwable thrown = catchThrowable(() -> equipmentAppService.register(
                "SB-2026-0001", "ELEVATOR", 7L, null, null, 12, null, null).block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("归属单位不存在");
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void register_equipment_with_duplicate_reg_code_fails() {
        UseUnit unit = UseUnit.register("SY-0001", "单位", null, null, null, null, UnitStatus.ACTIVE);
        when(unitRepository.findById(1L)).thenReturn(Mono.just(unit));
        when(equipmentRepository.findByRegCode("SB-2026-0001"))
                .thenReturn(Mono.just(Equipment.register("SB-2026-0001", DeviceType.CRANE, 1L,
                        null, null, 12, null, EquipmentStatus.IN_USE)));

        Throwable thrown = catchThrowable(() -> equipmentAppService.register(
                "SB-2026-0001", "ELEVATOR", 1L, null, null, 12, null, null).block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("注册代码已存在");
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void register_equipment_success_with_defaults() {
        UseUnit unit = UseUnit.register("SY-0001", "单位", null, null, null, null, UnitStatus.ACTIVE);
        when(unitRepository.findById(1L)).thenReturn(Mono.just(unit));
        org.mockito.Mockito.doAnswer(inv -> Mono.just(inv.<Equipment>getArgument(0))).when(equipmentRepository).save(any());

        Equipment saved = equipmentAppService.register(
                "SB-2026-0031", "ELEVATOR", 1L, "1 号楼",
                LocalDate.of(2026, 1, 1), 12, LocalDate.of(2027, 1, 1), null).block();
        assertThat(saved.getDeviceType()).isEqualTo(DeviceType.ELEVATOR);
        assertThat(saved.getStatus()).isEqualTo(EquipmentStatus.IN_USE);
        assertThat(saved.getPeriodMonth()).isEqualTo(12);
    }

    @Test
    void modify_equipment_reg_code_taken_by_other_fails() {
        Equipment self = Equipment.register("SB-2026-0001", DeviceType.ELEVATOR, 1L,
                null, null, 12, null, EquipmentStatus.IN_USE);
        self.setId(10L);
        Equipment other = Equipment.register("SB-2026-0002", DeviceType.CRANE, 1L,
                null, null, 12, null, EquipmentStatus.IN_USE);
        other.setId(11L);
        UseUnit unit = UseUnit.register("SY-0001", "单位", null, null, null, null, UnitStatus.ACTIVE);
        when(equipmentRepository.findById(10L)).thenReturn(Mono.just(self));
        when(unitRepository.findById(1L)).thenReturn(Mono.just(unit));
        when(equipmentRepository.findByRegCode("SB-2026-0002")).thenReturn(Mono.just(other));

        Throwable thrown = catchThrowable(() -> equipmentAppService.modify(
                10L, "SB-2026-0002", "ELEVATOR", 1L, null, null, 12, null, "IN_USE").block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("已存在");
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    void modify_equipment_can_change_status_to_scrapped() {
        Equipment self = Equipment.register("SB-2026-0001", DeviceType.ELEVATOR, 1L,
                null, null, 12, null, EquipmentStatus.IN_USE);
        self.setId(10L);
        UseUnit unit = UseUnit.register("SY-0001", "单位", null, null, null, null, UnitStatus.ACTIVE);
        when(equipmentRepository.findById(10L)).thenReturn(Mono.just(self));
        when(unitRepository.findById(1L)).thenReturn(Mono.just(unit));
        when(equipmentRepository.findByRegCode("SB-2026-0001")).thenReturn(Mono.empty());
        org.mockito.Mockito.doAnswer(inv -> Mono.just(inv.<Equipment>getArgument(0))).when(equipmentRepository).save(any());

        Equipment updated = equipmentAppService.modify(
                10L, "SB-2026-0001", "BOILER", 1L, "新地点",
                LocalDate.of(2025, 3, 1), 24, LocalDate.of(2027, 3, 1), "SCRAPPED").block();
        assertThat(updated.getDeviceType()).isEqualTo(DeviceType.BOILER);
        assertThat(updated.getStatus()).isEqualTo(EquipmentStatus.SCRAPPED);
        assertThat(updated.getInstallAddr()).isEqualTo("新地点");
    }

    @Test
    void invalid_device_type_code_fails() {
        UseUnit unit = UseUnit.register("SY-0001", "单位", null, null, null, null, UnitStatus.ACTIVE);
        when(unitRepository.findById(1L)).thenReturn(Mono.just(unit));
        Throwable thrown = catchThrowable(() -> equipmentAppService.register(
                "SB-1", "FORKLIFT", 1L, null, null, 12, null, null).block());
        assertThat(thrown).isInstanceOf(BizException.class).hasMessageContaining("ELEVATOR");
    }
}

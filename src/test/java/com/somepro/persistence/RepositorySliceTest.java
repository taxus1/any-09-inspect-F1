package com.somepro.persistence;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * 仓储适配器切片测试：H2（MySQL 兼容模式）+ 真实 MyBatis-Plus + PageHelper，
 * 验证雪花 id、唯一索引、软删自动条件、分页、按单位名联查、单位快照回填。
 */
class RepositorySliceTest extends RepositorySliceTestBase {

    @Autowired
    private UseUnitRepository unitRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTables() {
        jdbcTemplate.update("DELETE FROM t_equipment");
        jdbcTemplate.update("DELETE FROM t_use_unit");
    }

    private UseUnit newUnit(String code, String name) {
        return UseUnit.register(code, name, "91310000MA1FL0XX" + code,
                "浦东新区", "张三", "13800000001", null);
    }

    @Test
    void unit_register_and_find_back_with_audit_fields() {
        UseUnit saved = unitRepository.save(newUnit("SY-0031", "华东电梯有限公司")).block();
        assertThat(saved.getId()).isNotNull();

        UseUnit loaded = unitRepository.findById(saved.getId()).block();
        assertThat(loaded.getUnitCode()).isEqualTo("SY-0031");
        assertThat(loaded.getStatus()).isEqualTo(UnitStatus.ACTIVE);
        assertThat(loaded.getCreateBy()).isEqualTo("system"); // 切片没有 OperatorWebFilter，审计兜底 system
        assertThat(loaded.getCreateTime()).isNotNull();
    }

    @Test
    void unit_duplicate_code_is_rejected_by_db() {
        unitRepository.save(newUnit("SY-0001", "甲单位")).block();
        Throwable thrown = catchThrowable(() ->
                unitRepository.save(newUnit("SY-0001", "乙单位")).block());
        // H2 抛原生唯一约束异常；MySQL 生产环境经 MyBatis 异常翻译为 DuplicateKeyException，语义一致
        assertThat(thrown.getMessage().toUpperCase()).contains("UNIQUE").contains("UNIT_CODE");
    }

    @Test
    void unit_page_search_by_name_and_code_and_paginate() {
        for (int i = 1; i <= 5; i++) {
            unitRepository.save(newUnit("SY-%04d".formatted(i), "华东电梯第%d分公司".formatted(i))).block();
        }
        unitRepository.save(UseUnit.register("SY-9001", " unrelated ",
                null, null, null, null, null)).block();

        assertThat(unitRepository.page(1, 2, null, null, null).block().total()).isEqualTo(6);
        assertThat(unitRepository.page(3, 2, null, null, null).block().content()).hasSize(2);
        assertThat(unitRepository.page(4, 2, null, null, null).block().content()).isEmpty();

        assertThat(unitRepository.page(1, 20, null, "华东电梯", null).block().total()).isEqualTo(5);
        PageResult<UseUnit> byCode = unitRepository.page(1, 20, "SY-9", null, null).block();
        assertThat(byCode.content()).hasSize(1);
        assertThat(byCode.content().get(0).getUnitName()).isEqualTo("unrelated");
    }

    @Test
    void unit_modify_clears_nullable_fields_and_changes_code() {
        Long id = unitRepository.save(UseUnit.register("SY-0002", "乙单位", "CREDIT-1",
                "黄浦区", "李四", "139", UnitStatus.ACTIVE)).block().getId();

        UseUnit unit = unitRepository.findById(id).block();
        unit.modify("SY-0099", "乙单位改名", null, null, null, null, UnitStatus.CANCELLED);
        unitRepository.save(unit).block();

        UseUnit reloaded = unitRepository.findById(id).block();
        assertThat(reloaded.getUnitCode()).isEqualTo("SY-0099");
        assertThat(reloaded.getCreditCode()).isNull();
        assertThat(reloaded.getDistrict()).isNull();
        assertThat(reloaded.getContactName()).isNull();
        assertThat(reloaded.getStatus()).isEqualTo(UnitStatus.CANCELLED);
    }

    @Test
    void soft_delete_hides_row_but_keeps_physical_row() {
        Long id = unitRepository.save(newUnit("SY-0003", "丙单位")).block().getId();
        unitRepository.softDelete(id).block();

        assertThat(unitRepository.findById(id).block()).isNull();
        assertThat(unitRepository.findByCode("SY-0003").block()).isNull();
        Integer physical = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_use_unit WHERE unit_code = 'SY-0003'", Integer.class);
        assertThat(physical).isEqualTo(1);
        Integer delFlag = jdbcTemplate.queryForObject(
                "SELECT del_flag FROM t_use_unit WHERE unit_code = 'SY-0003'", Integer.class);
        assertThat(delFlag).isEqualTo(1);
    }

    @Test
    void equipment_crud_page_by_unit_name_and_snapshot_fill() {
        Long unitA = unitRepository.save(newUnit("SY-1001", "华东锅炉厂")).block().getId();
        Long unitB = unitRepository.save(newUnit("SY-1002", "西部机械厂")).block().getId();

        for (int i = 1; i <= 3; i++) {
            equipmentRepository.save(Equipment.register(
                    "SB-2026-%04d".formatted(i), DeviceType.BOILER, unitA,
                    "一号厂房%d号位".formatted(i), LocalDate.of(2026, 1, 10),
                    12, LocalDate.of(2027, 1, 9), null)).block();
        }
        equipmentRepository.save(Equipment.register(
                "SB-2026-9001", DeviceType.CRANE, unitB, "露天堆场",
                LocalDate.of(2025, 6, 1), 24, LocalDate.of(2027, 6, 1),
                EquipmentStatus.SEALED)).block();

        Equipment first = equipmentRepository.findByRegCode("SB-2026-0001").block();
        assertThat(first.getStatus()).isEqualTo(EquipmentStatus.IN_USE);
        assertThat(first.getUnitName()).isEqualTo("华东锅炉厂");
        assertThat(first.getUnitCode()).isEqualTo("SY-1001");

        PageResult<Equipment> page = equipmentRepository.page(1, 20, null, "华东", null).block();
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.content()).allSatisfy(e ->
                assertThat(e.getUnitName()).isEqualTo("华东锅炉厂"));

        PageResult<Equipment> filtered =
                equipmentRepository.page(1, 20, "SB-2026-9", null, unitB).block();
        assertThat(filtered.content()).hasSize(1);
        assertThat(filtered.content().get(0).getRegCode()).isEqualTo("SB-2026-9001");

        assertThat(unitRepository.countEquipment(unitA).block()).isEqualTo(3L);

        assertThat(equipmentRepository.page(1, 20, null, "查无此单位", null).block().content())
                .isEmpty();
        assertThat(equipmentRepository.page(2, 2, null, null, null).block().content()).hasSize(2);
    }
}

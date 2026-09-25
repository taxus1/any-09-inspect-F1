package com.somepro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单位 + 设备档案端到端集成测试（H2 MySQL 兼容模式，无需外部 MySQL/Docker）。
 *
 * 全链路覆盖：Security 登录态（basic auth）→ Controller → AppService → 领域校验 →
 * MyBatis-Plus 落库（PageHelper 分页 / @TableLogic 软删 / 雪花 ID / 审计填充）→ VO 白名单。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // 沙箱注入了指向外部 MySQL 的 SPRING_DATASOURCE_URL 环境变量，这里强制切回 H2 内存库
                "spring.datasource.url=jdbc:h2:mem:it_inspect;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        })
@ActiveProfiles("it")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UnitEquipmentIT {

    @Autowired
    private WebTestClient client;

    private final ObjectMapper json = new ObjectMapper();

    private Long unitId;
    private Long otherUnitId;
    private Long equipmentId;

    private WebTestClient.RequestHeadersSpec<?> auth(WebTestClient.RequestHeadersSpec<?> spec) {
        return spec.header("Authorization",
                "Basic " + java.util.Base64.getEncoder().encodeToString("admin:admin123".getBytes()));
    }

    private JsonNode postJson(String uri, String body) {
        String resp = auth(client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        return parse(resp);
    }

    private JsonNode parse(String resp) {
        try {
            JsonNode node = json.readTree(resp);
            return node;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertCode0(JsonNode node) {
        assertEquals(0, node.path("code").asInt(), "期望成功，实际 msg=" + node.path("msg").asText());
    }

    @Test
    @Order(1)
    @DisplayName("登记单位：编号/名称/状态落库，默认 ACTIVE，审计人是登录人")
    void registerUnit() {
        String body = """
                {
                  "unitCode": "SY-0031",
                  "unitName": "  某某置业有限公司  ",
                  "creditCode": "91310000MA1FL0000A",
                  "district": "浦东新区",
                  "contactName": "王安全",
                  "contactPhone": "13800000001"
                }
                """;
        JsonNode node = postJson("/api/units", body);
        assertCode0(node);
        JsonNode data = node.path("data");
        unitId = data.path("id").asLong();
        assertTrue(unitId > 0, "雪花 id 应回填");
        assertEquals("SY-0031", data.path("unitCode").asText());
        assertEquals("某某置业有限公司", data.path("unitName").asText(), "名称应 trim");
        assertEquals("ACTIVE", data.path("status").asText(), "不传状态默认在册");
        assertEquals("王安全", data.path("contactName").asText());
        assertNotNull(data.path("createTime").asText(null));
        assertTrue(data.path("delFlag").isMissingNode(), "VO 不得外泄 delFlag");
        assertTrue(data.path("createBy").isMissingNode(), "VO 不得外泄 createBy");
    }

    @Test
    @Order(2)
    @DisplayName("单位编号不能撞")
    void duplicateUnitCodeRejected() {
        String body = """
                {"unitCode": "SY-0031", "unitName": "另一家公司"}
                """;
        JsonNode node = postJson("/api/units", body);
        assertEquals(1, node.path("code").asInt());
        assertTrue(node.path("msg").asText().contains("SY-0031"));
    }

    @Test
    @Order(3)
    @DisplayName("单位必填校验：名称缺失返回中文提示")
    void unitValidation() {
        String body = """
                {"unitCode": "SY-0032"}
                """;
        JsonNode node = postJson("/api/units", body);
        assertEquals(1, node.path("code").asInt());
        assertEquals("单位名称不能为空", node.path("msg").asText());
    }

    @Test
    @Order(4)
    @DisplayName("非法状态被拒")
    void illegalUnitStatusRejected() {
        String body = """
                {"unitCode": "SY-0033", "unitName": "状态测试公司", "status": "FOO"}
                """;
        JsonNode node = postJson("/api/units", body);
        assertEquals(1, node.path("code").asInt());
        assertTrue(node.path("msg").asText().contains("ACTIVE / CANCELLED"));
    }

    @Test
    @Order(5)
    @DisplayName("单位改档：编号纠错成 SY-0099，状态改 CANCELLED")
    void updateUnit() {
        String body = """
                {
                  "unitCode": "SY-0099",
                  "unitName": "某某置业股份有限公司",
                  "creditCode": "91310000MA1FL0000A",
                  "district": "浦东新区",
                  "contactName": "王安全",
                  "contactPhone": "13900000002",
                  "status": "CANCELLED"
                }
                """;
        String resp = auth(client.put().uri("/api/units/" + unitId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        JsonNode node = parse(resp);
        assertCode0(node);
        assertEquals("SY-0099", node.path("data").path("unitCode").asText());
        assertEquals("CANCELLED", node.path("data").path("status").asText());

        // 旧编号查不到、新编号查得到
        JsonNode list = parse(auth(client.get().uri("/api/units?pageNum=1&pageSize=20&unitCode=SY-0031"))
                .exchange().expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody());
        assertEquals(0, list.path("data").path("total").asLong());
    }

    @Test
    @Order(6)
    @DisplayName("改档时编号撞别人也不行")
    void updateUnitCodeConflict() {
        JsonNode other = postJson("/api/units", """
                {"unitCode": "SY-0100", "unitName": "第二个单位"}
                """);
        assertCode0(other);

        String body = """
                {"unitCode": "SY-0100", "unitName": "某某置业股份有限公司", "status": "ACTIVE"}
                """;
        String resp = auth(client.put().uri("/api/units/" + unitId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody();
        JsonNode node = parse(resp);
        assertEquals(1, node.path("code").asInt());
        assertTrue(node.path("msg").asText().contains("编号不能重复"));

        // 清掉本用例临时建的 SY-0100（名下无设备，可删），避免污染后续分页计数
        JsonNode cleaned = parse(auth(client.delete().uri("/api/units/" + other.path("data").path("id").asLong()))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertCode0(cleaned);
    }

    @Test
    @Order(7)
    @DisplayName("把单位改回 ACTIVE，并建第二个在册单位用于后续过滤测试")
    void prepareSecondUnit() {
        String body = """
                {
                  "unitCode": "SY-0099", "unitName": "某某置业股份有限公司",
                  "district": "浦东新区", "status": "ACTIVE"
                }
                """;
        JsonNode node = parse(auth(client.put().uri("/api/units/" + unitId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertCode0(node);

        JsonNode other = postJson("/api/units", """
                {"unitCode": "SY-0200", "unitName": "黄浦机械厂", "district": "黄浦区"}
                """);
        assertCode0(other);
        otherUnitId = other.path("data").path("id").asLong();
    }

    @Test
    @Order(8)
    @DisplayName("单位分页：都不填全量；按名称模糊；按编号模糊；越界页为空")
    void unitPaging() {
        JsonNode all = parse(auth(client.get().uri("/api/units?pageNum=1&pageSize=1"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertCode0(all);
        assertEquals(2, all.path("data").path("total").asLong());
        assertEquals(1, all.path("data").path("content").size());
        assertEquals(2, all.path("data").path("totalPages").asInt());

        JsonNode byName = parse(auth(client.get().uri("/api/units?unitName=机械"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, byName.path("data").path("total").asLong());
        assertEquals("SY-0200", byName.path("data").path("content").get(0).path("unitCode").asText());

        JsonNode byCode = parse(auth(client.get().uri("/api/units?unitCode=SY-00"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, byCode.path("data").path("total").asLong());
        assertEquals("SY-0099", byCode.path("data").path("content").get(0).path("unitCode").asText());

        JsonNode overflow = parse(auth(client.get().uri("/api/units?pageNum=99&pageSize=20"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(0, overflow.path("data").path("content").size());
    }

    @Test
    @Order(20)
    @DisplayName("登记设备：挂在单位下，注册代码/类别/周期/到期日落库，默认 IN_USE，回填单位名")
    void registerEquipment() {
        String body = """
                {
                  "regCode": "SB-2026-0031",
                  "deviceType": "ELEVATOR",
                  "unitId": %d,
                  "installAddr": "1 号楼客梯",
                  "commissionDate": "2026-01-15",
                  "periodMonth": 12,
                  "nextInspectDate": "2027-01-14"
                }
                """.formatted(unitId);
        JsonNode node = postJson("/api/equipments", body);
        assertCode0(node);
        JsonNode data = node.path("data");
        equipmentId = data.path("id").asLong();
        assertEquals("SB-2026-0031", data.path("regCode").asText());
        assertEquals("ELEVATOR", data.path("deviceType").asText());
        assertEquals(unitId.longValue(), data.path("unitId").asLong());
        assertEquals("IN_USE", data.path("status").asText());
        assertEquals(12, data.path("periodMonth").asInt());
        assertEquals("某某置业股份有限公司", data.path("unitName").asText(), "应回填归属单位名");
        assertTrue(data.path("delFlag").isMissingNode());
    }

    @Test
    @Order(21)
    @DisplayName("设备注册代码不能撞")
    void duplicateRegCodeRejected() {
        String body = """
                {"regCode": "SB-2026-0031", "deviceType": "BOILER", "unitId": %d}
                """.formatted(otherUnitId);
        JsonNode node = postJson("/api/equipments", body);
        assertEquals(1, node.path("code").asInt());
        assertTrue(node.path("msg").asText().contains("SB-2026-0031"));
    }

    @Test
    @Order(22)
    @DisplayName("设备必须挂在存在且在册的单位下")
    void unitMustExistAndBeActive() {
        String missing = """
                {"regCode": "SB-2026-9001", "deviceType": "CRANE", "unitId": 999999999999}
                """;
        JsonNode node1 = postJson("/api/equipments", missing);
        assertEquals(1, node1.path("code").asInt());
        assertTrue(node1.path("msg").asText().contains("归属单位不存在"));

        // 把第二家单位注销后再挂设备，应被拒，然后恢复
        String cancel = """
                {"unitCode": "SY-0200", "unitName": "黄浦机械厂", "district": "黄浦区", "status": "CANCELLED"}
                """;
        JsonNode cancelled = parse(auth(client.put().uri("/api/units/" + otherUnitId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(cancel))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertCode0(cancelled);

        String toCancelled = """
                {"regCode": "SB-2026-9002", "deviceType": "CRANE", "unitId": %d}
                """.formatted(otherUnitId);
        JsonNode node2 = postJson("/api/equipments", toCancelled);
        assertEquals(1, node2.path("code").asInt());
        assertTrue(node2.path("msg").asText().contains("已注销"));

        String resume = """
                {"unitCode": "SY-0200", "unitName": "黄浦机械厂", "district": "黄浦区", "status": "ACTIVE"}
                """;
        JsonNode resumed = parse(auth(client.put().uri("/api/units/" + otherUnitId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(resume))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertCode0(resumed);
    }

    @Test
    @Order(23)
    @DisplayName("设备类别非法 / 周期非正 / 到期日早于投用日，均被拒")
    void equipmentInvariants() {
        JsonNode badType = postJson("/api/equipments", """
                {"regCode": "SB-2026-8001", "deviceType": "LIFT", "unitId": %d}
                """.formatted(unitId));
        assertEquals(1, badType.path("code").asInt());
        assertTrue(badType.path("msg").asText().contains("ELEVATOR"));

        JsonNode badPeriod = postJson("/api/equipments", """
                {"regCode": "SB-2026-8002", "deviceType": "BOILER", "unitId": %d, "periodMonth": 0}
                """.formatted(unitId));
        assertEquals(1, badPeriod.path("code").asInt());
        assertEquals("检验周期（月）必须为正整数", badPeriod.path("msg").asText());

        JsonNode badDate = postJson("/api/equipments", """
                {"regCode": "SB-2026-8003", "deviceType": "BOILER", "unitId": %d,
                 "commissionDate": "2026-01-15", "nextInspectDate": "2025-12-31"}
                """.formatted(unitId));
        assertEquals(1, badDate.path("code").asInt());
        assertTrue(badDate.path("msg").asText().contains("不能早于投用日期"));
    }

    @Test
    @Order(24)
    @DisplayName("设备改档：注册代码纠错 + 改挂单位 + 状态改停用")
    void updateEquipment() {
        String body = """
                {
                  "regCode": "SB-2026-0099",
                  "deviceType": "BOILER",
                  "unitId": %d,
                  "installAddr": "动力车间 2 号炉",
                  "commissionDate": "2025-06-01",
                  "periodMonth": 24,
                  "nextInspectDate": "2027-06-01",
                  "status": "SUSPENDED"
                }
                """.formatted(otherUnitId);
        JsonNode node = parse(auth(client.put().uri("/api/equipments/" + equipmentId)
                        .contentType(MediaType.APPLICATION_JSON).bodyValue(body))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertCode0(node);
        JsonNode data = node.path("data");
        assertEquals("SB-2026-0099", data.path("regCode").asText());
        assertEquals("BOILER", data.path("deviceType").asText());
        assertEquals(otherUnitId.longValue(), data.path("unitId").asLong());
        assertEquals("黄浦机械厂", data.path("unitName").asText());
        assertEquals(24, data.path("periodMonth").asInt());
        assertEquals("SUSPENDED", data.path("status").asText());
    }

    @Test
    @Order(25)
    @DisplayName("设备分页：全量/注册代码模糊/按单位/按类别/按状态过滤，且带单位名")
    void equipmentPaging() {
        // 再给第一个单位登记一台压力容器
        JsonNode second = postJson("/api/equipments", """
                {"regCode": "SB-2026-0100", "deviceType": "PRESSURE_VESSEL", "unitId": %d,
                 "installAddr": "化工车间", "periodMonth": 36}
                """.formatted(unitId));
        assertCode0(second);

        JsonNode all = parse(auth(client.get().uri("/api/equipments"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(2, all.path("data").path("total").asLong());

        JsonNode byCode = parse(auth(client.get().uri("/api/equipments?regCode=0099"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, byCode.path("data").path("total").asLong());
        assertEquals("SB-2026-0099", byCode.path("data").path("content").get(0).path("regCode").asText());

        JsonNode byUnit = parse(auth(client.get().uri("/api/equipments?unitId=" + unitId))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, byUnit.path("data").path("total").asLong());
        assertEquals("SB-2026-0100", byUnit.path("data").path("content").get(0).path("regCode").asText());
        assertEquals("某某置业股份有限公司",
                byUnit.path("data").path("content").get(0).path("unitName").asText());

        JsonNode byType = parse(auth(client.get().uri("/api/equipments?deviceType=BOILER"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, byType.path("data").path("total").asLong());

        JsonNode byStatus = parse(auth(client.get().uri("/api/equipments?status=IN_USE"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, byStatus.path("data").path("total").asLong());
        assertEquals("SB-2026-0100", byStatus.path("data").path("content").get(0).path("regCode").asText());

        JsonNode page2 = parse(auth(client.get().uri("/api/equipments?pageNum=2&pageSize=1"))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(2, page2.path("data").path("total").asLong());
        assertEquals(1, page2.path("data").path("content").size());
    }

    @Test
    @Order(30)
    @DisplayName("名下还有设备的单位不能删；把设备删光后单位可删；软删后查不到")
    void deleteRules() {
        // 先删设备 SB-2026-0099（挂在 otherUnit 名下的那台）
        JsonNode delEq = parse(auth(client.delete().uri("/api/equipments/" + equipmentId))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertCode0(delEq);
        JsonNode gone = parse(auth(client.get().uri("/api/equipments/" + equipmentId))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, gone.path("code").asInt());

        // otherUnit 现在没有设备了，可以删
        JsonNode delUnit = parse(auth(client.delete().uri("/api/units/" + otherUnitId))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertCode0(delUnit);
        JsonNode unitGone = parse(auth(client.get().uri("/api/units/" + otherUnitId))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, unitGone.path("code").asInt());

        // unitId 名下还挂着 SB-2026-0100，删除应被拒
        JsonNode reject = parse(auth(client.delete().uri("/api/units/" + unitId))
                .exchange().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody());
        assertEquals(1, reject.path("code").asInt());
        assertTrue(reject.path("msg").asText().contains("还有 1 台设备"));
    }

    @Test
    @Order(40)
    @DisplayName("未认证访问被拒绝（全路由需认证）")
    void unauthenticatedRejected() {
        client.get().uri("/api/units")
                .exchange()
                // 同时启用了 formLogin + httpBasic：默认入口点返回 401（Www-Authenticate），不是 302
                .expectStatus().isUnauthorized();
    }
}

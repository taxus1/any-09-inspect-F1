package com.somepro.persistence;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.somepro.infrastructure.config.MybatisPlusConfig;
import com.somepro.infrastructure.persistence.audit.AutoFillMetaObjectHandler;
import com.somepro.infrastructure.persistence.equipment.EquipmentRepositoryImpl;
import com.somepro.infrastructure.persistence.unit.UseUnitRepositoryImpl;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 仓储 / 应用层切片测试基类：只起 JDBC + MyBatis-Plus + PageHelper + 审计填充 + 两个业务模块，
 * 排除 Redis / Security / WebFlux（与本切片无关，本机也没有 Redis）。
 */
@SpringBootTest(classes = RepositorySliceTestBase.SliceConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class RepositorySliceTestBase {

    /** 强制数据源指向 H2：环境里若注入了 DB_URL/DB_* 会覆盖 yml，必须在这里再压回去。 */
    @DynamicPropertySource
    static void h2Props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:inspect;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:schema-h2.sql");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
    }

    @Configuration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            SqlInitializationAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class
    })
    @Import({
            MybatisPlusConfig.class,
            AutoFillMetaObjectHandler.class,
            UseUnitRepositoryImpl.class,
            EquipmentRepositoryImpl.class
    })
    // 只扫两个模块的 Mapper，不碰 demo 模块
    @MapperScan(basePackages = {
            "com.somepro.infrastructure.persistence.unit",
            "com.somepro.infrastructure.persistence.equipment"
    }, annotationClass = Mapper.class)
    static class SliceConfig {
    }
}

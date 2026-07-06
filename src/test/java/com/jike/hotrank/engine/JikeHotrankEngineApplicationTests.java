package com.jike.hotrank.engine;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用启动测试
 */
@SpringBootTest(properties = "jike-hotrank.scheduling.enabled=false")
class JikeHotrankEngineApplicationTests {

    @Test
    void contextLoads() {
    }
}

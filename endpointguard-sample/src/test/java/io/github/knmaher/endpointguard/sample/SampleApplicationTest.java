package io.github.knmaher.endpointguard.sample;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SampleApplicationTest {
    @Autowired
    private ApplicationContext context;

    @Test
    void startsWithMvcAndTheRealSecurityFilterChain() {
        assertThat(context.getBeansOfType(RequestMappingHandlerMapping.class)).isNotEmpty();
        assertThat(context.getBeansOfType(SecurityFilterChain.class)).isNotEmpty();
    }
}

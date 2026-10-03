package io.github.knmaher.endpointguard.spring;

import java.util.List;
import java.util.UUID;

import io.github.knmaher.endpointguard.EndpointDescriptor;
import io.github.knmaher.endpointguard.HttpMethod;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

@SpringBootTest(classes = SpringMvcEndpointDiscoveryTest.TestApplication.class)
class SpringMvcEndpointDiscoveryTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void discoversApplicationEndpointsWithControllerAndHandler() throws Exception {
        List<EndpointDescriptor> endpoints = new SpringMvcEndpointDiscovery(context).discover();

        assertThat(endpoints).contains(
                new EndpointDescriptor(HttpMethod.GET, "/api/users", UserController.class,
                        UserController.class.getDeclaredMethod("getUsers")),
                new EndpointDescriptor(HttpMethod.DELETE, "/api/users/{id}", UserController.class,
                        UserController.class.getDeclaredMethod("deleteUser", UUID.class)));
    }

    @Test
    void listsEveryEndpointSortedByPathThenMethod() {
        List<String> endpoints = new SpringMvcEndpointDiscovery(context).discover().stream()
                .map(EndpointDescriptor::toString)
                .toList();

        assertThat(endpoints).containsExactly(
                "GET /",
                "GET /api/admin",
                "GET /api/any",
                "POST /api/any",
                "PUT /api/any",
                "PATCH /api/any",
                "DELETE /api/any",
                "GET /api/reports",
                "POST /api/reports",
                "GET /api/summaries",
                "POST /api/summaries",
                "GET /api/users",
                "DELETE /api/users/{id}");
    }

    @Test
    void reportsUserClassForProxiedControllers() {
        EndpointDescriptor admin = new SpringMvcEndpointDiscovery(context).discover().stream()
                .filter(endpoint -> endpoint.path().equals("/api/admin"))
                .findFirst()
                .orElseThrow();

        assertThat(admin.controllerClass()).isEqualTo(AdminController.class);
        assertThat(admin.handlerSignature()).isEqualTo("AdminController.admin()");
    }

    @Test
    void excludesFrameworkEndpointsUnlessEnabled() {
        assertThat(new SpringMvcEndpointDiscovery(context).discover())
                .noneMatch(endpoint -> endpoint.path().equals("/error"));

        assertThat(new SpringMvcEndpointDiscovery(context).includingFrameworkEndpoints().discover())
                .anyMatch(endpoint -> endpoint.path().equals("/error")
                        && endpoint.controllerClass().getName().startsWith("org.springframework.boot."));
    }

    @Test
    void failsClearlyWithoutSpringMvc() {
        try (AnnotationConfigApplicationContext plainContext = new AnnotationConfigApplicationContext()) {
            plainContext.refresh();

            assertThatIllegalStateException()
                    .isThrownBy(() -> new SpringMvcEndpointDiscovery(plainContext).discover())
                    .withMessageContaining("No Spring MVC RequestMappingHandlerMapping");
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMethodSecurity
    @Import({UserController.class, AdminController.class, MultiMappingController.class, RootController.class})
    static class TestApplication {
    }

    @RestController
    @RequestMapping("/api/users")
    static class UserController {

        @GetMapping
        List<String> getUsers() {
            return List.of();
        }

        @DeleteMapping("/{id}")
        void deleteUser(@PathVariable UUID id) {
        }
    }

    @RestController
    static class AdminController {

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/api/admin")
        String admin() {
            return "admin";
        }
    }

    @RestController
    static class MultiMappingController {

        @RequestMapping(path = {"/api/reports", "/api/summaries"}, method = {RequestMethod.GET, RequestMethod.POST})
        String reports() {
            return "reports";
        }

        @RequestMapping("/api/any")
        String any() {
            return "any";
        }
    }

    @RestController
    static class RootController {

        @GetMapping
        String root() {
            return "root";
        }
    }
}

package io.github.knmaher.endpointguard.sample;

import java.util.Map;
import java.util.UUID;

import io.github.knmaher.endpointguard.annotation.PublicEndpoint;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints covering the three access levels of the sample.
 *
 * <p>{@code GET /api/admin} is protected twice: by the URL rules and by {@code @PreAuthorize}.
 * {@code DELETE /api/admin/users/{id}} relies on the URL rules alone, which is common and is
 * exactly what a misplaced {@code permitAll()} breaks.
 */
@RestController
@RequestMapping("/api")
class ApiController {

    @PublicEndpoint
    @GetMapping("/public")
    Map<String, String> publicInfo() {
        return Map.of("message", "Hello, anyone");
    }

    @GetMapping("/profile")
    Map<String, String> profile() {
        return Map.of("message", "Hello, signed-in user");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    Map<String, String> admin() {
        return Map.of("message", "Hello, admin");
    }

    @DeleteMapping("/admin/users/{id}")
    ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }
}

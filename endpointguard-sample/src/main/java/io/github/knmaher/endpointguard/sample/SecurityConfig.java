package io.github.knmaher.endpointguard.sample;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two security configurations for the same endpoints: the correct one (default) and a
 * deliberately broken one (profile {@code insecure}) that EndpointGuard is expected to catch.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    @Profile("!insecure")
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/public").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /**
     * A realistic mistake: a broad rule meant to open "the API" is placed first. Spring Security
     * uses the first matching rule, so the admin rule below it never applies.
     */
    @Bean
    @Profile("insecure")
    SecurityFilterChain insecureSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /** Demo accounts for trying the sample with curl. Never use {noop} passwords in real code. */
    @Bean
    InMemoryUserDetailsManager users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("user").password("{noop}password").roles("USER").build(),
                User.withUsername("admin").password("{noop}password").roles("USER", "ADMIN").build());
    }
}

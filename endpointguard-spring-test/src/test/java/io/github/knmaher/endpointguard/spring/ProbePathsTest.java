package io.github.knmaher.endpointguard.spring;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProbePathsTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "/api/users                 | /api/users",
            "/api/users/{id}            | /api/users/1",
            "/api/users/{id:\\d+}       | /api/users/1",
            "/api/codes/{code:[a-z]{3}} | /api/codes/1",
            "/api/{a}/items/{b}         | /api/1/items/1",
            "/files/**                  | /files/1",
            "/files/*/meta              | /files/1/meta",
            "/files/{*path}             | /files/1",
            "/                          | /"
    })
    void replacesVariablesAndWildcards(String pattern, String expected) {
        assertThat(ProbePaths.toRequestPath(pattern)).isEqualTo(expected);
    }
}

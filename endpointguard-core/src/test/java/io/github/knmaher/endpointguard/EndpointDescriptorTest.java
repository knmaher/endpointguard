package io.github.knmaher.endpointguard;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class EndpointDescriptorTest {

    @Test
    void describesHandlerWithSimpleParameterTypes() throws Exception {
        EndpointDescriptor endpoint = descriptor(HttpMethod.DELETE, "/api/admin/users/{id}", "deleteUser", UUID.class, String.class);

        assertThat(endpoint.handlerSignature()).isEqualTo("AdminUserController.deleteUser(UUID, String)");
        assertThat(endpoint).hasToString("DELETE /api/admin/users/{id}");
    }

    @Test
    void describesHandlerWithoutParameters() throws Exception {
        assertThat(descriptor(HttpMethod.GET, "/api/admin/users", "listUsers").handlerSignature())
                .isEqualTo("AdminUserController.listUsers()");
    }

    @Test
    void sortsByPathThenMethod() throws Exception {
        EndpointDescriptor deleteUser = descriptor(HttpMethod.DELETE, "/api/admin/users/{id}", "deleteUser", UUID.class, String.class);
        EndpointDescriptor postUsers = descriptor(HttpMethod.POST, "/api/admin/users", "listUsers");
        EndpointDescriptor getUsers = descriptor(HttpMethod.GET, "/api/admin/users", "listUsers");

        assertThat(List.of(deleteUser, postUsers, getUsers).stream().sorted())
                .containsExactly(getUsers, postUsers, deleteUser);
    }

    @Test
    void rejectsRelativePath() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> descriptor(HttpMethod.GET, "api/admin/users", "listUsers"))
                .withMessageContaining("api/admin/users");
    }

    @Test
    void rejectsMissingValues() {
        assertThatNullPointerException()
                .isThrownBy(() -> new EndpointDescriptor(null, "/x", AdminUserController.class, null))
                .withMessage("method");
    }

    private static EndpointDescriptor descriptor(HttpMethod method, String path, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method handler = AdminUserController.class.getDeclaredMethod(name, parameterTypes);
        return new EndpointDescriptor(method, path, AdminUserController.class, handler);
    }

    @SuppressWarnings("unused")
    static class AdminUserController {
        void listUsers() {
        }

        void deleteUser(UUID id, String reason) {
        }
    }
}

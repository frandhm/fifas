package duoc.bff;

import duoc.bff.controller.ProxyController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;

class ProxyResponseTests {
    @Test void conservaStatusCuerpoYContentType() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST,
                HttpStatus.UNAUTHORIZED, HttpStatus.CONFLICT, HttpStatus.CREATED}) {
            var client = WebClient.builder().exchangeFunction(request -> {
                assertThat(request.headers().getFirst("Authorization")).isEqualTo("Bearer token");
                assertThat(request.url().getPath()).isEqualTo("/api/usuarios/me");
                return Mono.just(ClientResponse.create(status).header("Content-Type", "application/json")
                    .body("{\"code\":\"PROFILE_NOT_FOUND\"}").build());
            });
            var request = new MockHttpServletRequest("GET", "/api/usuarios/me");
            request.addHeader("Authorization", "Bearer token");
            var result = new ProxyController(client).proxy("usuarios", request, null);
            assertThat(result.getStatusCode()).isEqualTo(status);
            assertThat(result.getHeaders().getContentType().toString()).isEqualTo("application/json");
            assertThat(new String(result.getBody())).contains("PROFILE_NOT_FOUND");
        }
    }
    @Test void conservaRespuestaSinCuerpo() {
        var client = WebClient.builder().exchangeFunction(request ->
            Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build()));
        var result = new ProxyController(client).proxy("usuarios",
            new MockHttpServletRequest("DELETE", "/api/usuarios/1"), null);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
    }
}

package duoc.bff;

import duoc.bff.config.SecurityConfig;
import duoc.bff.controller.ProxyController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = PreflightTests.Config.class)
class PreflightTests {
    @TestConfiguration
    @EnableAutoConfiguration
    @Import({SecurityConfig.class, ProxyController.class})
    static class Config {
        @Bean JwtDecoder decoder() { return token -> { throw new JwtException("Invalid test token"); }; }
        @Bean WebClient.Builder client() { return WebClient.builder().exchangeFunction(request -> {
            throw new AssertionError("Preflight must not call the microservice");
        }); }
    }
    @Autowired WebApplicationContext context;
    @Test void preflightWorksWithoutTokenButGetRemainsProtected() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (String path : new String[]{"/api/notificaciones", "/api/productos", "/api/notificaciones/1"}) {
            mvc.perform(options(path).header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().is2xxSuccessful());
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }
}

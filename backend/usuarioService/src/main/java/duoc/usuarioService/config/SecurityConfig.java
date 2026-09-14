package duoc.usuarioService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/usuarios/me").hasAuthority("SCOPE_access_as_user")
                .requestMatchers(HttpMethod.PUT, "/api/usuarios/me").hasAuthority("SCOPE_access_as_user")
                // El CRUD antiguo no debe exponer perfiles de otros clientes.
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {})).build();
    }
}

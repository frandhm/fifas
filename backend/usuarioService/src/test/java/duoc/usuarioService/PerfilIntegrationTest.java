package duoc.usuarioService;

import duoc.usuarioService.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PerfilIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository repository;
    private static final String DATOS = """
        {"nombre":" Ana ","apellido":"Pérez","correo":"ana@example.com",
         "telefono":"+56912345678","direccion":"Santiago 123"}
        """;
    private RequestPostProcessor cuenta(String subject) {
        return jwt().jwt(j -> j.subject(subject).issuer("https://issuer.example"))
            .authorities(new SimpleGrantedAuthority("SCOPE_access_as_user"));
    }
    @BeforeEach void limpiar() { repository.deleteAll(); }

    @Test void guardaRecuperaYActualizaSinDuplicar() throws Exception {
        mvc.perform(get("/api/usuarios/me").with(cuenta("ana")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROFILE_NOT_FOUND"));
        mvc.perform(put("/api/usuarios/me").with(cuenta("ana"))
                .contentType(MediaType.APPLICATION_JSON).content(DATOS))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.nombre").value("Ana"))
            .andExpect(jsonPath("$.propietario").doesNotExist());
        mvc.perform(put("/api/usuarios/me").with(cuenta("ana"))
                .contentType(MediaType.APPLICATION_JSON).content(DATOS.replace("Santiago 123", "Valparaíso 456")))
            .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/me").with(cuenta("ana")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.direccion").value("Valparaíso 456"));
        assertThat(repository.count()).isEqualTo(1);
    }
    @Test void aislaCuentasAunqueCompartanCorreoYNoAceptaPropietarioDelCliente() throws Exception {
        mvc.perform(put("/api/usuarios/me").with(cuenta("ana"))
            .contentType(MediaType.APPLICATION_JSON).content(DATOS)).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios/me").with(cuenta("otra"))).andExpect(status().isNotFound());
        mvc.perform(put("/api/usuarios/me").with(cuenta("otra"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(DATOS.replace("{", "{\"id\":1,\"propietario\":\"https://issuer.example|ana\",")))
            .andExpect(status().isOk());
        assertThat(repository.count()).isEqualTo(2);
        mvc.perform(get("/api/usuarios").with(cuenta("ana"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/1").with(cuenta("ana"))).andExpect(status().isForbidden());
    }
    @Test void exigeAutenticacionScopeYDatosValidos() throws Exception {
        mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/me").with(cuenta("ana"))
            .contentType(MediaType.APPLICATION_JSON).content(DATOS.replace("ana@example.com", "invalido")))
            .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }
}

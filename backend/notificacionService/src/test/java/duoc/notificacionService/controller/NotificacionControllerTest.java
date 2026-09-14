package duoc.notificacionService.controller;
import duoc.notificacionService.entity.Notificacion;
import duoc.notificacionService.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotificacionControllerTest {
    private final NotificacionRepository repository = mock(NotificacionRepository.class, withSettings().mockMaker("mock-maker-proxy"));
    private final NotificacionController controller = new NotificacionController(repository);
    private Jwt token(String sub) {
        return Jwt.withTokenValue("test").header("alg", "RS256").issuer("https://issuer.example").subject(sub).build();
    }
    @Test void scopesListToTokenOwner() {
        controller.listar(token("alice"));
        verify(repository).findByPropietarioOrderByIdDesc("https://issuer.example|alice");
        verify(repository, never()).findAll();
    }
    @Test void createsOwnerAndTimestampOnServer() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = controller.crear(new NotificacionController.Nueva("Bienvenido", "sesion"), token("alice"));
        assertEquals("https://issuer.example|alice", result.getPropietario());
        assertFalse(result.getLeido()); assertNotNull(result.getFecha()); assertNull(result.getUsuarioId());
    }
    @Test void cannotReadOrDeleteAnotherUsersNotification() {
        when(repository.findByIdAndPropietario(1L, "https://issuer.example|bob")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> controller.leer(1L, new NotificacionController.Lectura(true), token("bob")));
        assertThrows(ResponseStatusException.class, () -> controller.eliminar(1L, token("bob")));
        verify(repository, never()).save(any()); verify(repository, never()).delete(any(Notificacion.class));
    }
}

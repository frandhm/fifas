package duoc.kafkaService.controller;

import duoc.kafkaService.messaging.AlertaListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de ConsultaController.
 * Verifica la consulta de alertas acumuladas y actividad de usuarios.
 */
class ConsultaControllerTest {

    private final AlertaListener listener = mock(AlertaListener.class);
    private final ConsultaController controller = new ConsultaController(listener);

    @Test
    void retornaListaDeAlertas() {
        when(listener.getAlertas()).thenReturn(List.of("alerta-1", "alerta-2"));

        List<String> alertas = controller.alertas();

        assertEquals(2, alertas.size());
        assertEquals("alerta-1", alertas.get(0));
        assertEquals("alerta-2", alertas.get(1));
        verify(listener).getAlertas();
    }

    @Test
    void retornaListaDeActividad() {
        when(listener.getActividad()).thenReturn(List.of("actividad-1"));

        List<String> actividad = controller.actividad();

        assertEquals(1, actividad.size());
        assertEquals("actividad-1", actividad.get(0));
        verify(listener).getActividad();
    }

    @Test
    void retornaListasVaciasCuandoNoHayEventos() {
        when(listener.getAlertas()).thenReturn(List.of());
        when(listener.getActividad()).thenReturn(List.of());

        assertTrue(controller.alertas().isEmpty());
        assertTrue(controller.actividad().isEmpty());
    }
}


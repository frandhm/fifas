package duoc.productoService.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import duoc.productoService.entity.Producto;
import duoc.productoService.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de integración/componente para ProductoListener.
 * Verifica:
 * 1. Incremento de unidades vendidas y confirmación con ACK manual.
 * 2. Validación atómica: si un producto no existe, se rechaza todo (NACK -> DLQ).
 * 3. Rechazo de cantidades inválidas (<= 0) o IDs vacíos.
 * 4. Agrupación correcta de cantidades duplicadas del mismo producto en una misma orden.
 * 5. Manejo resiliente ante JSONs inválidos o excepciones.
 */
class ProductoListenerTest {

    private ProductoService service;
    private Channel canal;
    private ProductoListener listener;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(ProductoService.class);
        canal = mock(Channel.class);
        listener = new ProductoListener(service, mapper);
    }

    // ──── Caso Exitoso (ACK) ────

    @Test
    void actualizaUnidadesVendidasYConfirmaConAck() throws IOException {
        Producto p1 = new Producto();
        p1.setId("CAM-01");
        p1.setName("Camiseta Real Madrid");
        p1.setUnidadesVendidas(10);

        Producto p2 = new Producto();
        p2.setId("CAM-02");
        p2.setName("Camiseta Barcelona");
        p2.setUnidadesVendidas(null); // Caso inicial nulo

        when(service.buscarPorId("CAM-01")).thenReturn(Optional.of(p1));
        when(service.buscarPorId("CAM-02")).thenReturn(Optional.of(p2));

        String cuerpo = """
            {
              "items": [
                {"productoId": "CAM-01", "cantidad": 3},
                {"productoId": "CAM-02", "cantidad": 2}
              ]
            }
            """;
        long tag = 50L;

        listener.recibir(cuerpo, tag, canal);

        // Verifica que se guardaron ambos con las unidades incrementadas
        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(service, times(2)).guardar(captor.capture());

        Producto p1Actualizado = captor.getAllValues().get(0);
        assertEquals("CAM-01", p1Actualizado.getId());
        assertEquals(13, p1Actualizado.getUnidadesVendidas()); // 10 + 3

        Producto p2Actualizado = captor.getAllValues().get(1);
        assertEquals("CAM-02", p2Actualizado.getId());
        assertEquals(2, p2Actualizado.getUnidadesVendidas()); // 0 + 2

        verify(canal).basicAck(tag, false);
        verify(canal, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void agrupaCantidadesRepetidasDelMismoProducto() throws IOException {
        Producto p = new Producto();
        p.setId("CAM-01");
        p.setUnidadesVendidas(5);

        when(service.buscarPorId("CAM-01")).thenReturn(Optional.of(p));

        String cuerpo = """
            {
              "items": [
                {"productoId": "CAM-01", "cantidad": 2},
                {"productoId": "CAM-01", "cantidad": 3}
              ]
            }
            """;
        long tag = 51L;

        listener.recibir(cuerpo, tag, canal);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(service, times(1)).guardar(captor.capture());
        assertEquals(10, captor.getValue().getUnidadesVendidas()); // 5 + 2 + 3

        verify(canal).basicAck(tag, false);
    }

    // ──── Validaciones y Rechazos (NACK -> DLQ) ────

    @Test
    void rechazaYEnviaADlqSiProductoNoExiste() throws IOException {
        Producto pExistente = new Producto();
        pExistente.setId("CAM-01");
        pExistente.setUnidadesVendidas(5);

        when(service.buscarPorId("CAM-01")).thenReturn(Optional.of(pExistente));
        when(service.buscarPorId("CAM-INEXISTENTE")).thenReturn(Optional.empty());

        String cuerpo = """
            {
              "items": [
                {"productoId": "CAM-01", "cantidad": 1},
                {"productoId": "CAM-INEXISTENTE", "cantidad": 2}
              ]
            }
            """;
        long tag = 52L;

        listener.recibir(cuerpo, tag, canal);

        // Se debe rechazar toda la operación sin modificar ningún producto (atomicidad)
        verify(canal).basicNack(tag, false, false);
        verify(canal, never()).basicAck(anyLong(), anyBoolean());
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaCantidadInvalidaMenorAUno() throws IOException {
        String cuerpo = """
            {
              "items": [
                {"productoId": "CAM-01", "cantidad": 0}
              ]
            }
            """;
        long tag = 53L;

        listener.recibir(cuerpo, tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaProductoIdVacio() throws IOException {
        String cuerpo = """
            {
              "items": [
                {"productoId": "   ", "cantidad": 2}
              ]
            }
            """;
        long tag = 54L;

        listener.recibir(cuerpo, tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaListaDeItemsVacia() throws IOException {
        String cuerpo = "{\"items\": []}";
        long tag = 55L;

        listener.recibir(cuerpo, tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }

    @Test
    void rechazaJsonInvalidoYEnviaADlq() throws IOException {
        String cuerpo = "{ no_es_un_json_valido }";
        long tag = 56L;

        listener.recibir(cuerpo, tag, canal);

        verify(canal).basicNack(tag, false, false);
        verify(service, never()).guardar(any());
    }
}


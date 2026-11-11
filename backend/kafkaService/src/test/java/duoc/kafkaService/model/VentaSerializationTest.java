package duoc.kafkaService.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de serialización y deserialización del DTO Venta,
 * incluyendo JSON inválidos y campos nulos.
 */
class VentaSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ──── Deserialización exitosa ────

    @Test
    void deserializaVentaCompletaDesdeJson() throws Exception {
        String json = """
            {
              "solicitudId": 42,
              "productoId": "CAMISETA-001",
              "cantidad": 3,
              "precioUnitario": 29990.0,
              "total": 89970.0,
              "timestamp": "2026-11-10T12:00:00Z"
            }
            """;
        Venta v = mapper.readValue(json, Venta.class);
        assertEquals(42L, v.solicitudId());
        assertEquals("CAMISETA-001", v.productoId());
        assertEquals(3, v.cantidad());
        assertEquals(29990.0, v.precioUnitario());
        assertEquals(89970.0, v.total());
        assertEquals("2026-11-10T12:00:00Z", v.timestamp());
    }

    @Test
    void deserializaVentaConCamposNulos() throws Exception {
        String json = """
            {
              "solicitudId": null,
              "productoId": null,
              "cantidad": null,
              "precioUnitario": null,
              "total": null,
              "timestamp": null
            }
            """;
        Venta v = mapper.readValue(json, Venta.class);
        assertNull(v.solicitudId());
        assertNull(v.productoId());
        assertNull(v.cantidad());
        assertNull(v.precioUnitario());
        assertNull(v.total());
        assertNull(v.timestamp());
    }

    @Test
    void deserializaVentaConCamposFaltantes() throws Exception {
        String json = "{}";
        Venta v = mapper.readValue(json, Venta.class);
        assertNull(v.solicitudId());
        assertNull(v.productoId());
        assertNull(v.cantidad());
    }

    @Test
    void rechazaCamposDesconocidosPorDefecto() {
        String json = """
            {
              "solicitudId": 1,
              "productoId": "X",
              "cantidad": 1,
              "precioUnitario": 100.0,
              "total": 100.0,
              "timestamp": "2026-01-01T00:00:00Z",
              "campoExtra": "ignorado"
            }
            """;
        // ObjectMapper por defecto falla ante campos desconocidos si no está configurado FAIL_ON_UNKNOWN_PROPERTIES=false
        assertThrows(Exception.class, () -> mapper.readValue(json, Venta.class));
    }

    // ──── JSON inválidos ────

    @Test
    void fallaConJsonMalFormado() {
        String jsonInvalido = "{ productoId: sin_comillas }";
        assertThrows(Exception.class, () -> mapper.readValue(jsonInvalido, Venta.class));
    }

    @Test
    void fallaConStringVacio() {
        assertThrows(Exception.class, () -> mapper.readValue("", Venta.class));
    }

    @Test
    void fallaConTipoIncorrecto() {
        // cantidad debería ser Integer, no un string
        String json = """
            { "cantidad": "no_es_numero" }
            """;
        assertThrows(Exception.class, () -> mapper.readValue(json, Venta.class));
    }

    // ──── Serialización ────

    @Test
    void serializaVentaAjson() throws Exception {
        Venta v = new Venta(1L, "PROD-1", 2, 50000.0, 100000.0, "2026-11-10T12:00:00Z");
        String json = mapper.writeValueAsString(v);
        assertTrue(json.contains("\"productoId\":\"PROD-1\""));
        assertTrue(json.contains("\"total\":100000.0"));
    }

    @Test
    void serializaYDeserializaRoundTrip() throws Exception {
        Venta original = new Venta(99L, "CAM-CHILE", 5, 35000.0, 175000.0, "2026-06-15T18:30:00Z");
        String json = mapper.writeValueAsString(original);
        Venta recuperada = mapper.readValue(json, Venta.class);
        assertEquals(original, recuperada);
    }
}


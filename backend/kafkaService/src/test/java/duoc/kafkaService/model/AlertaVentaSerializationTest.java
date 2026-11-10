package duoc.kafkaService.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de serialización y deserialización del DTO AlertaVenta,
 * incluyendo JSON inválidos.
 */
class AlertaVentaSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void deserializaAlertaCompleta() throws Exception {
        String json = """
            {
              "productoId": "CAM-BRASIL",
              "cantidad": 10,
              "total": 500000.0,
              "motivo": "Venta de alto valor",
              "severidad": "ALTA",
              "timestamp": "2026-11-10T15:00:00Z"
            }
            """;
        AlertaVenta a = mapper.readValue(json, AlertaVenta.class);
        assertEquals("CAM-BRASIL", a.productoId());
        assertEquals(10, a.cantidad());
        assertEquals(500000.0, a.total());
        assertEquals("Venta de alto valor", a.motivo());
        assertEquals("ALTA", a.severidad());
    }

    @Test
    void serializaAlertaAjson() throws Exception {
        AlertaVenta a = new AlertaVenta("PROD-1", 3, 150000.0, "Venta grande", "ALTA", "2026-01-01T00:00:00Z");
        String json = mapper.writeValueAsString(a);
        assertTrue(json.contains("\"motivo\":\"Venta grande\""));
        assertTrue(json.contains("\"severidad\":\"ALTA\""));
    }

    @Test
    void roundTripAlerta() throws Exception {
        AlertaVenta original = new AlertaVenta("X", 1, 999.0, "Test", "BAJA", "ts");
        String json = mapper.writeValueAsString(original);
        AlertaVenta copia = mapper.readValue(json, AlertaVenta.class);
        assertEquals(original, copia);
    }

    @Test
    void fallaConJsonInvalido() {
        assertThrows(Exception.class, () -> mapper.readValue("no_json", AlertaVenta.class));
    }

    @Test
    void deserializaConCamposNulos() throws Exception {
        String json = """
            {
              "productoId": null,
              "cantidad": null,
              "total": null,
              "motivo": null,
              "severidad": null,
              "timestamp": null
            }
            """;
        AlertaVenta a = mapper.readValue(json, AlertaVenta.class);
        assertNull(a.productoId());
        assertNull(a.cantidad());
        assertNull(a.total());
    }
}


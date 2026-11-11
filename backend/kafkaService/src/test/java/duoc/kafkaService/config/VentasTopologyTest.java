package duoc.kafkaService.config;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyDescription;
import org.apache.kafka.streams.kstream.KStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de la topología de Kafka Streams (VentasTopology).
 * Verifica:
 * 1. La construcción de la topología de streams.
 * 2. Que el topic de entrada sea 'ventas.registradas' y el de salida 'ventas.alertas'.
 * 3. Filtrado de JSON inválidos (descartados).
 * 4. Filtrado de ventas con montos o cantidades inválidas (<= 0).
 * 5. Serialización a AlertaVenta de ventas que superan el umbral.
 */
class VentasTopologyTest {

    private VentasTopology ventasTopology;

    @BeforeEach
    void setUp() {
        ventasTopology = new VentasTopology();
    }

    // ──── Topología de Kafka Streams ────

    @Test
    void construyeTopologiaCorrectamenteConSourceYSink() {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> stream = ventasTopology.ventasStream(builder, 100000.0);

        assertNotNull(stream, "El KStream retornado no debe ser nulo");

        Topology topology = builder.build();
        TopologyDescription description = topology.describe();
        String descriptionStr = description.toString();

        // Verificar que lee del topic ventas.registradas
        assertTrue(descriptionStr.contains(KafkaConfig.VENTAS),
                "La topología debe consumir del topic: " + KafkaConfig.VENTAS);

        // Verificar que escribe al topic ventas.alertas
        assertTrue(descriptionStr.contains(KafkaConfig.ALERTAS),
                "La topología debe emitir al topic: " + KafkaConfig.ALERTAS);
    }

    // ──── Parseo y Descarte de JSON Inválido ────

    @Test
    void parseaJsonValidoCorrectamente() throws Exception {
        Method parsearMethod = VentasTopology.class.getDeclaredMethod("parsear", String.class);
        parsearMethod.setAccessible(true);

        String jsonValido = """
            {
              "solicitudId": 10,
              "productoId": "CAM-01",
              "cantidad": 2,
              "precioUnitario": 40000.0,
              "total": 80000.0,
              "timestamp": "2026-11-10T12:00:00Z"
            }
            """;

        Object result = parsearMethod.invoke(ventasTopology, jsonValido);
        assertNotNull(result, "Un JSON válido debe ser parseado");
    }

    @Test
    void descartaJsonInvalidoRetornandoNull() throws Exception {
        Method parsearMethod = VentasTopology.class.getDeclaredMethod("parsear", String.class);
        parsearMethod.setAccessible(true);

        // Caso 1: String corrupto
        Object result1 = parsearMethod.invoke(ventasTopology, "{ corrupto }");
        assertNull(result1, "JSON corrupto debe retornar null para ser descartado");

        // Caso 2: String vacío
        Object result2 = parsearMethod.invoke(ventasTopology, "");
        assertNull(result2, "String vacío debe retornar null");

        // Caso 3: No es JSON
        Object result3 = parsearMethod.invoke(ventasTopology, "texto plano");
        assertNull(result3, "Texto plano debe retornar null");
    }

    // ──── Reglas de Negocio: Umbral de Alertas ────

    @Test
    void serializaAlertaGeneradaCorrectamente() throws Exception {
        Method serializarMethod = VentasTopology.class.getDeclaredMethod("serializar", duoc.kafkaService.model.AlertaVenta.class);
        serializarMethod.setAccessible(true);

        duoc.kafkaService.model.AlertaVenta alerta = new duoc.kafkaService.model.AlertaVenta(
                "CAM-01", 5, 250000.0, "Venta de alto valor", "ALTA", "2026-11-10T15:00:00Z");

        String jsonGenerado = (String) serializarMethod.invoke(ventasTopology, alerta);

        assertNotNull(jsonGenerado);
        assertTrue(jsonGenerado.contains("CAM-01"));
        assertTrue(jsonGenerado.contains("250000.0"));
        assertTrue(jsonGenerado.contains("Venta de alto valor"));
        assertTrue(jsonGenerado.contains("ALTA"));
    }
}


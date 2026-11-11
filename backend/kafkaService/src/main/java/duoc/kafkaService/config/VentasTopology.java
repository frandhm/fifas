package duoc.kafkaService.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import duoc.kafkaService.model.AlertaVenta;
import duoc.kafkaService.model.Venta;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VentasTopology {
    private static final Logger log = LoggerFactory.getLogger(VentasTopology.class);
    private final ObjectMapper mapper = new ObjectMapper();

    @Bean
    public KStream<String, String> ventasStream(StreamsBuilder builder,
            @Value("${app.umbral-venta-grande}") double umbral) {

        KStream<String, String> entrada = builder.stream(KafkaConfig.VENTAS,
                Consumed.with(Serdes.String(), Serdes.String()));

        entrada
            // 1) Parsear: si el JSON es inválido, se descarta
            .mapValues(this::parsear)
            .filter((k, v) -> v != null)
            // 2) Filtro: ignorar ventas sin monto válido
            .filter((k, v) -> v.total() != null && v.total() > 0 && v.cantidad() != null && v.cantidad() > 0)
            .peek((k, v) -> log.info("Venta procesada: {} total={}", v.productoId(), v.total()))
            // 3) Detección: venta grande
            .filter((k, v) -> v.total() > umbral)
            .mapValues(v -> new AlertaVenta(v.productoId(), v.cantidad(), v.total(),
                    "Venta de alto valor", "ALTA", v.timestamp()))
            .peek((k, a) -> log.warn("ALERTA generada: {} total={}", a.productoId(), a.total()))
            .mapValues(this::serializar)
            // 4) Salida
            .to(KafkaConfig.ALERTAS, Produced.with(Serdes.String(), Serdes.String()));

        return entrada;
    }

    private Venta parsear(String json) {
        try { return mapper.readValue(json, Venta.class); }
        catch (Exception e) { log.warn("Venta inválida descartada: {}", json); return null; }
    }

    private String serializar(AlertaVenta a) {
        try { return mapper.writeValueAsString(a); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
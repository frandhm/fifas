package duoc.kafkaService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
@EnableKafkaStreams
public class KafkaConfig {
    public static final String VENTAS = "ventas.registradas";
    public static final String ALERTAS = "ventas.alertas";
    public static final String ACTIVIDAD = "actividad.usuario";

    // Si el topic ya existe, no lo modifica
    @Bean public NewTopic ventasTopic() { return TopicBuilder.name(VENTAS).partitions(3).replicas(1).build(); }
    @Bean public NewTopic alertasTopic() { return TopicBuilder.name(ALERTAS).partitions(3).replicas(1).build(); }
    @Bean public NewTopic actividadTopic() { return TopicBuilder.name(ACTIVIDAD).partitions(3).replicas(1).build(); }
}
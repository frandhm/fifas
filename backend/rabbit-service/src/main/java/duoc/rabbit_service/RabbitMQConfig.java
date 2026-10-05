package duoc.rabbit_service;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "eventos_exchange";
    public static final String DLX = "eventos_dlx";

    public static final String NOTIF_QUEUE = "notificaciones_queue";
    public static final String SOPORTE_QUEUE = "soporte_queue";
    public static final String NOTIF_DLQ = "notificaciones_dlq";
    public static final String SOPORTE_DLQ = "soporte_dlq";

    // Exchanges
    @Bean
    public DirectExchange eventosExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange eventosDlx() {
        return new DirectExchange(DLX, true, false);
    }

    // Colas principales (si un mensaje falla, van al DLX)
    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(NOTIF_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey("notificaciones.dlq")
                .build();
    }

    @Bean
    public Queue soporteQueue() {
        return QueueBuilder.durable(SOPORTE_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey("soporte.dlq")
                .build();
    }

    // Colas de mensajes fallidos (DLQ)
    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(NOTIF_DLQ).build();
    }

    @Bean
    public Queue soporteDlq() {
        return QueueBuilder.durable(SOPORTE_DLQ).build();
    }

    // Bindings de las colas principales
    @Bean
    public Binding notifCarrito() {
        return BindingBuilder.bind(notificacionesQueue()).to(eventosExchange()).with("carrito.agregado");
    }

    @Bean
    public Binding notifSolicitud() {
        return BindingBuilder.bind(notificacionesQueue()).to(eventosExchange()).with("solicitud.creada");
    }

    @Bean
    public Binding notifSoporte() {
        return BindingBuilder.bind(notificacionesQueue()).to(eventosExchange()).with("soporte.mensaje");
    }

    @Bean
    public Binding soporteMensaje() {
        return BindingBuilder.bind(soporteQueue()).to(eventosExchange()).with("soporte.mensaje");
    }

    // Bindings de las colas de fallidos
    @Bean
    public Binding notifDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq()).to(eventosDlx()).with("notificaciones.dlq");
    }

    @Bean
    public Binding soporteDlqBinding() {
        return BindingBuilder.bind(soporteDlq()).to(eventosDlx()).with("soporte.dlq");
    }

    // Abre la conexión al arrancar para que RabbitMQ cree todo de inmediato
    @Bean
    public ApplicationRunner crearTopologia(ConnectionFactory connectionFactory) {
        return args -> connectionFactory.createConnection().close();
    }
}
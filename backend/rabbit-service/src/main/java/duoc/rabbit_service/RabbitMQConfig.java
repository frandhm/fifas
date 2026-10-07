package duoc.rabbit_service;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    public static final String EXCHANGE = "eventos_exchange";
    public static final String DLX = "eventos_dlx";

    // Colas principales
    public static final String NOTIF_QUEUE = "notificaciones_queue";
    public static final String SOPORTE_QUEUE = "soporte_queue";
    public static final String SOLICITUDES_QUEUE = "solicitudes_queue";
    public static final String PRODUCTO_QUEUE = "producto_queue";

    // Colas de mensajes fallidos
    public static final String NOTIF_DLQ = "notificaciones_dlq";
    public static final String SOPORTE_DLQ = "soporte_dlq";
    public static final String SOLICITUDES_DLQ = "solicitudes_dlq";
    public static final String PRODUCTO_DLQ = "producto_dlq";

    // ---------- Exchanges ----------
    @Bean
    public DirectExchange eventosExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange eventosDlx() {
        return new DirectExchange(DLX, true, false);
    }

    // ---------- Colas principales (si un mensaje falla, van al DLX) ----------
    private Queue colaPrincipal(String nombre, String claveDlq) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(claveDlq)
                .build();
    }

    @Bean
    public Queue notificacionesQueue() {
        return colaPrincipal(NOTIF_QUEUE, "notificaciones.dlq");
    }

    @Bean
    public Queue soporteQueue() {
        return colaPrincipal(SOPORTE_QUEUE, "soporte.dlq");
    }

    @Bean
    public Queue solicitudesQueue() {
        return colaPrincipal(SOLICITUDES_QUEUE, "solicitudes.dlq");
    }

    @Bean
    public Queue productoQueue() {
        return colaPrincipal(PRODUCTO_QUEUE, "producto.dlq");
    }

    // ---------- Colas de fallidos (DLQ) ----------
    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(NOTIF_DLQ).build();
    }

    @Bean
    public Queue soporteDlq() {
        return QueueBuilder.durable(SOPORTE_DLQ).build();
    }

    @Bean
    public Queue solicitudesDlq() {
        return QueueBuilder.durable(SOLICITUDES_DLQ).build();
    }

    @Bean
    public Queue productoDlq() {
        return QueueBuilder.durable(PRODUCTO_DLQ).build();
    }

    // ---------- Bindings de las colas principales ----------
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
    public Binding notifSesion() {
        return BindingBuilder.bind(notificacionesQueue()).to(eventosExchange()).with("sesion.iniciada");
    }

    @Bean
    public Binding soporteMensaje() {
        return BindingBuilder.bind(soporteQueue()).to(eventosExchange()).with("soporte.mensaje");
    }

    @Bean
    public Binding solicitudesCarrito() {
        return BindingBuilder.bind(solicitudesQueue()).to(eventosExchange()).with("carrito.confirmado");
    }

    @Bean
    public Binding productoSolicitud() {
        return BindingBuilder.bind(productoQueue()).to(eventosExchange()).with("solicitud.creada");
    }

    // ---------- Bindings de las colas de fallidos ----------
    @Bean
    public Binding notifDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq()).to(eventosDlx()).with("notificaciones.dlq");
    }

    @Bean
    public Binding soporteDlqBinding() {
        return BindingBuilder.bind(soporteDlq()).to(eventosDlx()).with("soporte.dlq");
    }

    @Bean
    public Binding solicitudesDlqBinding() {
        return BindingBuilder.bind(solicitudesDlq()).to(eventosDlx()).with("solicitudes.dlq");
    }

    @Bean
    public Binding productoDlqBinding() {
        return BindingBuilder.bind(productoDlq()).to(eventosDlx()).with("producto.dlq");
    }

    // Abre la conexión al arrancar para que RabbitMQ cree todo de inmediato
    @Bean
    public ApplicationRunner crearTopologia(ConnectionFactory connectionFactory) {
        return args -> connectionFactory.createConnection().close();
    }
}
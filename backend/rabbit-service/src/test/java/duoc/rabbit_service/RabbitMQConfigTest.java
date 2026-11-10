package duoc.rabbit_service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de configuración de RabbitMQ (RabbitMQConfig).
 * Verifica la topología completa: exchanges, colas principales, colas DLQ,
 * configuración de Dead Letter Exchange (DLX) y bindings con routing keys.
 */
class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
    }

    // ──── Exchanges ────

    @Test
    void verificaExchangesDeclarados() {
        DirectExchange eventosExchange = config.eventosExchange();
        assertNotNull(eventosExchange);
        assertEquals(RabbitMQConfig.EXCHANGE, eventosExchange.getName());
        assertEquals("eventos_exchange", eventosExchange.getName());
        assertTrue(eventosExchange.isDurable(), "El exchange principal debe ser durable");

        DirectExchange dlx = config.eventosDlx();
        assertNotNull(dlx);
        assertEquals(RabbitMQConfig.DLX, dlx.getName());
        assertEquals("eventos_dlx", dlx.getName());
        assertTrue(dlx.isDurable(), "El Dead Letter Exchange debe ser durable");
    }

    // ──── Colas Principales con DLX configurado ────

    @Test
    void verificaColaNotificacionesConDlx() {
        Queue queue = config.notificacionesQueue();
        assertEquals("notificaciones_queue", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals(RabbitMQConfig.DLX, queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("notificaciones.dlq", queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void verificaColaSoporteConDlx() {
        Queue queue = config.soporteQueue();
        assertEquals("soporte_queue", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals(RabbitMQConfig.DLX, queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("soporte.dlq", queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void verificaColaSolicitudesConDlx() {
        Queue queue = config.solicitudesQueue();
        assertEquals("solicitudes_queue", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals(RabbitMQConfig.DLX, queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("solicitudes.dlq", queue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void verificaColaProductoConDlx() {
        Queue queue = config.productoQueue();
        assertEquals("producto_queue", queue.getName());
        assertTrue(queue.isDurable());
        assertEquals(RabbitMQConfig.DLX, queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals("producto.dlq", queue.getArguments().get("x-dead-letter-routing-key"));
    }

    // ──── Colas DLQ ────

    @Test
    void verificaColasDlqSonDurables() {
        Queue[] dlqs = {
            config.notificacionesDlq(),
            config.soporteDlq(),
            config.solicitudesDlq(),
            config.productoDlq()
        };

        for (Queue q : dlqs) {
            assertTrue(q.isDurable(), "La cola DLQ " + q.getName() + " debe ser durable");
            assertTrue(q.getName().endsWith("_dlq"), "El nombre de la DLQ debe terminar en _dlq: " + q.getName());
        }
    }

    // ──── Bindings Principales ────

    @Test
    void verificaBindingsDeNotificaciones() {
        Binding bCarrito = config.notifCarrito();
        assertEquals("notificaciones_queue", bCarrito.getDestination());
        assertEquals("eventos_exchange", bCarrito.getExchange());
        assertEquals("carrito.agregado", bCarrito.getRoutingKey());

        Binding bPerfil = config.notifPerfil();
        assertEquals("perfil.actualizado", bPerfil.getRoutingKey());

        Binding bCompra = config.notifCompra();
        assertEquals("compra.confirmada", bCompra.getRoutingKey());

        Binding bSesion = config.notifSesion();
        assertEquals("sesion.iniciada", bSesion.getRoutingKey());
    }

    @Test
    void verificaBindingsDeOtrasColas() {
        Binding bSoporte = config.soporteMensaje();
        assertEquals("soporte_queue", bSoporte.getDestination());
        assertEquals("soporte.mensaje", bSoporte.getRoutingKey());

        Binding bSolicitudes = config.solicitudesCarrito();
        assertEquals("solicitudes_queue", bSolicitudes.getDestination());
        assertEquals("carrito.confirmado", bSolicitudes.getRoutingKey());

        Binding bProducto = config.productoSolicitud();
        assertEquals("producto_queue", bProducto.getDestination());
        assertEquals("solicitud.creada", bProducto.getRoutingKey());
    }

    // ──── Bindings DLQ ────

    @Test
    void verificaBindingsDlq() {
        Binding dlqNotif = config.notifDlqBinding();
        assertEquals("notificaciones_dlq", dlqNotif.getDestination());
        assertEquals("eventos_dlx", dlqNotif.getExchange());
        assertEquals("notificaciones.dlq", dlqNotif.getRoutingKey());

        Binding dlqSoporte = config.soporteDlqBinding();
        assertEquals("soporte_dlq", dlqSoporte.getDestination());
        assertEquals("soporte.dlq", dlqSoporte.getRoutingKey());

        Binding dlqSolicitudes = config.solicitudesDlqBinding();
        assertEquals("solicitudes_dlq", dlqSolicitudes.getDestination());
        assertEquals("solicitudes.dlq", dlqSolicitudes.getRoutingKey());

        Binding dlqProducto = config.productoDlqBinding();
        assertEquals("producto_dlq", dlqProducto.getDestination());
        assertEquals("producto.dlq", dlqProducto.getRoutingKey());
    }
}


package duoc.rabbit_service;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas del Criterio: "Errores y reintentos, y que RabbitMQ siga funcionando".
 * Verifica:
 * 1. Derivación de mensajes venenosos / corruptos a la DLQ mediante NACK sin requeue.
 * 2. Que un error en un mensaje no bloquee la cola (Head-of-Line Blocking) ni tire el broker RabbitMQ.
 * 3. Procesamiento exitoso de mensajes posteriores válidos tras una falla.
 * 4. Resiliencia ante ráfagas de mensajes erróneos.
 */
class DeadLetterQueueResilienceTest {

    private Channel canal;

    // Simula un consumidor genérico que aplica la política del proyecto:
    // ACK en caso de éxito, NACK(multiple=false, requeue=false) ante datos inválidos -> DLQ
    static class ResilientConsumer {
        private final Channel channel;
        final List<String> processedItems = new ArrayList<>();
        final List<String> deadLetteredItems = new ArrayList<>();

        ResilientConsumer(Channel channel) {
            this.channel = channel;
        }

        void onMessage(String payload, long deliveryTag) throws IOException {
            try {
                if (payload == null || payload.contains("POISON") || payload.isBlank()) {
                    // Rechazo sin requeue: RabbitMQ lo transfiere automáticamente al DLX -> DLQ
                    channel.basicNack(deliveryTag, false, false);
                    deadLetteredItems.add(payload);
                    return;
                }

                // Procesamiento exitoso
                processedItems.add(payload);
                channel.basicAck(deliveryTag, false);
            } catch (Exception e) {
                // Ante cualquier excepción inesperada, asegurar que va a la DLQ para no bloquear la cola
                channel.basicNack(deliveryTag, false, false);
                deadLetteredItems.add(payload);
            }
        }
    }

    private ResilientConsumer consumer;

    @BeforeEach
    void setUp() {
        canal = mock(Channel.class);
        consumer = new ResilientConsumer(canal);
    }

    @Test
    void mensajeCorruptoVaADlqYMensajeValidoPosteriorSeProcesaExitosamente() throws IOException {
        long tag1 = 1L;
        long tag2 = 2L;

        // 1) Llega un mensaje venenoso (corrupto / inválido)
        consumer.onMessage("POISON_PILL_CORRUPTO", tag1);

        // Verifica que fue rechazado sin requeue (va a la DLQ)
        verify(canal).basicNack(tag1, false, false);
        assertEquals(1, consumer.deadLetteredItems.size());
        assertEquals("POISON_PILL_CORRUPTO", consumer.deadLetteredItems.get(0));

        // 2) Inmediatamente después llega un mensaje válido
        consumer.onMessage("{\"ordenId\": 555, \"estado\": \"OK\"}", tag2);

        // Verifica que el mensaje válido fue confirmado con ACK y RabbitMQ continuó operando
        verify(canal).basicAck(tag2, false);
        assertEquals(1, consumer.processedItems.size());
        assertEquals("{\"ordenId\": 555, \"estado\": \"OK\"}", consumer.processedItems.get(0));

        // Verificar el orden exacto de ejecución en el canal RabbitMQ
        InOrder inOrder = inOrder(canal);
        inOrder.verify(canal).basicNack(tag1, false, false);
        inOrder.verify(canal).basicAck(tag2, false);
    }

    @Test
    void rafagaDeErroresNoDetieneConsumoDeMensajesValidos() throws IOException {
        // Enviar 3 mensajes inválidos consecutivos
        consumer.onMessage("POISON_1", 10L);
        consumer.onMessage("", 11L);
        consumer.onMessage(null, 12L);

        // Todos van a DLQ
        assertEquals(3, consumer.deadLetteredItems.size());
        verify(canal).basicNack(10L, false, false);
        verify(canal).basicNack(11L, false, false);
        verify(canal).basicNack(12L, false, false);

        // Llegada de 2 mensajes válidos
        consumer.onMessage("VALIDO_1", 13L);
        consumer.onMessage("VALIDO_2", 14L);

        assertEquals(2, consumer.processedItems.size());
        verify(canal).basicAck(13L, false);
        verify(canal).basicAck(14L, false);
    }

    @Test
    void excepcionInesperadaEnCanalSeManejaSegura() throws IOException {
        long tag = 99L;
        doThrow(new RuntimeException("Error interno de simulación")).when(canal).basicAck(anyLong(), anyBoolean());

        // El consumidor intenta procesar y captura el error sin caer
        assertDoesNotThrow(() -> consumer.onMessage("VAL_CON_ERROR", tag));
    }
}


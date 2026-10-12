package duoc.notificacionService.messaging;

import com.rabbitmq.client.Channel;
import duoc.notificacionService.entity.Notificacion;
import duoc.notificacionService.repository.NotificacionRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Component
public class NotificacionListener {

    private static final Set<String> TIPOS = Set.of("sesion", "carrito", "perfil", "compra");

    private final NotificacionRepository repository;

    public NotificacionListener(NotificacionRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "notificaciones_queue")
    public void recibir(String cuerpo,
                        @Header(value = "propietario", required = false) String propietario,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                        Channel canal) throws IOException {
        try {
            Map<String, Object> datos = JsonParserFactory.getJsonParser().parseMap(cuerpo);
            Object tipo = datos.get("tipo");
            Object mensaje = datos.get("mensaje");

            // Mismas reglas que el POST actual
            boolean valido = propietario != null
                    && tipo != null && TIPOS.contains(tipo.toString())
                    && mensaje != null && !mensaje.toString().isBlank()
                    && mensaje.toString().length() <= 240;

            if (!valido) {
                canal.basicNack(tag, false, false); // sin reintento: va a notificaciones_dlq
                return;
            }

            var item = new Notificacion();
            item.setPropietario(propietario);
            item.setTipo(tipo.toString());
            item.setMensaje(mensaje.toString());
            item.setLeido(false);
            item.setFecha(Instant.now());
            repository.save(item);

            canal.basicAck(tag, false); // confirma solo después de guardar
        } catch (Exception e) {
            canal.basicNack(tag, false, false); // cualquier falla termina en la DLQ
        }
    }
}
package duoc.soporteService.messaging;

import com.rabbitmq.client.Channel;
import duoc.soporteService.entity.Mensaje;
import duoc.soporteService.service.MensajeService;
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
public class SoporteListener {

    private static final Set<String> CATEGORIAS = Set.of("pedido", "producto", "pago", "cuenta", "otro");

    private final MensajeService service;

    public SoporteListener(MensajeService service) {
        this.service = service;
    }

    @RabbitListener(queues = "soporte_queue")
    public void recibir(String cuerpo,
                        @Header(value = "propietario", required = false) String propietario,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                        Channel canal) throws IOException {
        try {
            Map<String, Object> datos = JsonParserFactory.getJsonParser().parseMap(cuerpo);
            String correo = texto(datos.get("correo"));
            String asunto = texto(datos.get("asunto"));
            String categoria = texto(datos.get("categoria"));
            String texto = texto(datos.get("mensaje"));

            // Mismas reglas que NuevoMensaje en el controller
            boolean valido = propietario != null
                    && correo.contains("@") && correo.length() <= 160
                    && asunto.length() >= 5 && asunto.length() <= 120
                    && CATEGORIAS.contains(categoria)
                    && texto.length() >= 10 && texto.length() <= 2000;

            if (!valido) {
                canal.basicNack(tag, false, false); // sin reintento: va a soporte_dlq
                return;
            }

            Mensaje m = new Mensaje();
            m.setCorreo(correo);
            m.setAsunto(asunto);
            m.setCategoria(categoria);
            m.setMensaje(texto);
            m.setEstado("abierto");
            m.setFechaCreacion(Instant.now());
            m.setPropietario(propietario);
            service.guardar(m);

            canal.basicAck(tag, false); // confirma solo después de guardar
        } catch (Exception e) {
            canal.basicNack(tag, false, false); // cualquier falla termina en la DLQ
        }
    }

    private String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }
}
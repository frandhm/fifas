package duoc.solicitudService.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import duoc.solicitudService.controller.SolicitudController;
import duoc.solicitudService.entity.Solicitud;
import duoc.solicitudService.repository.SolicitudRepository;
import jakarta.validation.Validator;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class SolicitudListener {

    private final SolicitudRepository repository;
    private final ObjectMapper mapper;
    private final Validator validator;
    private final RabbitTemplate rabbitTemplate;

    public SolicitudListener(SolicitudRepository repository, ObjectMapper mapper,
                             Validator validator, RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.validator = validator;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "solicitudes_queue")
    public void recibir(String cuerpo,
                        @Header(value = "propietario", required = false) String propietario,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                        Channel canal) throws IOException {
        try {
            var compra = mapper.readValue(cuerpo, SolicitudController.NuevaCompra.class);

            // Mismas validaciones que el POST del controller
            if (propietario == null || !validator.validate(compra).isEmpty()) {
                canal.basicNack(tag, false, false); // va a solicitudes_dlq
                return;
            }

            List<Solicitud> guardadas = repository.saveAll(
                    compra.items().stream().map(i -> crear(i, propietario)).toList());

            // Avisa a producto con los datos de lo que se vendió
            var items = guardadas.stream()
                    .map(s -> Map.of("productoId", s.getProductoId(), "cantidad", s.getCantidad()))
                    .toList();
            rabbitTemplate.convertAndSend("eventos_exchange", "solicitud.creada",
                    mapper.writeValueAsString(Map.of("items", items)));

            canal.basicAck(tag, false);
        } catch (Exception e) {
            canal.basicNack(tag, false, false);
        }
    }

    private Solicitud crear(SolicitudController.NuevaSolicitud body, String propietario) {
        var s = new Solicitud();
        s.setPropietario(propietario);
        s.setProductoId(body.productoId());
        s.setProductoNombre(body.productoNombre().trim());
        s.setCantidad(body.cantidad());
        s.setPrecioUnitario(body.precioUnitario());
        s.setTalla(body.talla());
        s.setNombreEstampado(body.nombreEstampado());
        s.setNumeroEstampado(body.numeroEstampado());
        s.setEstado("PENDIENTE");
        s.setFechaCreacion(Instant.now());
        return s;
    }
}
package duoc.productoService.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import duoc.productoService.entity.Producto;
import duoc.productoService.service.ProductoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ProductoListener {

    private final ProductoService service;
    private final ObjectMapper mapper;

    public ProductoListener(ProductoService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @RabbitListener(queues = "producto_queue")
    public void recibir(String cuerpo,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                        Channel canal) throws IOException {
        try {
            JsonNode items = mapper.readTree(cuerpo).path("items");
            Map<String, Integer> cantidades = new LinkedHashMap<>();

            // 1) Validar todo antes de modificar nada
            for (JsonNode item : items) {
                String id = item.path("productoId").asText("");
                int cantidad = item.path("cantidad").asInt(0);
                if (id.isBlank() || cantidad < 1) {
                    canal.basicNack(tag, false, false); // va a producto_dlq
                    return;
                }
                cantidades.merge(id, cantidad, Integer::sum);
            }
            if (cantidades.isEmpty()) {
                canal.basicNack(tag, false, false);
                return;
            }
            for (String id : cantidades.keySet()) {
                if (service.buscarPorId(id).isEmpty()) {
                    canal.basicNack(tag, false, false); // producto inexistente
                    return;
                }
            }

            // 2) Aplicar
            cantidades.forEach((id, cantidad) -> {
                Producto p = service.buscarPorId(id).get();
                int actuales = p.getUnidadesVendidas() == null ? 0 : p.getUnidadesVendidas();
                p.setUnidadesVendidas(actuales + cantidad);
                service.guardar(p);
            });

            canal.basicAck(tag, false);
        } catch (Exception e) {
            canal.basicNack(tag, false, false);
        }
    }
}
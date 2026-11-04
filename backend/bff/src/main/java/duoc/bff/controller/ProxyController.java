package duoc.bff.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.Map;

@RestController
public class ProxyController {

    private final WebClient.Builder webClientBuilder;

    // Aqui defines a que microservicio corresponde cada primer segmento de la ruta
    private static final String MC1_HOST = System.getenv().getOrDefault("MC1_HOST", "http://172.31.31.56");
    private static final String MC2_HOST = System.getenv().getOrDefault("MC2_HOST", "http://172.31.23.71");
    private static final String RABBIT_HOST = System.getenv().getOrDefault("RABBIT_HOST", "http://172.31.27.189");
    private static final String KAFKA_HOST = System.getenv().getOrDefault("KAFKA_HOST", "http://172.31.22.133");

    private static final Map<String, String> RUTAS = Map.of(
        "usuarios", MC2_HOST + ":8082",
        "productos", MC2_HOST + ":8083",
        "carritos", MC2_HOST + ":8084",
        "solicitudes", MC1_HOST + ":8087",
        "notificaciones", MC1_HOST + ":8086",
        "mensajes", MC1_HOST + ":8088",
        "rabbit", RABBIT_HOST + ":8085",
        "kafka", KAFKA_HOST + ":8091"
    );

    public ProxyController(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @RequestMapping("/api/{servicio}/**")
    public ResponseEntity<byte[]> proxy(
            @PathVariable String servicio,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        // API Gateway adds CORS headers; preflight must never reach a protected microservice.
        if ("OPTIONS".equals(request.getMethod())) {
            return ResponseEntity.noContent().build();
        }

        String baseUrl = RUTAS.get(servicio);
        if (baseUrl == null) {
            return ResponseEntity.notFound().build();
        }

        String rutaCompleta = request.getRequestURI();
        String queryString = request.getQueryString();
        String urlDestino = baseUrl + rutaCompleta + (queryString != null ? "?" + queryString : "");

        HttpMethod metodo = HttpMethod.valueOf(request.getMethod());

        WebClient.RequestBodySpec peticion = webClientBuilder.build()
                .method(metodo)
                .uri(urlDestino)
                .headers(headers -> Collections.list(request.getHeaderNames()).forEach(nombre -> {
                    if (!nombre.equalsIgnoreCase("host") && !nombre.equalsIgnoreCase("content-length")) {
                        headers.add(nombre, request.getHeader(nombre));
                    }
                }));

        WebClient.RequestHeadersSpec<?> salida = (body != null && body.length > 0)
                ? peticion.bodyValue(body) : peticion;
        // Conservar 404/400/401/409 y respuestas sin cuerpo; retrieve() convertía errores en 500.
        return salida.exchangeToMono(response -> response.toEntity(byte[].class)).block();
    }
}
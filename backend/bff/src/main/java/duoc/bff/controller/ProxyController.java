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
    private static final Map<String, String> RUTAS = Map.of(
        "usuarios", "http://172.31.24.223:8082",
        "productos", "http://172.31.24.223:8083",
        "carritos", "http://172.31.24.223:8084",
        "solicitudes", "http://172.31.95.160:8087",
        "notificaciones", "http://172.31.95.160:8086",
        "mensajes", "http://172.31.95.160:8088"
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
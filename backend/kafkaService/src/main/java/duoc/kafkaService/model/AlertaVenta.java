package duoc.kafkaService.model;

public record AlertaVenta(String productoId, Integer cantidad, Double total,
                          String motivo, String severidad, String timestamp) {}
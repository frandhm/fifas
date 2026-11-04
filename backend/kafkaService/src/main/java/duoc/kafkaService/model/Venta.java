package duoc.kafkaService.model;

public record Venta(Long solicitudId, String productoId, Integer cantidad,
                    Double precioUnitario, Double total, String timestamp) {}
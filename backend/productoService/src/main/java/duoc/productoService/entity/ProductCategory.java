package duoc.productoService.entity;

public enum ProductCategory {
    LOCAL("local"),
    VISITANTE("visitante"),
    TERCERA("tercera"),
    PORTERO("portero"),
    RETRO("retro");

    private final String valor;

    ProductCategory(String valor) {
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }
}
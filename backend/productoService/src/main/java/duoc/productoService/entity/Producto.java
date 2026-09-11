package duoc.productoService.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    private String id;

    private String name;
    private Double price;
    private String accent;
    private String stripe;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    private String description;

    @ElementCollection
    @CollectionTable(name = "producto_sizes", joinColumns = @JoinColumn(name = "producto_id"))
    @Column(name = "size")
    private List<String> sizes;

    public Producto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getAccent() { return accent; }
    public void setAccent(String accent) { this.accent = accent; }

    public String getStripe() { return stripe; }
    public void setStripe(String stripe) { this.stripe = stripe; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getSizes() { return sizes; }
    public void setSizes(List<String> sizes) { this.sizes = sizes; }
}
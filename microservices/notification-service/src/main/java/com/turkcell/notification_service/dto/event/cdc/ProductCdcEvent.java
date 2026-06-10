package com.turkcell.notification_service.dto.event.cdc;

import java.math.BigDecimal;

/**
 * Product CDC Event DTO
 * Debezium'un product-service.public.products tablosundan
 * okuduğu verileri eşlemek için kullanılan DTO.
 */
public class ProductCdcEvent {
    
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String description;
    
    public ProductCdcEvent() {
    }
    
    public ProductCdcEvent(Long id, String name, BigDecimal price, Integer stock, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getStock() {
        return stock;
    }
    
    public void setStock(Integer stock) {
        this.stock = stock;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "ProductCdcEvent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", description='" + description + '\'' +
                '}';
    }
}

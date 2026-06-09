package com.turkcell.order_service.dto;

public class OrderRequest {
    
    private Long productId;
    private Integer quantity;
    private Double totalAmount;
    
    public OrderRequest() {
    }
    
    public OrderRequest(Long productId, Integer quantity, Double totalAmount) {
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}

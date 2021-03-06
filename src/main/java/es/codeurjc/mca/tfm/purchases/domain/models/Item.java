package es.codeurjc.mca.tfm.purchases.domain.models;

/**
 * Item domain entity.
 */
public class Item {

  /**
   * Product identifier.
   */
  private Integer productId;

  /**
   * Unit price.
   */
  private Double unitPrice;

  /**
   * Quantity.
   */
  private Integer quantity;

  /**
   * Total price.
   */
  private Double totalPrice;

  /**
   * All args constructor.
   *
   * @param productId product identifier.
   * @param unitPrice item unit price.
   * @param quantity  item quantity.
   */
  public Item(Integer productId, Double unitPrice, Integer quantity) {
    this.productId = productId;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.totalPrice = unitPrice * quantity;
  }

  public Integer getProductId() {
    return productId;
  }

  public void setProductId(Integer productId) {
    this.productId = productId;
  }

  public Double getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(Double unitPrice) {
    this.unitPrice = unitPrice;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public void setQuantity(Integer quantity) {
    this.quantity = quantity;
  }

  public Double getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(Double totalPrice) {
    this.totalPrice = totalPrice;
  }

  /**
   * Updates item info.
   *
   * @param unitPrice product unit price.
   * @param quantity  product quantity.
   */
  public void update(Double unitPrice, Integer quantity) {
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.totalPrice = unitPrice * quantity;
  }
}

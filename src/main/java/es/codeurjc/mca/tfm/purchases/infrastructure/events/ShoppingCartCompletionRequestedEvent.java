package es.codeurjc.mca.tfm.purchases.infrastructure.events;

import lombok.Data;

/**
 * Completed shopping cart event.
 */
@Data
public class ShoppingCartCompletionRequestedEvent {

  /**
   * Shopping cart identifier.
   */
  private Long id;

  /**
   * Total price.
   */
  private Double totalPrice;

}

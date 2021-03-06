package es.codeurjc.mca.tfm.purchases.infrastructure.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.codeurjc.mca.tfm.purchases.domain.ports.in.OrderUseCase;
import es.codeurjc.mca.tfm.purchases.infrastructure.entities.ShoppingCartEntity;
import es.codeurjc.mca.tfm.purchases.infrastructure.events.ShoppingCartCompletionRequestedEvent;
import es.codeurjc.mca.tfm.purchases.infrastructure.events.ShoppingCartCreationRequestedEvent;
import es.codeurjc.mca.tfm.purchases.infrastructure.events.ShoppingCartDeletionRequestedEvent;
import es.codeurjc.mca.tfm.purchases.infrastructure.events.ShoppingCartItemsUpdateRequestedEvent;
import es.codeurjc.mca.tfm.purchases.infrastructure.mappers.InfraMapper;
import es.codeurjc.mca.tfm.purchases.infrastructure.repositories.JpaShoppingCartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Shopping cart events listener.
 */
@Service
@Slf4j
public class ShoppingCartEventsListener {

  /**
   * Mapper.
   */
  private InfraMapper mapper;

  /**
   * Shopping cart repository.
   */
  private JpaShoppingCartRepository jpaShoppingCartRepository;

  /**
   * Order use case.
   */
  private OrderUseCase orderUseCase;

  /**
   * Object mapper.
   */
  private ObjectMapper objectMapper;

  /**
   * Constructor.
   *
   * @param mapper                    mapper.
   * @param jpaShoppingCartRepository shopping cart repository.
   * @param orderUseCase              order use case.
   */
  public ShoppingCartEventsListener(InfraMapper mapper,
      JpaShoppingCartRepository jpaShoppingCartRepository,
      OrderUseCase orderUseCase) {
    this.mapper = mapper;
    this.jpaShoppingCartRepository = jpaShoppingCartRepository;
    this.orderUseCase = orderUseCase;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Listener to process created shopping cart events and save them in database.
   *
   * @param shoppingCartCreationRequestedEvent with shopping cart to save info.
   */
  @KafkaListener(topics = "${kafka.topics.createShoppingCart}", groupId = "${kafka.groupId}")
  public void onCreatedShoppingCart(String shoppingCartCreationRequestedEvent) throws Exception {
    try {
      log.info("Received shoppingCartCreationRequestedEvent {}",
          shoppingCartCreationRequestedEvent);
      ShoppingCartEntity shoppingCartEntity = this.mapper.map(
          this.objectMapper.readValue(shoppingCartCreationRequestedEvent,
              ShoppingCartCreationRequestedEvent.class));
      this.jpaShoppingCartRepository.findByUserIdAndCompletedIsFalse(shoppingCartEntity.getUserId())
          .ifPresentOrElse(
              incompleteShoppingCartEntity -> log.error(
                  "Can't create shopping cart. Already exists an incomplete shopping cart {}",
                  incompleteShoppingCartEntity),
              () -> {
                this.jpaShoppingCartRepository.save(shoppingCartEntity);
                log.info("Shopping cart {} saved", shoppingCartEntity);
              }
          );
    } catch (Exception e) {
      log.error("Error processing event {}: {}", shoppingCartCreationRequestedEvent,
          e.getMessage());
      throw e;
    }
  }

  /**
   * Listener to process delete shopping cart events.
   *
   * @param shoppingCartDeletionRequestedEvent with shopping cart to delete id.
   */
  @KafkaListener(topics = "${kafka.topics.deleteShoppingCart}", groupId = "${kafka.groupId}")
  public void onDeletedShoppingCart(String shoppingCartDeletionRequestedEvent) throws Exception {
    try {
      log.info("Received shoppingCartDeletionRequestedEvent {}",
          shoppingCartDeletionRequestedEvent);
      Long id =
          this.objectMapper.readValue(shoppingCartDeletionRequestedEvent,
              ShoppingCartDeletionRequestedEvent.class).getId();
      this.jpaShoppingCartRepository.deleteById(id);
      log.info("Shopping cart with id {} deleted", id);
    } catch (Exception e) {
      log.error("Error processing event {}: {}", shoppingCartDeletionRequestedEvent,
          e.getMessage());
      throw e;
    }
  }

  /**
   * Listener to process completed shopping cart events and save them in database.
   *
   * @param shoppingCartCompletionRequestedEvent with completed shopping cart to save info.
   */
  @KafkaListener(topics = "${kafka.topics.completeShoppingCart}", groupId = "${kafka.groupId}")
  public void onCompletedShoppingCart(String shoppingCartCompletionRequestedEvent)
      throws Exception {
    try {
      log.info("Received shoppingCartCompletionRequestedEvent {}",
          shoppingCartCompletionRequestedEvent);
      ShoppingCartCompletionRequestedEvent completionRequestedEvent = this.objectMapper.readValue(
          shoppingCartCompletionRequestedEvent, ShoppingCartCompletionRequestedEvent.class);
      this.jpaShoppingCartRepository.findById(completionRequestedEvent.getId()).ifPresentOrElse(
          shoppingCartEntity -> {
            if (shoppingCartEntity.isCompleted()) {
              log.info("Shopping cart with id {} already completed",
                  completionRequestedEvent.getId());
            } else if (shoppingCartEntity.getTotalPrice().compareTo(
                completionRequestedEvent.getTotalPrice()) != 0) {
              log.error(
                  "Shopping cart total price is different of passed price. "
                      + "Please check and try again");
            } else {
              shoppingCartEntity.setCompleted(true);
              this.jpaShoppingCartRepository.save(shoppingCartEntity);
              log.info("Shopping cart {} saved", shoppingCartEntity);

              this.orderUseCase.create(this.mapper.map(shoppingCartEntity));
              log.info("Requested order creation for shopping cart {}", shoppingCartEntity);
            }
          },
          () -> log.error("Not shopping cart found with id {}", completionRequestedEvent.getId())
      );
    } catch (Exception e) {
      log.error("Error processing event {}: {}", shoppingCartCompletionRequestedEvent,
          e.getMessage());
      throw e;
    }
  }

  /**
   * Listener to process shopping cart update items events and save them in database.
   *
   * @param shoppingCartItemsUpdateRequestedEvent with info to save items in shopping cart.
   */
  @KafkaListener(topics = "${kafka.topics.updateItems}", groupId = "${kafka.groupId}")
  public void onUpdateShoppingCartItems(String shoppingCartItemsUpdateRequestedEvent)
      throws Exception {
    try {
      log.info("Received shoppingCartItemsUpdateRequestedEvent {}",
          shoppingCartItemsUpdateRequestedEvent);
      ShoppingCartItemsUpdateRequestedEvent itemsUpdateRequestedEvent = this.objectMapper.readValue(
          shoppingCartItemsUpdateRequestedEvent, ShoppingCartItemsUpdateRequestedEvent.class);
      String items = this.mapper.map(itemsUpdateRequestedEvent.getItems());
      this.jpaShoppingCartRepository.findById(itemsUpdateRequestedEvent.getId()).ifPresentOrElse(
          shoppingCartEntity -> {
            if (shoppingCartEntity.isCompleted()) {
              log.error("Can't update items on a completed shopping cart.");
            } else {
              shoppingCartEntity.setItems(items);
              shoppingCartEntity.setTotalPrice(itemsUpdateRequestedEvent.getTotalPrice());
              this.jpaShoppingCartRepository.save(shoppingCartEntity);
              log.info("Shopping cart {} saved", shoppingCartEntity);
            }
          },
          () -> log.error("Not shopping cart found with id {}", itemsUpdateRequestedEvent.getId())
      );
    } catch (Exception e) {
      log.error("Error processing event {}: {}", shoppingCartItemsUpdateRequestedEvent,
          e.getMessage());
      throw e;
    }
  }

}

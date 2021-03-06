package es.codeurjc.mca.tfm.purchases.infrastructure.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.codeurjc.mca.tfm.purchases.domain.dtos.ShoppingCartDto;
import es.codeurjc.mca.tfm.purchases.domain.ports.out.ShoppingCartRepository;
import es.codeurjc.mca.tfm.purchases.infrastructure.events.ShoppingCartCompletionRequestedEvent;
import es.codeurjc.mca.tfm.purchases.infrastructure.events.ShoppingCartCreationRequestedEvent;
import es.codeurjc.mca.tfm.purchases.infrastructure.events.ShoppingCartDeletionRequestedEvent;
import es.codeurjc.mca.tfm.purchases.infrastructure.events.ShoppingCartItemsUpdateRequestedEvent;
import es.codeurjc.mca.tfm.purchases.infrastructure.mappers.InfraMapper;
import es.codeurjc.mca.tfm.purchases.infrastructure.repositories.JpaShoppingCartRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Shopping cart repository adapter Kafka implementation.
 */
@Service
@Slf4j
public class KafkaShoppingCartRepositoryAdapter implements ShoppingCartRepository {

  /**
   * Mapper.
   */
  private InfraMapper infraMapper;

  /**
   * Kafka template.
   */
  private KafkaTemplate<String, String> kafkaTemplate;

  /**
   * Shopping cart repository.
   */
  private JpaShoppingCartRepository jpaShoppingCartRepository;

  /**
   * Kafka create shopping cart topic.
   */
  @Value("${kafka.topics.createShoppingCart}")
  private String createShoppingCartTopic;

  /**
   * Kafka delete shopping cart topic.
   */
  @Value("${kafka.topics.deleteShoppingCart}")
  private String deleteShoppingCartTopic;

  /**
   * Kafka complete shopping cart topic.
   */
  @Value("${kafka.topics.completeShoppingCart}")
  private String completeShoppingCartTopic;

  /**
   * Kafka set item to shopping cart topic.
   */
  @Value("${kafka.topics.updateItems}")
  private String updateItemsTopic;

  /**
   * Object mapper.
   */
  private ObjectMapper objectMapper;

  /**
   * Constructor.
   *
   * @param infraMapper   mapper.
   * @param kafkaTemplate kafka template.
   */
  public KafkaShoppingCartRepositoryAdapter(InfraMapper infraMapper,
      KafkaTemplate<String, String> kafkaTemplate,
      JpaShoppingCartRepository jpaShoppingCartRepository) {
    this.infraMapper = infraMapper;
    this.kafkaTemplate = kafkaTemplate;
    this.jpaShoppingCartRepository = jpaShoppingCartRepository;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Send a created shopping cart event to save it in database.
   *
   * @param shoppingCartDto DTO with shopping cart info.
   */
  @Override
  public void create(ShoppingCartDto shoppingCartDto) {
    try {
      ShoppingCartCreationRequestedEvent shoppingCartCreationRequestedEvent =
          this.infraMapper.mapToShoppingCartCreationRequestedEvent(shoppingCartDto);
      this.kafkaTemplate.send(this.createShoppingCartTopic,
          this.objectMapper.writeValueAsString(shoppingCartCreationRequestedEvent));
      log.info("Sent shopping cart creation requested event {}",
          shoppingCartCreationRequestedEvent);
    } catch (JsonProcessingException e) {
      log.error("Error sending shopping cart creation requested event");
      e.printStackTrace();
    }
  }

  /**
   * Get the current only incomplete shopping cart for passed user.
   *
   * @param userId user identifier.
   * @return optional of incomplete shopping cart for user if exists, else empty.
   */
  @Override
  public Optional<ShoppingCartDto> getIncompleteByUser(Integer userId) {
    return this.jpaShoppingCartRepository.findByUserIdAndCompletedIsFalse(userId)
        .map(this.infraMapper::map);
  }

  /**
   * Get shopping cart by identifier and user.
   *
   * @param id     shopping cart identifier.
   * @param userId user identifier.
   * @return optional of shopping cart with id and user.
   */
  @Override
  public Optional<ShoppingCartDto> getByIdAndUser(Long id, Integer userId) {
    return this.jpaShoppingCartRepository.findByIdAndUserId(id, userId)
        .map(this.infraMapper::map);
  }

  /**
   * Delete a shopping cart by id.
   *
   * @param id shopping cart identifier.
   */
  @Override
  public void delete(Long id) {
    try {
      ShoppingCartDeletionRequestedEvent shoppingCartDeletionRequestedEvent =
          new ShoppingCartDeletionRequestedEvent(id);
      this.kafkaTemplate.send(this.deleteShoppingCartTopic,
          this.objectMapper.writeValueAsString(shoppingCartDeletionRequestedEvent));
      log.info("Sent shopping cart deletion requested event {}",
          shoppingCartDeletionRequestedEvent);
    } catch (JsonProcessingException e) {
      log.error("Error sending shopping cart deletion requested event");
      e.printStackTrace();
    }
  }

  /**
   * Send a completed shopping cart event to save it in database.
   *
   * @param shoppingCartDto DTO with completed shopping cart info.
   */
  @Override
  public void complete(ShoppingCartDto shoppingCartDto) {
    try {
      final ShoppingCartCompletionRequestedEvent shoppingCartCompletionRequestedEvent =
          this.infraMapper.mapToShoppingCartCompletionRequestedEvent(shoppingCartDto);
      this.kafkaTemplate.send(this.completeShoppingCartTopic,
          this.objectMapper.writeValueAsString(shoppingCartCompletionRequestedEvent));
      log.info("Sent shopping cart completion requested event {}",
          shoppingCartCompletionRequestedEvent);
    } catch (JsonProcessingException e) {
      log.error("Error sending shopping cart completion requested event");
      e.printStackTrace();
    }
  }

  /**
   * Updates shopping cart items.
   *
   * @param shoppingCartDto DTO with shopping cart with updated items.
   */
  @Override
  public void updateItems(ShoppingCartDto shoppingCartDto) {
    try {
      final ShoppingCartItemsUpdateRequestedEvent shoppingCartItemsUpdateRequestedEvent =
          this.infraMapper.mapToShoppingCartItemsUpdateRequestedEvent(shoppingCartDto);
      this.kafkaTemplate.send(this.updateItemsTopic,
          this.objectMapper.writeValueAsString(shoppingCartItemsUpdateRequestedEvent));
      log.info("Sent shopping cart items update requested event {}",
          shoppingCartItemsUpdateRequestedEvent);
    } catch (JsonProcessingException e) {
      log.error("Error sending shopping cart items update requested event");
      e.printStackTrace();
    }
  }

}

package com.rdn.rdn_api.infrastructure.persistence.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.order.InventoryGateway;
import com.rdn.rdn_api.domain.InventoryItem;
import com.rdn.rdn_api.domain.InventoryMovement;
import com.rdn.rdn_api.domain.MovementType;
import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryMovementJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.mapper.InventoryItemMapper;
import com.rdn.rdn_api.infrastructure.persistence.mapper.InventoryMovementMapper;
import com.rdn.rdn_api.infrastructure.repository.InventoryItemJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.InventoryMovementJpaRepository;

/**
 * Adaptador de {@link InventoryGateway}.
 *
 * <p>A leitura usa lock pessimista de escrita: a linha de estoque fica travada
 * ate o fim da transacao do pedido, impedindo que duas comandas concorrentes
 * baixem o mesmo saldo.</p>
 */
@Component
public class InventoryGatewayAdapter implements InventoryGateway {

    private final InventoryItemJpaRepository inventoryRepository;

    private final InventoryMovementJpaRepository movementRepository;

    public InventoryGatewayAdapter(InventoryItemJpaRepository inventoryRepository,
            InventoryMovementJpaRepository movementRepository) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    @Transactional
    public ReservationResult reserve(UUID branchId, UUID menuItemId, BigDecimal quantity, String notes) {
        Optional<InventoryItemJpaEntity> found = inventoryRepository.findForUpdate(branchId, menuItemId);
        if (found.isEmpty()) {
            return new ReservationResult(false, BigDecimal.ZERO);
        }

        InventoryItemJpaEntity entity = found.get();
        InventoryItem item = InventoryItemMapper.toDomain(entity);
        BigDecimal available = item.getQuantityOnHand() == null
                ? BigDecimal.ZERO : item.getQuantityOnHand();

        if (!Boolean.TRUE.equals(item.deductStock(quantity, notes))) {
            return new ReservationResult(false, available);
        }

        InventoryItemMapper.copyScalarsToEntity(item, entity);
        inventoryRepository.save(entity);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventoryItem(item);
        movement.setType(MovementType.OUTBOUND);
        movement.setQuantity(quantity);
        movement.setTimestamp(LocalDateTime.now());
        movement.setNotes(notes);

        InventoryMovementJpaEntity movementEntity = InventoryMovementMapper.toEntity(movement);
        movementEntity.setInventoryItem(entity);
        movementRepository.save(movementEntity);

        return new ReservationResult(true, item.getQuantityOnHand());
    }
}

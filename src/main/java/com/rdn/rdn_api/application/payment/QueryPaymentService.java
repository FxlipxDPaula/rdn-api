package com.rdn.rdn_api.application.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdn.rdn_api.application.auth.AuthenticatedUser;
import com.rdn.rdn_api.application.order.QueryOrderService;
import com.rdn.rdn_api.domain.Payment;

/**
 * Caso de uso de consulta do historico de pagamentos de um pedido.
 */
@Service
public class QueryPaymentService {

    private final PaymentRepository paymentRepository;

    private final QueryOrderService queryOrderService;

    public QueryPaymentService(PaymentRepository paymentRepository, QueryOrderService queryOrderService) {
        this.paymentRepository = paymentRepository;
        this.queryOrderService = queryOrderService;
    }

    /**
     * Lista as tentativas de pagamento do pedido.
     *
     * <p>Reaproveita {@link QueryOrderService#findById(UUID, AuthenticatedUser)}
     * apenas para garantir existencia e dono antes de expor o historico.</p>
     *
     * @param pedidoId  identificador do pedido
     * @param principal usuario autenticado
     * @return tentativas, da mais recente para a mais antiga
     */
    @Transactional(readOnly = true)
    public List<Payment> listByOrder(UUID pedidoId, AuthenticatedUser principal) {
        queryOrderService.findById(pedidoId, principal);

        return paymentRepository.findByOrderId(pedidoId);
    }
}

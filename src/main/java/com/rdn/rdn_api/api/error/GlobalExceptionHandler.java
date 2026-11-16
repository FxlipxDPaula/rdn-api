package com.rdn.rdn_api.api.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.rdn.rdn_api.application.auth.InactiveUserException;
import com.rdn.rdn_api.application.auth.InvalidCredentialsException;
import com.rdn.rdn_api.application.loyalty.exception.ConsentRequiredException;
import com.rdn.rdn_api.application.loyalty.exception.InsufficientPointsException;
import com.rdn.rdn_api.application.loyalty.exception.RedemptionAlreadyAppliedException;
import com.rdn.rdn_api.application.loyalty.exception.RedemptionExceedsOrderTotalException;
import com.rdn.rdn_api.application.loyalty.exception.RedemptionNotAllowedException;
import com.rdn.rdn_api.application.order.exception.InsufficientStockException;
import com.rdn.rdn_api.application.order.exception.InvalidStatusTransitionException;
import com.rdn.rdn_api.application.order.exception.OperationNotAllowedException;
import com.rdn.rdn_api.application.order.exception.ResourceNotFoundException;
import com.rdn.rdn_api.application.payment.exception.IdempotencyConflictException;
import com.rdn.rdn_api.application.payment.exception.MissingIdempotencyKeyException;
import com.rdn.rdn_api.application.payment.exception.OrderNotAwaitingPaymentException;
import com.rdn.rdn_api.application.promotion.exception.PromotionNotApplicableException;
import com.rdn.rdn_api.application.user.exception.EmailAlreadyRegisteredException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Traduz excecoes em respostas HTTP no formato {@link ApiError}.
 *
 * <p>Centralizar aqui evita que cada controller invente seu proprio JSON de
 * erro e garante os status coerentes exigidos no roteiro (400, 401, 403, 404,
 * 405, 500).</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return ApiError.of("CREDENCIAIS_INVALIDAS", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InactiveUserException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleInactiveUser(InactiveUserException ex, HttpServletRequest request) {
        return ApiError.of("USUARIO_INATIVO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex,
            HttpServletRequest request) {
        return ApiError.of("EMAIL_JA_CADASTRADO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return ApiError.of("DADOS_INVALIDOS", "Um ou mais campos estao invalidos.", details, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ApiError.of("CORPO_INVALIDO", "O corpo da requisicao esta ausente ou malformado.",
                request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return ApiError.of("RECURSO_NAO_ENCONTRADO", "Recurso nao encontrado.", request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiError handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return ApiError.of("METODO_NAO_SUPORTADO", "Metodo HTTP nao suportado para este recurso.",
                request.getRequestURI());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return ApiError.of("RECURSO_NAO_ENCONTRADO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleOperationNotAllowed(OperationNotAllowedException ex, HttpServletRequest request) {
        return ApiError.of("ACESSO_NEGADO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        List<ApiErrorDetail> details = List.of(new ApiErrorDetail(
                "itens[produtoId=" + ex.getProdutoId() + "].quantidade",
                "Disponivel: " + ex.getDisponivel() + "; solicitado: " + ex.getSolicitado()));

        return ApiError.of("ESTOQUE_INSUFICIENTE",
                "Nao ha quantidade suficiente para um ou mais itens.", details, request.getRequestURI());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleInvalidTransition(InvalidStatusTransitionException ex,
            HttpServletRequest request) {
        return ApiError.of("TRANSICAO_INVALIDA", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(OrderNotAwaitingPaymentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleOrderNotAwaitingPayment(OrderNotAwaitingPaymentException ex,
            HttpServletRequest request) {
        return ApiError.of("PEDIDO_NAO_AGUARDA_PAGAMENTO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleIdempotencyConflict(IdempotencyConflictException ex,
            HttpServletRequest request) {
        return ApiError.of("CHAVE_IDEMPOTENCIA_CONFLITANTE", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingIdempotencyKey(MissingIdempotencyKeyException ex,
            HttpServletRequest request) {
        return ApiError.of("CABECALHO_OBRIGATORIO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(PromotionNotApplicableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handlePromotionNotApplicable(PromotionNotApplicableException ex,
            HttpServletRequest request) {
        return ApiError.of("PROMOCAO_NAO_APLICAVEL", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ConsentRequiredException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConsentRequired(ConsentRequiredException ex, HttpServletRequest request) {
        return ApiError.of("CONSENTIMENTO_NECESSARIO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InsufficientPointsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleInsufficientPoints(InsufficientPointsException ex,
            HttpServletRequest request) {
        List<ApiErrorDetail> details = List.of(new ApiErrorDetail("pontos",
                "Disponivel: " + ex.getAvailable() + "; solicitado: " + ex.getRequested()));

        return ApiError.of("PONTOS_INSUFICIENTES", ex.getMessage(), details, request.getRequestURI());
    }

    @ExceptionHandler(RedemptionAlreadyAppliedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleRedemptionAlreadyApplied(RedemptionAlreadyAppliedException ex,
            HttpServletRequest request) {
        return ApiError.of("RESGATE_JA_APLICADO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RedemptionExceedsOrderTotalException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleRedemptionExceedsOrderTotal(RedemptionExceedsOrderTotalException ex,
            HttpServletRequest request) {
        List<ApiErrorDetail> details = List.of(new ApiErrorDetail("desconto",
                "Desconto: " + ex.getDiscount() + "; saldo devedor: " + ex.getRemaining()));

        return ApiError.of("RESGATE_EXCEDE_TOTAL", ex.getMessage(), details, request.getRequestURI());
    }

    @ExceptionHandler(RedemptionNotAllowedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleRedemptionNotAllowed(RedemptionNotAllowedException ex,
            HttpServletRequest request) {
        return ApiError.of("RESGATE_NAO_PERMITIDO", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        List<ApiErrorDetail> details = List.of(new ApiErrorDetail(ex.getName(),
                "Valor invalido para o parametro."));

        return ApiError.of("PARAMETRO_INVALIDO", "Parametro de requisicao invalido.", details,
                request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ApiError.of("DADOS_INVALIDOS", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro nao tratado em {}", request.getRequestURI(), ex);
        return ApiError.of("ERRO_INTERNO", "Erro interno inesperado.", request.getRequestURI());
    }
}

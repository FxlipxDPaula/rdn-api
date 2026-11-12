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
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.rdn.rdn_api.application.auth.InactiveUserException;
import com.rdn.rdn_api.application.auth.InvalidCredentialsException;

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

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro nao tratado em {}", request.getRequestURI(), ex);
        return ApiError.of("ERRO_INTERNO", "Erro interno inesperado.", request.getRequestURI());
    }
}

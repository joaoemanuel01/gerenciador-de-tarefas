package br.com.tarefas.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Centraliza o tratamento de erros: toda falha da API devolve o mesmo JSON
 * ({@link ErroResponse}) com o código HTTP adequado. Erros inesperados (500)
 * são registrados no log, mas nunca expõem detalhes internos ao cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------------- erros de domínio ----------------

    @ExceptionHandler(TarefaNaoEncontradaException.class)
    public ResponseEntity<ErroResponse> naoEncontrada(TarefaNaoEncontradaException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(ParametroInvalidoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of(), req);
    }

    // ---------------- validação de entrada (400) ----------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhes, req);
    }

    /**
     * Violações em @PathVariable / @RequestParam (Spring 6.1+). Quando o método também
     * tem um @Valid @RequestBody, os erros do corpo chegam aqui como ParameterErrors.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResponse> parametrosInvalidos(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<String> detalhes = ex.getParameterValidationResults().stream()
                .flatMap(r -> {
                    if (r instanceof ParameterErrors pe && !pe.getFieldErrors().isEmpty()) {
                        return pe.getFieldErrors().stream()
                                .map(f -> f.getField() + ": " + f.getDefaultMessage());
                    }
                    return r.getResolvableErrors().stream()
                            .map(e -> nomeParametro(r.getMethodParameter().getParameterName()) + ": " + mensagem(e));
                })
                .sorted()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", detalhes, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> violacoes(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> detalhes = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .toList();
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", detalhes, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> jsonInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente, mal formatado ou com campos inválidos "
                        + "(verifique o JSON, campos desconhecidos, o status e o formato yyyy-MM-dd da data)",
                List.of(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + ex.getName() + "'", List.of(), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> parametroAusente(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST,
                "O parâmetro '" + ex.getParameterName() + "' é obrigatório", List.of(), req);
    }

    // ---------------- erros de protocolo HTTP ----------------

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED,
                "O método " + ex.getMethod() + " não é permitido para este endpoint", List.of(), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> midiaNaoSuportada(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de conteúdo não suportado. Envie o corpo como application/json", List.of(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Endpoint não encontrado", List.of(), req);
    }

    // ---------------- qualquer outro erro (500) ----------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno no servidor. Tente novamente mais tarde", List.of(), req);
    }

    // ---------------- auxiliares ----------------

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem,
                                                  List<String> detalhes, HttpServletRequest req) {
        ErroResponse corpo = new ErroResponse(
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                req.getRequestURI(),
                detalhes,
                LocalDateTime.now());
        return ResponseEntity.status(status).body(corpo);
    }

    private String nomeParametro(String nome) {
        return nome != null ? nome : "parametro";
    }

    private String mensagem(MessageSourceResolvable erro) {
        return erro.getDefaultMessage() != null ? erro.getDefaultMessage() : "valor inválido";
    }
}

package br.com.tarefas.exception;

/** Dados válidos no formato, mas que violam uma regra de negócio (HTTP 422). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}

package br.com.tarefas.exception;

/** Parâmetro de consulta com valor não aceito (HTTP 400). */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensagem) {
        super(mensagem);
    }
}

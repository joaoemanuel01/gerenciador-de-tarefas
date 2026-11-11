package br.com.tarefas.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Formato único de erro devolvido por toda a API. */
public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<String> detalhes,
        LocalDateTime timestamp
) {
}

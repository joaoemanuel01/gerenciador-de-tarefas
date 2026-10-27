package br.com.tarefas.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
        int status,
        String erro,
        String mensagem,
        List<String> detalhes,
        LocalDateTime timestamp
) {
    public static ErroResponse de(int status, String erro, String mensagem, List<String> detalhes) {
        return new ErroResponse(status, erro, mensagem, detalhes, LocalDateTime.now());
    }
}

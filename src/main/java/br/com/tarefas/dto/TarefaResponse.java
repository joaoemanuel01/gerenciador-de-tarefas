package br.com.tarefas.dto;

import br.com.tarefas.model.StatusTarefa;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Representação da tarefa devolvida ao cliente (a entidade JPA nunca é exposta). */
public record TarefaResponse(
        Long id,
        String titulo,
        String descricao,
        StatusTarefa status,
        LocalDate dataLimite,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm
) {
}

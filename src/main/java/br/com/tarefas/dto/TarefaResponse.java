package br.com.tarefas.dto;

import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.model.Tarefa;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TarefaResponse(
        Long id,
        String titulo,
        String descricao,
        StatusTarefa status,
        LocalDate dataLimite,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm
) {
    public static TarefaResponse de(Tarefa t) {
        return new TarefaResponse(
                t.getId(),
                t.getTitulo(),
                t.getDescricao(),
                t.getStatus(),
                t.getDataLimite(),
                t.getCriadaEm(),
                t.getAtualizadaEm()
        );
    }
}

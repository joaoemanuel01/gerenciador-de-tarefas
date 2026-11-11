package br.com.tarefas.dto;

import br.com.tarefas.model.StatusTarefa;
import jakarta.validation.constraints.NotNull;

/** Corpo do PATCH /api/tarefas/{id}/status. */
public record StatusRequest(
        @NotNull(message = "O status é obrigatório")
        StatusTarefa status
) {
}

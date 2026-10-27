package br.com.tarefas.dto;

import br.com.tarefas.model.StatusTarefa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Corpo aceito em POST e PUT.
 * Se "status" for omitido, a tarefa assume PENDENTE.
 */
public record TarefaRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 120, message = "O título deve ter no máximo 120 caracteres")
        String titulo,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        StatusTarefa status,

        LocalDate dataLimite
) {
}

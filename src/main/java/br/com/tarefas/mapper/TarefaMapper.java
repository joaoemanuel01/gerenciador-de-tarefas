package br.com.tarefas.mapper;

import br.com.tarefas.dto.TarefaRequest;
import br.com.tarefas.dto.TarefaResponse;
import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.model.Tarefa;
import org.springframework.stereotype.Component;

/**
 * Converte entre DTOs e entidade, isolando o controller e o service
 * dos detalhes de persistência (padrão Mapper).
 */
@Component
public class TarefaMapper {

    public Tarefa paraEntidade(TarefaRequest request) {
        Tarefa tarefa = new Tarefa();
        copiar(request, tarefa);
        return tarefa;
    }

    public void copiar(TarefaRequest origem, Tarefa destino) {
        destino.setTitulo(origem.titulo().trim());
        destino.setDescricao(textoOuNulo(origem.descricao()));
        destino.setStatus(origem.status() != null ? origem.status() : StatusTarefa.PENDENTE);
        destino.setDataLimite(origem.dataLimite());
    }

    public TarefaResponse paraResponse(Tarefa t) {
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

    private String textoOuNulo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }
}

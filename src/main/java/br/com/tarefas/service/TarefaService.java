package br.com.tarefas.service;

import br.com.tarefas.dto.PaginaResponse;
import br.com.tarefas.dto.TarefaRequest;
import br.com.tarefas.dto.TarefaResponse;
import br.com.tarefas.model.StatusTarefa;
import org.springframework.data.domain.Pageable;

/**
 * Contrato da camada de serviço. O controller depende desta interface
 * e não da implementação (inversão de dependência).
 */
public interface TarefaService {

    PaginaResponse<TarefaResponse> listar(StatusTarefa status, String busca, Pageable pageable);

    TarefaResponse buscarPorId(Long id);

    TarefaResponse criar(TarefaRequest request);

    TarefaResponse atualizar(Long id, TarefaRequest request);

    TarefaResponse atualizarStatus(Long id, StatusTarefa novoStatus);

    void excluir(Long id);
}

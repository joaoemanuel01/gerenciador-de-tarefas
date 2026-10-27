package br.com.tarefas.service;

import br.com.tarefas.dto.TarefaRequest;
import br.com.tarefas.dto.TarefaResponse;
import br.com.tarefas.exception.TarefaNaoEncontradaException;
import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.model.Tarefa;
import br.com.tarefas.repository.TarefaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TarefaService {

    private final TarefaRepository repository;

    public TarefaService(TarefaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TarefaResponse> listar(StatusTarefa status) {
        List<Tarefa> tarefas = (status == null)
                ? repository.findAll(Sort.by("id"))
                : repository.findByStatusOrderByIdAsc(status);
        return tarefas.stream().map(TarefaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TarefaResponse buscarPorId(Long id) {
        return TarefaResponse.de(obter(id));
    }

    public TarefaResponse criar(TarefaRequest request) {
        Tarefa tarefa = new Tarefa();
        copiar(request, tarefa);
        return TarefaResponse.de(repository.save(tarefa));
    }

    public TarefaResponse atualizar(Long id, TarefaRequest request) {
        Tarefa tarefa = obter(id);
        copiar(request, tarefa);
        return TarefaResponse.de(repository.save(tarefa));
    }

    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    private Tarefa obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }

    private void copiar(TarefaRequest origem, Tarefa destino) {
        destino.setTitulo(origem.titulo().trim());
        destino.setDescricao(origem.descricao());
        destino.setStatus(origem.status() != null ? origem.status() : StatusTarefa.PENDENTE);
        destino.setDataLimite(origem.dataLimite());
    }
}

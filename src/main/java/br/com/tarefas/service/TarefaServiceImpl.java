package br.com.tarefas.service;

import br.com.tarefas.dto.PaginaResponse;
import br.com.tarefas.dto.TarefaRequest;
import br.com.tarefas.dto.TarefaResponse;
import br.com.tarefas.exception.ConflitoException;
import br.com.tarefas.exception.ParametroInvalidoException;
import br.com.tarefas.exception.RegraNegocioException;
import br.com.tarefas.exception.TarefaNaoEncontradaException;
import br.com.tarefas.mapper.TarefaMapper;
import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.model.Tarefa;
import br.com.tarefas.repository.TarefaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Regras de negócio:
 * <ul>
 *   <li>a data limite não pode estar no passado (ao criar ou ao alterar a data);</li>
 *   <li>não pode haver duas tarefas em aberto (não concluídas) com o mesmo título;</li>
 *   <li>a ordenação só é aceita para campos conhecidos.</li>
 * </ul>
 */
@Service
@Transactional
public class TarefaServiceImpl implements TarefaService {

    private static final Set<String> CAMPOS_ORDENAVEIS =
            Set.of("id", "titulo", "status", "dataLimite", "criadaEm");

    private final TarefaRepository repository;
    private final TarefaMapper mapper;

    public TarefaServiceImpl(TarefaRepository repository, TarefaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<TarefaResponse> listar(StatusTarefa status, String busca, Pageable pageable) {
        validarOrdenacao(pageable.getSort());
        Page<Tarefa> pagina = repository.findAll(filtro(status, busca), pageable);
        return PaginaResponse.de(pagina.map(mapper::paraResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public TarefaResponse buscarPorId(Long id) {
        return mapper.paraResponse(obter(id));
    }

    @Override
    public TarefaResponse criar(TarefaRequest request) {
        validarDataLimite(request.dataLimite());
        Tarefa tarefa = mapper.paraEntidade(request);
        validarDuplicidade(tarefa.getTitulo(), tarefa.getStatus(), null);
        return mapper.paraResponse(repository.save(tarefa));
    }

    @Override
    public TarefaResponse atualizar(Long id, TarefaRequest request) {
        Tarefa tarefa = obter(id);
        if (!Objects.equals(request.dataLimite(), tarefa.getDataLimite())) {
            validarDataLimite(request.dataLimite());
        }
        mapper.copiar(request, tarefa);
        validarDuplicidade(tarefa.getTitulo(), tarefa.getStatus(), id);
        return mapper.paraResponse(repository.save(tarefa));
    }

    @Override
    public TarefaResponse atualizarStatus(Long id, StatusTarefa novoStatus) {
        Tarefa tarefa = obter(id);
        validarDuplicidade(tarefa.getTitulo(), novoStatus, id);
        tarefa.setStatus(novoStatus);
        return mapper.paraResponse(repository.save(tarefa));
    }

    @Override
    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    // ---------------------------------------------------------------- auxiliares

    private Tarefa obter(Long id) {
        return repository.findById(id).orElseThrow(() -> new TarefaNaoEncontradaException(id));
    }

    private void validarDataLimite(LocalDate dataLimite) {
        if (dataLimite != null && dataLimite.isBefore(LocalDate.now())) {
            throw new RegraNegocioException("A data limite não pode estar no passado");
        }
    }

    private void validarDuplicidade(String titulo, StatusTarefa status, Long idIgnorado) {
        if (status == StatusTarefa.CONCLUIDA) {
            return; // tarefas concluídas não competem por título
        }
        boolean existe = (idIgnorado == null)
                ? repository.existsByTituloIgnoreCaseAndStatusNot(titulo, StatusTarefa.CONCLUIDA)
                : repository.existsByTituloIgnoreCaseAndStatusNotAndIdNot(titulo, StatusTarefa.CONCLUIDA, idIgnorado);
        if (existe) {
            throw new ConflitoException("Já existe uma tarefa em aberto com o título '" + titulo + "'");
        }
    }

    private void validarOrdenacao(Sort sort) {
        sort.forEach(order -> {
            if (!CAMPOS_ORDENAVEIS.contains(order.getProperty())) {
                throw new ParametroInvalidoException("Campo de ordenação inválido: '" + order.getProperty()
                        + "'. Use um destes: " + String.join(", ", new java.util.TreeSet<>(CAMPOS_ORDENAVEIS)));
            }
        });
    }

    private Specification<Tarefa> filtro(StatusTarefa status, String busca) {
        Specification<Tarefa> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (busca != null && !busca.isBlank()) {
            String termo = busca.trim().toLowerCase()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String padrao = "%" + termo + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("titulo")), padrao, '\\'));
        }
        return spec;
    }
}

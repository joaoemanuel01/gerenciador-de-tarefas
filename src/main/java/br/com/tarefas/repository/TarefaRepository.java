package br.com.tarefas.repository;

import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TarefaRepository extends JpaRepository<Tarefa, Long>, JpaSpecificationExecutor<Tarefa> {

    /** Existe alguma tarefa com esse título (ignorando maiúsculas) cujo status seja diferente do informado? */
    boolean existsByTituloIgnoreCaseAndStatusNot(String titulo, StatusTarefa status);

    /** Mesma verificação, ignorando a própria tarefa (usada em atualizações). */
    boolean existsByTituloIgnoreCaseAndStatusNotAndIdNot(String titulo, StatusTarefa status, Long id);
}

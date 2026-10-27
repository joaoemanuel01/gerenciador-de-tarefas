package br.com.tarefas.repository;

import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.model.Tarefa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    List<Tarefa> findByStatusOrderByIdAsc(StatusTarefa status);
}

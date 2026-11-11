package br.com.tarefas.mapper;

import br.com.tarefas.dto.TarefaRequest;
import br.com.tarefas.dto.TarefaResponse;
import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.model.Tarefa;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TarefaMapperTest {

    private final TarefaMapper mapper = new TarefaMapper();

    @Test
    void converteRequestParaEntidadeNormalizandoTextos() {
        var req = new TarefaRequest("  Título  ", "  desc  ", StatusTarefa.EM_ANDAMENTO, LocalDate.of(2030, 1, 1));

        Tarefa t = mapper.paraEntidade(req);

        assertEquals("Título", t.getTitulo());
        assertEquals("desc", t.getDescricao());
        assertEquals(StatusTarefa.EM_ANDAMENTO, t.getStatus());
        assertEquals(LocalDate.of(2030, 1, 1), t.getDataLimite());
    }

    @Test
    void descricaoEmBrancoViraNula() {
        Tarefa t = mapper.paraEntidade(new TarefaRequest("X", "   ", null, null));

        assertNull(t.getDescricao());
        assertEquals(StatusTarefa.PENDENTE, t.getStatus());
    }

    @Test
    void converteEntidadeParaResponse() {
        Tarefa t = mapper.paraEntidade(new TarefaRequest("X", "d", StatusTarefa.CONCLUIDA, null));

        TarefaResponse r = mapper.paraResponse(t);

        assertEquals("X", r.titulo());
        assertEquals("d", r.descricao());
        assertEquals(StatusTarefa.CONCLUIDA, r.status());
    }
}

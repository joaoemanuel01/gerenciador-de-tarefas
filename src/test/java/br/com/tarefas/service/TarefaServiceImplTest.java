package br.com.tarefas.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testa as regras de negócio isoladamente, sem subir o Spring (repositório simulado). */
@ExtendWith(MockitoExtension.class)
class TarefaServiceImplTest {

    @Mock TarefaRepository repository;

    TarefaServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new TarefaServiceImpl(repository, new TarefaMapper());
    }

    @Test
    void criarComSucessoAssumeStatusPendente() {
        when(repository.save(any(Tarefa.class))).thenAnswer(i -> i.getArgument(0));

        TarefaResponse r = service.criar(new TarefaRequest("  Estudar  ", "  ", null, null));

        assertEquals("Estudar", r.titulo());
        assertNull(r.descricao());
        assertEquals(StatusTarefa.PENDENTE, r.status());
    }

    @Test
    void criarComDataNoPassadoLancaRegraNegocio() {
        var req = new TarefaRequest("X", null, null, LocalDate.now().minusDays(1));

        assertThrows(RegraNegocioException.class, () -> service.criar(req));
        verify(repository, never()).save(any());
    }

    @Test
    void criarComDataDeHojeEhPermitido() {
        when(repository.save(any(Tarefa.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> service.criar(new TarefaRequest("X", null, null, LocalDate.now())));
    }

    @Test
    void criarComTituloDuplicadoLancaConflito() {
        when(repository.existsByTituloIgnoreCaseAndStatusNot("Estudar", StatusTarefa.CONCLUIDA)).thenReturn(true);

        assertThrows(ConflitoException.class,
                () -> service.criar(new TarefaRequest("Estudar", null, null, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void criarConcluidaNaoVerificaDuplicidade() {
        when(repository.save(any(Tarefa.class))).thenAnswer(i -> i.getArgument(0));

        service.criar(new TarefaRequest("Estudar", null, StatusTarefa.CONCLUIDA, null));

        verify(repository, never()).existsByTituloIgnoreCaseAndStatusNot(any(), any());
    }

    @Test
    void atualizarInexistenteLancaNaoEncontrada() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(TarefaNaoEncontradaException.class,
                () -> service.atualizar(7L, new TarefaRequest("X", null, null, null)));
    }

    @Test
    void atualizarMantendoDataPassadaJaExistenteNaoReclama() {
        Tarefa existente = new Tarefa();
        existente.setTitulo("Antiga");
        existente.setDataLimite(LocalDate.now().minusDays(30));
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByTituloIgnoreCaseAndStatusNotAndIdNot("Antiga", StatusTarefa.CONCLUIDA, 1L))
                .thenReturn(false);
        when(repository.save(any(Tarefa.class))).thenAnswer(i -> i.getArgument(0));

        var req = new TarefaRequest("Antiga", "agora com descrição", null, existente.getDataLimite());

        assertDoesNotThrow(() -> service.atualizar(1L, req));
    }

    @Test
    void atualizarStatusAplicaNovoStatus() {
        Tarefa existente = new Tarefa();
        existente.setTitulo("Tarefa");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Tarefa.class))).thenAnswer(i -> i.getArgument(0));

        TarefaResponse r = service.atualizarStatus(1L, StatusTarefa.CONCLUIDA);

        assertEquals(StatusTarefa.CONCLUIDA, r.status());
    }

    @Test
    void excluirInexistenteLancaNaoEncontrada() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(TarefaNaoEncontradaException.class, () -> service.excluir(5L));
        verify(repository, never()).delete(any(Tarefa.class));
    }

    @Test
    void listarComCampoDeOrdenacaoDesconhecidoLancaParametroInvalido() {
        var pageable = PageRequest.of(0, 10, Sort.by("senha"));

        assertThrows(ParametroInvalidoException.class, () -> service.listar(null, null, pageable));
    }
}

package br.com.tarefas.controller;

import br.com.tarefas.dto.TarefaRequest;
import br.com.tarefas.dto.TarefaResponse;
import br.com.tarefas.exception.ErroResponse;
import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.service.TarefaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tarefas")
@Tag(name = "Tarefas", description = "CRUD de tarefas pessoais")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    // ---------- GET (lista) ----------
    @GetMapping
    @Operation(summary = "Lista todas as tarefas",
            description = "Retorna todas as tarefas. Use ?status=PENDENTE|EM_ANDAMENTO|CONCLUIDA para filtrar.")
    @ApiResponse(responseCode = "200", description = "Lista retornada (pode ser vazia)")
    @ApiResponse(responseCode = "400", description = "Valor de status inválido",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public List<TarefaResponse> listar(@RequestParam(required = false) StatusTarefa status) {
        return service.listar(status);
    }

    // ---------- GET (por id) ----------
    @GetMapping("/{id}")
    @Operation(summary = "Busca uma tarefa pelo id")
    @ApiResponse(responseCode = "200", description = "Tarefa encontrada")
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public TarefaResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    // ---------- POST ----------
    @PostMapping
    @Operation(summary = "Cria uma nova tarefa")
    @ApiResponse(responseCode = "201", description = "Tarefa criada (header Location aponta para o recurso)")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ResponseEntity<TarefaResponse> criar(@Valid @RequestBody TarefaRequest request) {
        TarefaResponse criada = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    // ---------- PUT ----------
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma tarefa existente",
            description = "Substitui os dados da tarefa pelos enviados no corpo.")
    @ApiResponse(responseCode = "200", description = "Tarefa atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public TarefaResponse atualizar(@PathVariable Long id, @Valid @RequestBody TarefaRequest request) {
        return service.atualizar(id, request);
    }

    // ---------- DELETE ----------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma tarefa")
    @ApiResponse(responseCode = "204", description = "Tarefa excluída (sem corpo)")
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}

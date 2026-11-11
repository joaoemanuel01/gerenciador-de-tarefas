package br.com.tarefas.controller;

import br.com.tarefas.dto.PaginaResponse;
import br.com.tarefas.dto.StatusRequest;
import br.com.tarefas.dto.TarefaRequest;
import br.com.tarefas.dto.TarefaResponse;
import br.com.tarefas.exception.ErroResponse;
import br.com.tarefas.exception.ParametroInvalidoException;
import br.com.tarefas.model.StatusTarefa;
import br.com.tarefas.service.TarefaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Camada de apresentação (o "C" do MVC): só traduz HTTP em chamadas ao serviço.
 * Nenhuma regra de negócio fica aqui.
 */
@RestController
@RequestMapping(path = "/api/tarefas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tarefas", description = "CRUD de tarefas pessoais")
public class TarefaController {

    private final TarefaService service;

    public TarefaController(TarefaService service) {
        this.service = service;
    }

    // ---------- GET (lista paginada) ----------
    @GetMapping
    @Operation(summary = "Lista tarefas (paginado, com filtro e ordenação)",
            description = "Filtros opcionais: status e busca (trecho do título). "
                    + "Ordenação: ordenarPor = id | titulo | status | dataLimite | criadaEm; direcao = asc | desc.")
    @ApiResponse(responseCode = "200", description = "Página retornada (conteúdo pode ser vazio)")
    @ApiResponse(responseCode = "400", description = "Parâmetro inválido",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public PaginaResponse<TarefaResponse> listar(
            @RequestParam(required = false) StatusTarefa status,
            @RequestParam(required = false) @Size(max = 100, message = "A busca deve ter no máximo 100 caracteres") String busca,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "A página deve ser maior ou igual a 0") int pagina,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "O tamanho deve ser no mínimo 1")
            @Max(value = 50, message = "O tamanho deve ser no máximo 50") int tamanho,
            @RequestParam(defaultValue = "id") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direcao) {

        Sort.Direction sentido = Sort.Direction.fromOptionalString(direcao)
                .orElseThrow(() -> new ParametroInvalidoException(
                        "Direção de ordenação inválida: '" + direcao + "'. Use asc ou desc"));
        return service.listar(status, busca, PageRequest.of(pagina, tamanho, Sort.by(sentido, ordenarPor)));
    }

    // ---------- GET (por id) ----------
    @GetMapping("/{id}")
    @Operation(summary = "Busca uma tarefa pelo id")
    @ApiResponse(responseCode = "200", description = "Tarefa encontrada")
    @ApiResponse(responseCode = "400", description = "Id inválido",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public TarefaResponse buscar(@PathVariable @Positive(message = "O id deve ser um número positivo") Long id) {
        return service.buscarPorId(id);
    }

    // ---------- POST ----------
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cria uma nova tarefa")
    @ApiResponse(responseCode = "201", description = "Tarefa criada (header Location aponta para o recurso)")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "409", description = "Já existe tarefa em aberto com o mesmo título",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "415", description = "Content-Type diferente de application/json",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada (ex.: data limite no passado)",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public ResponseEntity<TarefaResponse> criar(@Valid @RequestBody TarefaRequest request) {
        TarefaResponse criada = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    // ---------- PUT ----------
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualiza (substitui) uma tarefa existente",
            description = "Substitui todos os campos pelos enviados no corpo. Para alterar só o status, use PATCH.")
    @ApiResponse(responseCode = "200", description = "Tarefa atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "409", description = "Já existe outra tarefa em aberto com o mesmo título",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada (ex.: data limite no passado)",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public TarefaResponse atualizar(@PathVariable @Positive(message = "O id deve ser um número positivo") Long id,
                                    @Valid @RequestBody TarefaRequest request) {
        return service.atualizar(id, request);
    }

    // ---------- PATCH ----------
    @PatchMapping(path = "/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Altera somente o status da tarefa",
            description = "Atualização parcial: corpo {\"status\": \"CONCLUIDA\"}.")
    @ApiResponse(responseCode = "200", description = "Status atualizado")
    @ApiResponse(responseCode = "400", description = "Status ausente ou inválido",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "409", description = "Reabrir a tarefa conflitaria com outra em aberto de mesmo título",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public TarefaResponse atualizarStatus(@PathVariable @Positive(message = "O id deve ser um número positivo") Long id,
                                          @Valid @RequestBody StatusRequest request) {
        return service.atualizarStatus(id, request.status());
    }

    // ---------- DELETE ----------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma tarefa")
    @ApiResponse(responseCode = "204", description = "Tarefa excluída (sem corpo)")
    @ApiResponse(responseCode = "400", description = "Id inválido",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada",
            content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    public void excluir(@PathVariable @Positive(message = "O id deve ser um número positivo") Long id) {
        service.excluir(id);
    }
}

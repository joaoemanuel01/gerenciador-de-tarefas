package br.com.tarefas.controller;

import br.com.tarefas.repository.TarefaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TarefaControllerTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TarefaRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    private long criarTarefa(String json) throws Exception {
        String corpo = mvc.perform(post("/api/tarefas").contentType(JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(corpo).get("id").asLong();
    }

    private String futuro(int dias) {
        return LocalDate.now().plusDays(dias).toString();
    }

    // =================================================================== GET

    @Test
    void getListaVaziaRetorna200ComPaginaVazia() throws Exception {
        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(0)))
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void getListaTodasAsTarefasOrdenadasPorId() throws Exception {
        criarTarefa("{\"titulo\":\"Estudar Spring\"}");
        criarTarefa("{\"titulo\":\"Pagar conta de luz\"}");

        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(2)))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Estudar Spring"))
                .andExpect(jsonPath("$.conteudo[1].titulo").value("Pagar conta de luz"));
    }

    @Test
    void getPaginaOsResultados() throws Exception {
        criarTarefa("{\"titulo\":\"T1\"}");
        criarTarefa("{\"titulo\":\"T2\"}");
        criarTarefa("{\"titulo\":\"T3\"}");

        mvc.perform(get("/api/tarefas").param("tamanho", "2").param("pagina", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(2)))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.ultima").value(false));

        mvc.perform(get("/api/tarefas").param("tamanho", "2").param("pagina", "1"))
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    void getOrdenaPorTituloDecrescente() throws Exception {
        criarTarefa("{\"titulo\":\"A\"}");
        criarTarefa("{\"titulo\":\"C\"}");
        criarTarefa("{\"titulo\":\"B\"}");

        mvc.perform(get("/api/tarefas").param("ordenarPor", "titulo").param("direcao", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].titulo").value("C"))
                .andExpect(jsonPath("$.conteudo[2].titulo").value("A"));
    }

    @Test
    void getFiltraPorStatus() throws Exception {
        criarTarefa("{\"titulo\":\"A\",\"status\":\"PENDENTE\"}");
        criarTarefa("{\"titulo\":\"B\",\"status\":\"CONCLUIDA\"}");

        mvc.perform(get("/api/tarefas").param("status", "CONCLUIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("B"));
    }

    @Test
    void getBuscaPorTrechoDoTituloSemDiferenciarMaiusculas() throws Exception {
        criarTarefa("{\"titulo\":\"Estudar Spring Boot\"}");
        criarTarefa("{\"titulo\":\"Comprar pão\"}");

        mvc.perform(get("/api/tarefas").param("busca", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].titulo").value("Estudar Spring Boot"));
    }

    @Test
    void getBuscaTrataCoringasComoTextoLiteral() throws Exception {
        criarTarefa("{\"titulo\":\"Meta 100% concluída\"}");
        criarTarefa("{\"titulo\":\"Outra tarefa\"}");

        mvc.perform(get("/api/tarefas").param("busca", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)));
    }

    @Test
    void getComStatusInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/tarefas").param("status", "XPTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.caminho").value("/api/tarefas"));
    }

    @Test
    void getComCampoDeOrdenacaoInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/tarefas").param("ordenarPor", "senha"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("senha")));
    }

    @Test
    void getComDirecaoInvalidaRetorna400() throws Exception {
        mvc.perform(get("/api/tarefas").param("direcao", "lado"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getComTamanhoAcimaDoLimiteRetorna400() throws Exception {
        mvc.perform(get("/api/tarefas").param("tamanho", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("tamanho")));
    }

    @Test
    void getComPaginaNegativaRetorna400() throws Exception {
        mvc.perform(get("/api/tarefas").param("pagina", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPorIdRetornaTarefa() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Ler livro\",\"descricao\":\"Capítulo 3\"}");

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.descricao").value("Capítulo 3"));
    }

    @Test
    void getPorIdInexistenteRetorna404() throws Exception {
        mvc.perform(get("/api/tarefas/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem", containsString("9999")))
                .andExpect(jsonPath("$.caminho").value("/api/tarefas/9999"));
    }

    @Test
    void getComIdZeroOuNegativoRetorna400() throws Exception {
        mvc.perform(get("/api/tarefas/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("id")));
        mvc.perform(get("/api/tarefas/-5"))
                .andExpect(status().isBadRequest());
    }

    // =================================================================== POST

    @Test
    void postCriaTarefaRetorna201ComLocation() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON).content("""
                        {"titulo":"Entregar atividade","descricao":"Unidade HTTP",
                         "status":"EM_ANDAMENTO","dataLimite":"%s"}
                        """.formatted(futuro(10))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/tarefas/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Entregar atividade"))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.dataLimite").value(futuro(10)))
                .andExpect(jsonPath("$.criadaEm").exists());
    }

    @Test
    void postSemStatusAssumePendente() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON).content("{\"titulo\":\"Sem status\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void postRemoveEspacosExtrasDoTitulo() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON).content("{\"titulo\":\"   Com espaços   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Com espaços"));
    }

    @Test
    void postSemTituloRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON).content("{\"titulo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("titulo")));
    }

    @Test
    void postComTituloLongoDemaisRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON)
                        .content("{\"titulo\":\"" + "x".repeat(121) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postComHtmlNoTituloOuDescricaoRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON)
                        .content("{\"titulo\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("titulo")));

        mvc.perform(post("/api/tarefas").contentType(JSON)
                        .content("{\"titulo\":\"ok\",\"descricao\":\"<b>x</b>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("descricao")));
    }

    @Test
    void postComCampoDesconhecidoRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON)
                        .content("{\"titulo\":\"X\",\"id\":99}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postComJsonMalFormatadoRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON).content("{titulo: sem aspas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postSemCorpoRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postComStatusInvalidoRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON)
                        .content("{\"titulo\":\"X\",\"status\":\"INEXISTENTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postComDataEmFormatoInvalidoRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON)
                        .content("{\"titulo\":\"X\",\"dataLimite\":\"31/12/2030\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postComContentTypeInvalidoRetorna415() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(MediaType.TEXT_PLAIN).content("titulo=X"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void postComDataLimiteNoPassadoRetorna422() throws Exception {
        mvc.perform(post("/api/tarefas").contentType(JSON)
                        .content("{\"titulo\":\"X\",\"dataLimite\":\"2000-01-01\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem", containsString("passado")));
    }

    @Test
    void postComTituloDuplicadoEmAbertoRetorna409() throws Exception {
        criarTarefa("{\"titulo\":\"Estudar\"}");

        mvc.perform(post("/api/tarefas").contentType(JSON).content("{\"titulo\":\"  estudar \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void postPermiteTituloIgualQuandoAnteriorEstaConcluida() throws Exception {
        criarTarefa("{\"titulo\":\"Estudar\",\"status\":\"CONCLUIDA\"}");

        mvc.perform(post("/api/tarefas").contentType(JSON).content("{\"titulo\":\"Estudar\"}"))
                .andExpect(status().isCreated());
    }

    // =================================================================== PUT

    @Test
    void putAtualizaTarefaExistente() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Original\"}");

        mvc.perform(put("/api/tarefas/{id}", id).contentType(JSON)
                        .content("{\"titulo\":\"Atualizada\",\"status\":\"CONCLUIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Atualizada"))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.titulo").value("Atualizada"));
    }

    @Test
    void putPodeManterOProprioTituloSemConflito() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Mesmo título\"}");

        mvc.perform(put("/api/tarefas/{id}", id).contentType(JSON)
                        .content("{\"titulo\":\"Mesmo título\",\"descricao\":\"nova descrição\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("nova descrição"));
    }

    @Test
    void putEmTarefaInexistenteRetorna404() throws Exception {
        mvc.perform(put("/api/tarefas/9999").contentType(JSON).content("{\"titulo\":\"Qualquer\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putComDadosInvalidosRetorna400() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Original\"}");

        mvc.perform(put("/api/tarefas/{id}", id).contentType(JSON).content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putComDataNoPassadoRetorna422() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Original\"}");

        mvc.perform(put("/api/tarefas/{id}", id).contentType(JSON)
                        .content("{\"titulo\":\"Original\",\"dataLimite\":\"2000-01-01\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void putComTituloDeOutraTarefaEmAbertoRetorna409() throws Exception {
        criarTarefa("{\"titulo\":\"Primeira\"}");
        long id = criarTarefa("{\"titulo\":\"Segunda\"}");

        mvc.perform(put("/api/tarefas/{id}", id).contentType(JSON).content("{\"titulo\":\"primeira\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void putNaColecaoRetorna405() throws Exception {
        mvc.perform(put("/api/tarefas").contentType(JSON).content("{\"titulo\":\"X\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    // =================================================================== PATCH

    @Test
    void patchAlteraSomenteOStatus() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Tarefa\",\"descricao\":\"mantida\"}");

        mvc.perform(patch("/api/tarefas/{id}/status", id).contentType(JSON)
                        .content("{\"status\":\"CONCLUIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.titulo").value("Tarefa"))
                .andExpect(jsonPath("$.descricao").value("mantida"));
    }

    @Test
    void patchSemStatusRetorna400() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Tarefa\"}");

        mvc.perform(patch("/api/tarefas/{id}/status", id).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("status")));
    }

    @Test
    void patchComStatusInvalidoRetorna400() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Tarefa\"}");

        mvc.perform(patch("/api/tarefas/{id}/status", id).contentType(JSON)
                        .content("{\"status\":\"FEITA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchEmTarefaInexistenteRetorna404() throws Exception {
        mvc.perform(patch("/api/tarefas/9999/status").contentType(JSON)
                        .content("{\"status\":\"CONCLUIDA\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchReabrirTarefaConflitanteRetorna409() throws Exception {
        long concluida = criarTarefa("{\"titulo\":\"Estudar\",\"status\":\"CONCLUIDA\"}");
        criarTarefa("{\"titulo\":\"Estudar\"}");

        mvc.perform(patch("/api/tarefas/{id}/status", concluida).contentType(JSON)
                        .content("{\"status\":\"PENDENTE\"}"))
                .andExpect(status().isConflict());
    }

    // =================================================================== DELETE

    @Test
    void deleteExcluiTarefaRetorna204() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Descartável\"}");

        mvc.perform(delete("/api/tarefas/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteInexistenteRetorna404() throws Exception {
        mvc.perform(delete("/api/tarefas/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteComIdNaoNumericoRetorna400() throws Exception {
        mvc.perform(delete("/api/tarefas/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("id")));
    }

    // =================================================================== outros

    @Test
    void rotaInexistenteRetorna404Padronizado() throws Exception {
        mvc.perform(get("/api/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Endpoint não encontrado"));
    }
}

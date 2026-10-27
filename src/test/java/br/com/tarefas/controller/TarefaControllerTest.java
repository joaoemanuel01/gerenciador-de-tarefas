package br.com.tarefas.controller;

import br.com.tarefas.repository.TarefaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TarefaControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TarefaRepository repository;

    @BeforeEach
    void limparBanco() {
        repository.deleteAll();
    }

    private long criarTarefa(String json) throws Exception {
        String corpo = mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(corpo);
        return node.get("id").asLong();
    }

    // ---------------- GET ----------------

    @Test
    void getListaVaziaRetorna200() throws Exception {
        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getListaTodasAsTarefas() throws Exception {
        criarTarefa("{\"titulo\":\"Estudar Spring\"}");
        criarTarefa("{\"titulo\":\"Pagar conta de luz\"}");

        mvc.perform(get("/api/tarefas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].titulo").value("Estudar Spring"))
                .andExpect(jsonPath("$[1].titulo").value("Pagar conta de luz"));
    }

    @Test
    void getFiltraPorStatus() throws Exception {
        criarTarefa("{\"titulo\":\"A\",\"status\":\"PENDENTE\"}");
        criarTarefa("{\"titulo\":\"B\",\"status\":\"CONCLUIDA\"}");

        mvc.perform(get("/api/tarefas").param("status", "CONCLUIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titulo").value("B"));
    }

    @Test
    void getComStatusInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/tarefas").param("status", "XPTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
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
                .andExpect(jsonPath("$.mensagem", containsString("9999")));
    }

    // ---------------- POST ----------------

    @Test
    void postCriaTarefaRetorna201ComLocation() throws Exception {
        mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Entregar atividade","descricao":"Unidade HTTP",
                                 "status":"EM_ANDAMENTO","dataLimite":"2026-11-05"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/tarefas/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titulo").value("Entregar atividade"))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.dataLimite").value("2026-11-05"))
                .andExpect(jsonPath("$.criadaEm").exists());
    }

    @Test
    void postSemStatusAssumePendente() throws Exception {
        mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Sem status\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void postSemTituloRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]", containsString("titulo")));
    }

    @Test
    void postComTituloLongoDemaisRetorna400() throws Exception {
        String titulo = "x".repeat(121);
        mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + titulo + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postComJsonMalFormatadoRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{titulo: sem aspas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postComStatusInvalidoRetorna400() throws Exception {
        mvc.perform(post("/api/tarefas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"X\",\"status\":\"INEXISTENTE\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------- PUT ----------------

    @Test
    void putAtualizaTarefaExistente() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Original\"}");

        mvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Atualizada\",\"status\":\"CONCLUIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.titulo").value("Atualizada"))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        mvc.perform(get("/api/tarefas/{id}", id))
                .andExpect(jsonPath("$.titulo").value("Atualizada"));
    }

    @Test
    void putEmTarefaInexistenteRetorna404() throws Exception {
        mvc.perform(put("/api/tarefas/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Qualquer\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putComDadosInvalidosRetorna400() throws Exception {
        long id = criarTarefa("{\"titulo\":\"Original\"}");

        mvc.perform(put("/api/tarefas/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------- DELETE ----------------

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
                .andExpect(status().isBadRequest());
    }
}

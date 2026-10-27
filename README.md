# Gerenciador de Tarefas — API RESTful com Spring Boot

API para gerenciamento de tarefas pessoais. Permite **criar, listar, atualizar e excluir** tarefas através dos métodos HTTP `POST`, `GET`, `PUT` e `DELETE`.

## Tecnologias

| Item | Versão / uso |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.5 |
| spring-boot-starter-web | API REST (Spring MVC, Jackson, Tomcat embarcado) |
| spring-boot-starter-data-jpa | Persistência com Hibernate |
| spring-boot-starter-validation | Validação do corpo das requisições |
| H2 | Banco em memória (dados são perdidos ao reiniciar) |
| springdoc-openapi | Swagger UI gerado a partir do código |
| JUnit 5 + MockMvc | Testes automatizados |

## Como executar

Pré-requisitos: JDK 21 e Maven 3.9+.

```bash
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

- Swagger UI (documentação interativa): http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Console do banco H2: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:tarefasdb`, usuário `sa`, senha vazia)

Para rodar os testes automatizados:

```bash
mvn test
```

## Estrutura do projeto

```
src/main/java/br/com/tarefas
├── TarefasApplication.java        # classe principal
├── controller/TarefaController    # camada HTTP (endpoints)
├── service/TarefaService          # regras de negócio
├── repository/TarefaRepository    # acesso a dados (Spring Data JPA)
├── model/Tarefa, StatusTarefa     # entidade e enum
├── dto/TarefaRequest, TarefaResponse
└── exception/                     # exceções e tratamento global de erros
```

A separação em camadas (controller → service → repository) mantém cada classe com uma responsabilidade única. Os DTOs (`TarefaRequest` / `TarefaResponse`) evitam expor a entidade JPA diretamente.

## Modelo de dados

### Corpo aceito em POST e PUT (`TarefaRequest`)

| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `titulo` | string | **sim** | não pode ser vazio/em branco; máx. 120 caracteres |
| `descricao` | string | não | máx. 500 caracteres |
| `status` | string | não | `PENDENTE`, `EM_ANDAMENTO` ou `CONCLUIDA` (padrão: `PENDENTE`) |
| `dataLimite` | string (data) | não | formato `yyyy-MM-dd` |

### Resposta (`TarefaResponse`)

```json
{
  "id": 1,
  "titulo": "Entregar atividade",
  "descricao": "Unidade HTTP",
  "status": "EM_ANDAMENTO",
  "dataLimite": "2026-11-05",
  "criadaEm": "2026-10-27T11:30:00.123",
  "atualizadaEm": "2026-10-27T11:30:00.123"
}
```

## Endpoints

Base URL: `http://localhost:8080/api/tarefas`

| Método | Rota | Descrição | Sucesso | Falhas |
|---|---|---|---|---|
| `GET` | `/api/tarefas` | Lista todas as tarefas | `200 OK` | `400` (status inválido no filtro) |
| `GET` | `/api/tarefas/{id}` | Busca uma tarefa pelo id | `200 OK` | `404` |
| `POST` | `/api/tarefas` | Cria uma tarefa | `201 Created` (+ header `Location`) | `400` |
| `PUT` | `/api/tarefas/{id}` | Atualiza uma tarefa existente | `200 OK` | `400`, `404` |
| `DELETE` | `/api/tarefas/{id}` | Exclui uma tarefa | `204 No Content` (sem corpo) | `404`, `400` (id não numérico) |

### GET `/api/tarefas`

Parâmetro opcional de query: `status` (`PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`).

```bash
curl http://localhost:8080/api/tarefas
curl "http://localhost:8080/api/tarefas?status=CONCLUIDA"
```

Resposta `200` — lista (vazia se não houver tarefas):

```json
[
  { "id": 1, "titulo": "Estudar Spring", "descricao": null, "status": "PENDENTE",
    "dataLimite": null, "criadaEm": "...", "atualizadaEm": "..." }
]
```

### GET `/api/tarefas/{id}`

```bash
curl http://localhost:8080/api/tarefas/1
```

- `200` — tarefa encontrada
- `404` — tarefa inexistente

### POST `/api/tarefas`

```bash
curl -X POST http://localhost:8080/api/tarefas \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Entregar atividade","descricao":"Unidade HTTP","status":"EM_ANDAMENTO","dataLimite":"2026-11-05"}'
```

- `201` — criada; o header `Location` aponta para `/api/tarefas/{id}`
- `400` — título ausente/em branco, tamanho excedido, JSON mal formado, status ou data inválidos

### PUT `/api/tarefas/{id}`

Substitui os dados da tarefa pelos enviados no corpo (mesmas regras do POST).

```bash
curl -X PUT http://localhost:8080/api/tarefas/1 \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Entregar atividade","status":"CONCLUIDA"}'
```

- `200` — atualizada (retorna a tarefa)
- `400` — dados inválidos
- `404` — tarefa inexistente

### DELETE `/api/tarefas/{id}`

```bash
curl -X DELETE http://localhost:8080/api/tarefas/1 -i
```

- `204` — excluída (sem corpo)
- `404` — tarefa inexistente
- `400` — id não numérico (ex.: `/api/tarefas/abc`)

## Formato dos erros

Toda falha retorna o mesmo JSON, produzido pelo `GlobalExceptionHandler`:

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos",
  "detalhes": ["titulo: O título é obrigatório"],
  "timestamp": "2026-10-27T11:30:00.123"
}
```

```json
{
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Tarefa com id 9999 não encontrada",
  "detalhes": [],
  "timestamp": "2026-10-27T11:30:00.123"
}
```

## Testando a API

Três opções:

1. **Swagger UI** — http://localhost:8080/swagger-ui.html permite executar cada endpoint pelo navegador.
2. **Insomnia / Postman** — importe o arquivo `docs/requests.http` (ou crie as requisições conforme a tabela de endpoints). Dica: no Insomnia/Postman, use `Content-Type: application/json` nas requisições `POST` e `PUT`.
3. **Testes automatizados** — `mvn test` executa `TarefaControllerTest` (18 testes com MockMvc).

### Roteiro de teste manual sugerido

| # | Requisição | Resultado esperado |
|---|---|---|
| 1 | `GET /api/tarefas` | `200`, `[]` |
| 2 | `POST /api/tarefas` com título válido | `201` + `Location` |
| 3 | `POST /api/tarefas` com `{"titulo":""}` | `400` com `detalhes` |
| 4 | `GET /api/tarefas` | `200`, lista com 1 item |
| 5 | `PUT /api/tarefas/1` com `status: CONCLUIDA` | `200` |
| 6 | `PUT /api/tarefas/9999` | `404` |
| 7 | `DELETE /api/tarefas/1` | `204` |
| 8 | `GET /api/tarefas/1` | `404` |

## Aspectos da unidade contemplados

| Aspecto | Onde está no código |
|---|---|
| Projeto Spring Boot com dependências para API RESTful | `pom.xml` (web, data-jpa, validation, h2, springdoc) |
| `GET` — listar tarefas | `TarefaController#listar` e `#buscar` |
| `POST` — criar tarefa | `TarefaController#criar` (retorna `201` + `Location`) |
| `PUT` — atualizar tarefa | `TarefaController#atualizar` |
| `DELETE` — excluir tarefa | `TarefaController#excluir` (retorna `204`) |
| Códigos de status HTTP adequados | 200, 201, 204, 400, 404 conforme cada caso |
| Tratamento de sucesso e falha | `GlobalExceptionHandler` + validação com `@Valid` |
| Uso correto de `@RestController`, `@RequestMapping`, `@PathVariable`, `@RequestBody`, `@RequestParam` | `TarefaController` |
| Separação de responsabilidades | camadas controller / service / repository / dto |
| Testes da API | `TarefaControllerTest` (sucesso e falha de cada método) |
| Documentação | este README + Swagger UI + `docs/requests.http` |

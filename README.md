# Gerenciador de Tarefas — API RESTful com Spring Boot (v2)

API para gerenciamento de tarefas pessoais: **criar, listar, atualizar e excluir** tarefas com `GET`, `POST`, `PUT`, `PATCH` e `DELETE`.

Esta versão acrescenta, em relação à primeira entrega: arquitetura em camadas formalizada (MVC + Service + Mapper + DTO), validações adicionais, regras de negócio, paginação/filtro/ordenação, atualização parcial (`PATCH`) e tratamento padronizado de todos os erros.

## Tecnologias

| Item | Uso |
|---|---|
| Java 21 / Spring Boot 3.4.5 | base do projeto |
| spring-boot-starter-web | API REST (Spring MVC, Jackson, Tomcat embarcado) |
| spring-boot-starter-data-jpa | persistência (Hibernate, Spring Data, Specifications) |
| spring-boot-starter-validation | Bean Validation nos DTOs e parâmetros |
| H2 | banco em memória (dados perdidos ao reiniciar) |
| springdoc-openapi | Swagger UI gerado a partir do código |
| JUnit 5, Mockito, MockMvc | testes |

## Como executar

Pré-requisitos: JDK 21 e Maven 3.9+.

```bash
mvn test              # roda os 59 testes
mvn spring-boot:run   # sobe a API em http://localhost:8080
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Console H2 (desligado por padrão por segurança): para usar, mude `spring.h2.console.enabled=true` em `application.properties` e acesse `/h2-console` (JDBC URL `jdbc:h2:mem:tarefasdb`, usuário `sa`, senha vazia).

## Modelos arquiteturais aplicados

> Os padrões abaixo são os modelos arquiteturais clássicos de uma API Spring (MVC e arquitetura em camadas, com Repository, DTO, Mapper e tratamento centralizado de exceções).

```
 Cliente HTTP (Insomnia / Postman / front-end)
        │  JSON
        ▼
┌────────────────────────────────────────────────────────────┐
│ controller/   TarefaController          (C do MVC)         │  traduz HTTP ⇄ chamadas de serviço
│ exception/    GlobalExceptionHandler    (@RestControllerAdvice) │  erros → JSON padronizado
├────────────────────────────────────────────────────────────┤
│ dto/          TarefaRequest, StatusRequest,                │  contrato da API (View do MVC = JSON)
│               TarefaResponse, PaginaResponse               │
│ mapper/       TarefaMapper                                 │  DTO ⇄ entidade
├────────────────────────────────────────────────────────────┤
│ service/      TarefaService (interface)                    │  regras de negócio
│               TarefaServiceImpl                            │
├────────────────────────────────────────────────────────────┤
│ repository/   TarefaRepository (Spring Data JPA)           │  acesso a dados (padrão Repository)
│ model/        Tarefa, StatusTarefa                         │  Model do MVC (entidade JPA)
└────────────────────────────────────────────────────────────┘
        ▼
       H2 (em memória)
```

| Modelo / padrão | Como foi aplicado |
|---|---|
| **MVC** | Model = `Tarefa`; Controller = `TarefaController`; View = representação JSON (`TarefaResponse`) |
| **Arquitetura em camadas** | apresentação → serviço → repositório; cada camada só conhece a imediatamente abaixo |
| **Service Layer + inversão de dependência** | o controller depende da interface `TarefaService`, não da implementação |
| **Repository** | `TarefaRepository` esconde o acesso ao banco (Spring Data JPA + Specifications para filtros) |
| **DTO** | `TarefaRequest`/`TarefaResponse` separam o contrato da API da entidade JPA (a entidade nunca é exposta) |
| **Mapper** | `TarefaMapper` concentra a conversão e a normalização (trim de textos) |
| **Injeção de dependência** | todas as dependências por construtor (sem `@Autowired` em campo) |
| **Tratamento centralizado de exceções** | `GlobalExceptionHandler` + exceções de domínio (`TarefaNaoEncontrada`, `Conflito`, `RegraNegocio`, `ParametroInvalido`) |

## Modelo de dados

### Corpo de POST e PUT (`TarefaRequest`)

| Campo | Tipo | Obrigatório | Validações |
|---|---|---|---|
| `titulo` | string | **sim** | não vazio/em branco; máx. 120; sem `<` ou `>` |
| `descricao` | string | não | máx. 500; sem `<` ou `>` (em branco vira `null`) |
| `status` | string | não | `PENDENTE` (padrão), `EM_ANDAMENTO` ou `CONCLUIDA` |
| `dataLimite` | data | não | formato `yyyy-MM-dd`; não pode estar no passado |

Campos desconhecidos no JSON (ex.: `"id": 99`) são **rejeitados com 400**, evitando o envio de dados que a API não prevê.

### Resposta (`TarefaResponse`)

```json
{
  "id": 1,
  "titulo": "Entregar atividade",
  "descricao": "Unidade HTTP",
  "status": "EM_ANDAMENTO",
  "dataLimite": "2026-12-15",
  "criadaEm": "2026-11-11T19:30:00.123",
  "atualizadaEm": "2026-11-11T19:30:00.123"
}
```

## Endpoints

Base: `http://localhost:8080/api/tarefas` (substantivo no plural, sem verbos na URL; o método HTTP indica a ação).

| Método | Rota | Descrição | Sucesso | Falhas possíveis |
|---|---|---|---|---|
| `GET` | `/api/tarefas` | Lista paginada, com filtro e ordenação | `200` | `400` |
| `GET` | `/api/tarefas/{id}` | Busca por id | `200` | `400`, `404` |
| `POST` | `/api/tarefas` | Cria tarefa | `201` + `Location` | `400`, `409`, `415`, `422` |
| `PUT` | `/api/tarefas/{id}` | Substitui todos os campos | `200` | `400`, `404`, `409`, `415`, `422` |
| `PATCH` | `/api/tarefas/{id}/status` | Altera só o status | `200` | `400`, `404`, `409`, `415` |
| `DELETE` | `/api/tarefas/{id}` | Exclui | `204` (sem corpo) | `400`, `404` |

### GET `/api/tarefas`

Parâmetros de consulta (todos opcionais):

| Parâmetro | Padrão | Regra |
|---|---|---|
| `status` | — | `PENDENTE`, `EM_ANDAMENTO` ou `CONCLUIDA` |
| `busca` | — | trecho do título (sem diferenciar maiúsculas; máx. 100 caracteres) |
| `pagina` | `0` | ≥ 0 |
| `tamanho` | `10` | entre 1 e 50 |
| `ordenarPor` | `id` | `id`, `titulo`, `status`, `dataLimite` ou `criadaEm` |
| `direcao` | `asc` | `asc` ou `desc` |

```bash
curl "http://localhost:8080/api/tarefas?status=PENDENTE&busca=spring&tamanho=5&ordenarPor=titulo&direcao=desc"
```

Resposta `200`:

```json
{
  "conteudo": [ { "id": 1, "titulo": "Estudar Spring", "status": "PENDENTE", "...": "..." } ],
  "pagina": 0,
  "tamanho": 5,
  "totalElementos": 1,
  "totalPaginas": 1,
  "ultima": true
}
```

> Mudança em relação à v1: a lista agora vem dentro do envelope paginado (`conteudo`), com metadados para o cliente navegar pelas páginas.

### GET `/api/tarefas/{id}`

`200` tarefa encontrada · `400` id não positivo/não numérico · `404` inexistente.

### POST `/api/tarefas`

```bash
curl -X POST http://localhost:8080/api/tarefas \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Entregar atividade","descricao":"Unidade HTTP","status":"EM_ANDAMENTO","dataLimite":"2026-12-15"}'
```

- `201` criada; header `Location: /api/tarefas/{id}`
- `400` título ausente/em branco, tamanho excedido, `<`/`>` nos textos, JSON mal formado, campo desconhecido, status ou data inválidos
- `409` já existe tarefa **em aberto** (não concluída) com o mesmo título (ignora maiúsculas e espaços nas pontas)
- `415` `Content-Type` diferente de `application/json`
- `422` data limite no passado

### PUT `/api/tarefas/{id}`

Substitui todos os campos (status omitido volta para `PENDENTE`). Mesmas validações do POST; a data só é revalidada se for alterada, e o título pode ser mantido sem gerar conflito consigo mesma.

`200` · `400` · `404` · `409` · `415` · `422`.

### PATCH `/api/tarefas/{id}/status`

Atualização parcial, boa prática quando só um campo muda:

```bash
curl -X PATCH http://localhost:8080/api/tarefas/1/status \
  -H "Content-Type: application/json" -d '{"status":"CONCLUIDA"}'
```

`200` · `400` status ausente/inválido · `404` · `409` reabrir a tarefa conflitaria com outra em aberto de mesmo título.

### DELETE `/api/tarefas/{id}`

`204` excluída · `400` id inválido (ex.: `/api/tarefas/abc`) · `404` inexistente.

## Formato único de erros

Toda falha — de validação, de negócio, de protocolo HTTP ou inesperada — retorna o mesmo JSON:

```json
{
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos",
  "caminho": "/api/tarefas",
  "detalhes": ["titulo: O título é obrigatório"],
  "timestamp": "2026-11-11T19:30:00.123"
}
```

| Código | Quando acontece |
|---|---|
| `400` | validação de corpo/parâmetros, JSON inválido, campo desconhecido, id inválido, ordenação inválida |
| `404` | tarefa ou endpoint inexistente |
| `405` | método HTTP não permitido na rota (ex.: `PUT /api/tarefas`) |
| `409` | conflito com o estado atual (título duplicado em aberto) |
| `415` | corpo que não é `application/json` |
| `422` | dados bem formados, mas que violam regra de negócio (data no passado) |
| `500` | erro inesperado: registrado no log do servidor; o cliente recebe apenas uma mensagem genérica, **sem stack trace nem detalhes internos** |

## Validações e segurança

- **Bean Validation** nos DTOs (`@NotBlank`, `@Size`, `@Pattern`, `@NotNull`) e nos parâmetros (`@Positive`, `@Min`, `@Max`).
- **Entrada saneada:** `<` e `>` rejeitados em textos (barreira contra injeção de HTML/script); espaços nas pontas removidos.
- **Sem mass assignment:** o JSON só pode conter os campos previstos; `id`, `criadaEm` etc. nunca vêm do cliente.
- **Lista branca de ordenação:** o cliente não escolhe campos arbitrários da entidade.
- **Limites de paginação:** `tamanho` máx. 50, impedindo consultas gigantes.
- **Busca segura:** `%` e `_` digitados são tratados como texto literal; consultas via JPA (sem concatenar SQL).
- **Exposição mínima:** entidade JPA nunca é retornada; erros 500 sem detalhes internos; console H2 desligado por padrão.

## Testes

`mvn test` executa **59 testes**:

| Classe | Testes | O que cobre |
|---|---|---|
| `TarefaControllerTest` | 46 | integração HTTP (MockMvc + H2): sucesso e falha de cada endpoint, paginação, filtro, ordenação, validações, 400/404/405/409/415/422 |
| `TarefaServiceImplTest` | 10 | regras de negócio isoladas, com repositório simulado (Mockito) |
| `TarefaMapperTest` | 3 | conversões e normalização |

Para testar manualmente: **Swagger UI**, ou importe `docs/requests.http` (IntelliJ/VS Code) e replique no Insomnia/Postman.

### Roteiro manual sugerido

| # | Requisição | Esperado |
|---|---|---|
| 1 | `GET /api/tarefas` | `200`, `conteudo: []` |
| 2 | `POST` com título válido | `201` + `Location` |
| 3 | `POST` com o mesmo título | `409` |
| 4 | `POST` com `{"titulo":""}` | `400` com `detalhes` |
| 5 | `POST` com `"dataLimite":"2000-01-01"` | `422` |
| 6 | `PATCH /1/status` `{"status":"CONCLUIDA"}` | `200` |
| 7 | `PUT /9999` | `404` |
| 8 | `GET ?tamanho=1000` | `400` |
| 9 | `DELETE /1` | `204` |
| 10 | `GET /1` | `404` |

## Aspectos da unidade contemplados

| Aspecto do enunciado | Onde está |
|---|---|
| 1. Revisão dos métodos HTTP e boas práticas | `TarefaController`: verbos corretos, `201`+`Location` no POST, `204` no DELETE, `consumes`/`produces` JSON, `PATCH` para atualização parcial |
| 1. Validações adicionais (integridade e segurança) | `TarefaRequest` (Bean Validation, `@Pattern` anti-HTML), `@Positive/@Min/@Max` nos parâmetros, regras em `TarefaServiceImpl` (data no passado, título duplicado), lista branca de ordenação, `fail-on-unknown-properties` |
| 2. Controladores RESTful com CRUD | `TarefaController` (`@RestController`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`, `@RequestBody`, `@PathVariable`, `@RequestParam`) |
| 3. Mapeamento de endpoints e convenções | recurso no plural `/api/tarefas`, `/{id}` para itens, sub-recurso `/{id}/status`; sem verbos nas URLs |
| 4. Requisições inválidas e erros do servidor | `GlobalExceptionHandler`: 400, 404, 405, 409, 415, 422 e 500 (com log e mensagem segura) |
| 4. Respostas úteis ao cliente | `ErroResponse` (status, mensagem, caminho, detalhes por campo, timestamp), `PaginaResponse` (metadados de paginação), header `Location` |
| 5. Testes abrangentes | 59 testes (integração, unidade de serviço e mapper) |
| 5. Documentação atualizada | este README, Swagger UI (`@Operation`/`@ApiResponse` em cada endpoint), `docs/requests.http` |
| Modelos arquiteturais discutidos | seção "Modelos arquiteturais aplicados" (MVC, camadas, Service/interface, Repository, DTO, Mapper, exceções centralizadas) |

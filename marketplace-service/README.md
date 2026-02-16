# Marketplace Service

Serviço responsável por gerenciar pedidos (orders) e webhooks do marketplace. Implementa o padrão Transactional Outbox para garantir entrega confiável de eventos.

## Segurança

Este serviço implementa autenticação service-to-service usando tokens simples no header `Authorization`. Apenas serviços com o token correto podem acessar os endpoints protegidos.

## Endpoints

### Públicos

- `GET /actuator/health` - Health check
- `GET /swagger-ui/index.html` - Documentação da API

### Protegidos (requerem token)

- `POST /api/v1/webhooks` - Registrar webhook para receber eventos
- `POST /api/v1/orders` - Criar novo pedido
- `GET /api/v1/orders/{orderId}` - Recuperar pedido por ID
- `PATCH /api/v1/orders/{orderId}/status` - Atualizar status do pedido

## Executar Localmente

```bash
# 1. Configure as variáveis de ambiente
cp .env.example .env
# Edite o .env com seus valores

# 2. Inicie o banco de dados
cd ../docker
docker-compose up -d postgres

# 3. Execute o serviço
./gradlew bootRun
```

## Testar

```bash
# Testes unitários
./gradlew test

# Testes de integração
./gradlew integrationTest

# Testes de arquitetura
./gradlew archTest

# Todos os testes
./gradlew build
```

## Exemplos de Chamadas

### Registrar Webhook

```bash
curl -X POST http://localhost:8000/api/v1/webhooks \
  -H "Content-Type: application/json" \
  -H "Authorization: marketplace-service-secret" \
  -d '{
    "store_ids": ["store-1"],
    "callback_url": "http://receiver-service:8001/api/v1/events",
    "token": "receiver-service-secret"
  }'
```

### Criar Pedido

```bash
curl -X POST http://localhost:8000/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: marketplace-service-secret" \
  -d '{
    "store_id": "store-1",
    "items": [
      {
        "product_name": "Placa de video RTX 5090",
        "quantity": 1,
        "unit_price": 8980,
        "discount": 100,
        "tax": 10
      }
    ]
  }'
```

### Recuperar Pedido por ID

```bash
curl -X GET http://localhost:8000/api/v1/orders/cf062c2f-0a31-472c-a34f-356f9461014f \
  -H "Content-Type: application/json" \
  -H "Authorization: marketplace-service-secret"
```

### Atualizar Status do Pedido

```bash
curl -X PATCH http://localhost:8000/api/v1/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -H "Authorization: marketplace-service-secret" \
  -d '{
    "status": "PAID"
  }'
```

## Tecnologias

- Kotlin 2.3.0
- Spring Boot 4.0.2
- PostgreSQL
- Flyway (migrations)
- Resilience4j (circuit breaker e retry)
- Testcontainers (testes)

## Estrutura do Projeto

```
marketplace-service/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/thiagosena/marketplace/
│   │   │       ├── application/
│   │   │       │   ├── config/          # Configurações (Security, OpenAPI)
│   │   │       │   ├── security/        # Filtros de autenticação
│   │   │       │   ├── scheduler/       # Jobs agendados (Outbox)
│   │   │       │   └── web/             # Controllers e DTOs
│   │   │       ├── domain/              # Lógica de negócio
│   │   │       └── resources/           # Infraestrutura (DB, clients)
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/migration/            # Scripts Flyway
│   ├── integrationTest/                 # Testes de integração
│   ├── archTest/                        # Testes de arquitetura
│   └── test/                            # Testes unitários
├── scripts/                             # Scripts utilitários
├── docker/                              # Dockerfiles
└── marketplace.http                     # Exemplos de requisições HTTP
```

## Variáveis de Ambiente

| Variável                      | Descrição                          | Padrão                                         |
|-------------------------------|------------------------------------|------------------------------------------------|
| `SERVER_PORT`                 | Porta do servidor                  | `8000`                                         |
| `SERVICE_SHARED_SECRET`       | Token de autenticação              | `marketplace-service-secret`                   |
| `DATABASE_URL`                | URL do PostgreSQL                  | `jdbc:postgresql://localhost:5432/marketplace` |
| `DATABASE_USER`               | Usuário do banco                   | `postgres`                                     |
| `DATABASE_PASSWORD`           | Senha do banco                     | `admin`                                        |
| `WEBHOOK_TIMEOUT`             | Timeout para chamadas webhook (s)  | `5`                                            |
| `OUTBOX_EVENT_MAX_RETRIES`    | Máximo de tentativas de reenvio    | `5`                                            |
| `OUTBOX_EVENT_BATCH_SIZE`     | Tamanho do lote de processamento   | `100`                                          |
| `OUTBOX_EVENT_BASE_DELAY`     | Delay base entre tentativas (s)    | `10`                                           |
| `OUTBOX_EVENT_MAX_DELAY`      | Delay máximo entre tentativas (s)  | `3600`                                         |
| `OUTBOX_EVENT_JITTER_FACTOR`  | Fator de jitter para delays (s)    | `15`                                           |
| `OUTBOX_EVENT_FIXED_DELAY`    | Intervalo do scheduler (ms)        | `5000`                                         |

## Padrão Transactional Outbox

Este serviço implementa o padrão Transactional Outbox para garantir entrega confiável de eventos:

1. Quando um pedido é criado/atualizado, o evento é salvo na tabela `outbox_events` na mesma transação
2. Um scheduler processa eventos pendentes periodicamente
3. Eventos são enviados para os webhooks registrados
4. Implementa retry com backoff exponencial e circuit breaker
5. Eventos com falha são reprocessados automaticamente

## Troubleshooting

### Erro 401 ao chamar endpoints

- Verifique se o header `Authorization` está presente
- Confirme que o token está correto (sem prefixo Bearer)
- Veja os logs para mais detalhes

### Eventos não estão sendo enviados

- Verifique se há webhooks registrados para a loja
- Consulte a tabela `outbox_events` para ver eventos pendentes
- Verifique os logs do scheduler para erros de processamento
- Confirme que o circuit breaker não está aberto

### Erro de conexão com banco de dados

- Verifique se o PostgreSQL está rodando
- Confirme as credenciais nas variáveis de ambiente
- Teste a conexão: `psql -h localhost -U postgres -d marketplace`
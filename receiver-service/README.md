# Receiver Service

Serviço responsável por receber e processar eventos de webhook do Marketplace Service.

## Segurança

Este serviço implementa autenticação service-to-service usando tokens simples no header `Authorization`. Apenas serviços
com o token correto podem enviar eventos.

## Endpoints

### Públicos

- `GET /actuator/health` - Health check
- `GET /swagger-ui/index.html` - Documentação da API

### Protegidos (requerem token)

- `POST /api/v1/events` - Receber eventos de webhook

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

## Exemplo de Chamada

```bash
curl -X POST http://localhost:8001/api/v1/events \
  -H "Content-Type: application/json" \
  -H "Authorization: seu-token-aqui" \
  -d '{
    "idempotency_key": "unique-key-123",
    "event_type": "order.created",
    "order_id": "order-123",
    "store_id": "store-456",
    "created_at": "2026-02-16T10:00:00Z"
  }'
```

## Tecnologias

- Kotlin 2.3.0
- Spring Boot 4.0.2
- PostgreSQL
- Flyway (migrations)
- OpenFeign (client HTTP)
- Resilience4j (circuit breaker)
- Testcontainers (testes)

## Estrutura do Projeto

```
receiver-service/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/thiagosena/receiver/
│   │   │       ├── application/
│   │   │       │   ├── config/          # Configurações (Security, OpenAPI)
│   │   │       │   ├── security/        # Filtros de autenticação
│   │   │       │   ├── scheduler/       # Jobs agendados
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
├── examples/                            # Exemplos de integração
└── docker/                              # Dockerfiles
```

## Variáveis de Ambiente

| Variável                | Descrição             | Padrão                                      |
|-------------------------|-----------------------|---------------------------------------------|
| `SERVER_PORT`           | Porta do servidor     | `8001`                                      |
| `SERVICE_SHARED_SECRET` | Token de autenticação | `change-me-in-production`                   |
| `DATABASE_URL`          | URL do PostgreSQL     | `jdbc:postgresql://localhost:5432/receiver` |
| `DATABASE_USER`         | Usuário do banco      | `postgres`                                  |
| `DATABASE_PASSWORD`     | Senha do banco        | `admin`                                     |

## Troubleshooting

### Erro 401 ao chamar /api/v1/events

- Verifique se o header `Authorization` está presente
- Confirme que o token está correto (sem prefixo Bearer)
- Veja os logs para mais detalhes
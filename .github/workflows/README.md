# GitHub Actions Workflows

Este projeto utiliza workflows separados para cada serviço, permitindo builds e deploys independentes.

## Workflows

### marketplace-service.yml
Pipeline CI/CD para o Marketplace Service
- Executa apenas quando há mudanças em `marketplace-service/**`
- Build, testes (incluindo Testcontainers), análise SonarCloud e deploy na AWS ECS

### receiver-service.yml
Pipeline CI/CD para o Receiver Service
- Executa apenas quando há mudanças em `receiver-service/**`
- Build, testes (incluindo Testcontainers), análise SonarCloud e deploy na AWS ECS

## Testcontainers

Os workflows estão configurados para executar testes de integração com Testcontainers:

- **Docker disponível**: Os runners do GitHub Actions (ubuntu-latest) já vêm com Docker instalado
- **PostgreSQL**: Os testes de integração iniciam automaticamente um container PostgreSQL 16
- **Isolamento**: Cada execução de teste usa um container limpo
- **Performance**: Cache do Gradle otimiza o download de dependências e imagens Docker

### Como funciona

1. Durante os testes de integração, o Testcontainers:
   - Detecta o Docker disponível no runner
   - Baixa a imagem `postgres:16` (se não estiver em cache)
   - Inicia um container PostgreSQL temporário
   - Configura automaticamente a conexão via `@ServiceConnection`
   - Executa os testes
   - Remove o container ao final

2. Não é necessária nenhuma configuração adicional nos workflows

## Configuração Necessária

### Secrets do GitHub

Configure os seguintes secrets no repositório (Settings > Secrets and variables > Actions):

#### AWS
- `AWS_ACCESS_KEY_ID`: Access Key ID da AWS
- `AWS_SECRET_ACCESS_KEY`: Secret Access Key da AWS

#### SonarCloud
- `SONAR_TOKEN`: Token de autenticação do SonarCloud
- `SONAR_ORGANIZATION`: Nome da organização no SonarCloud
- `SONAR_PROJECT_KEY_MARKETPLACE`: Project key do marketplace-service
- `SONAR_PROJECT_KEY_RECEIVER`: Project key do receiver-service

### Configuração do SonarCloud

1. Acesse https://sonarcloud.io
2. Faça login com sua conta GitHub
3. Crie uma organização (se ainda não tiver)
4. Crie dois projetos:
   - marketplace-service
   - receiver-service
5. Copie os project keys e adicione nos secrets do GitHub
6. Gere um token em Account > Security > Generate Tokens
7. Adicione o token nos secrets do GitHub como `SONAR_TOKEN`

### Recursos AWS Necessários

Para cada serviço, você precisa criar:

#### ECR (Elastic Container Registry)
- Repositório: `marketplace-service`
- Repositório: `receiver-service`

#### ECS (Elastic Container Service)
- Cluster: `marketplace-cluster`
- Cluster: `receiver-cluster`
- Task Definition: `marketplace-service`
- Task Definition: `receiver-service`
- Service: `marketplace-service`
- Service: `receiver-service`

### Ajustes nos Workflows

Edite as variáveis de ambiente no início de cada arquivo `.yml` conforme sua infraestrutura:

```yaml
env:
  AWS_REGION: us-east-2
  ECR_REPOSITORY: nome-do-repositorio
  ECS_SERVICE: nome-do-service
  ECS_CLUSTER: nome-do-cluster
  ECS_TASK_DEFINITION: nome-da-task-definition
  CONTAINER_NAME: nome-do-container
```

## Vantagens da Separação

- **Builds independentes**: Mudanças em um serviço não disparam build do outro
- **Deploys isolados**: Cada serviço pode ser deployado separadamente
- **Melhor rastreabilidade**: Histórico de builds separado por serviço
- **Otimização de recursos**: Economiza minutos de CI/CD
- **Manutenção facilitada**: Configurações específicas por serviço

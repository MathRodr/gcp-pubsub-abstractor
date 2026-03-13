# gcp-pubsub-abstractor

Biblioteca para integração padronizada com **Google Cloud Pub/Sub**,
utilizando o SDK nativo da Google, com foco em:

- Cloud-native architecture
- Controle de lifecycle
- Graceful shutdown
- Flow control e paralelismo configurável
- Suporte a ACK automático ou manual
- HealthIndicator para Actuator
- Suporte a Emulator para desenvolvimento local

---

## 🎯 Objetivo

Abstrair o uso do Pub/Sub para os microserviços do time, garantindo:

- Código consistente
- Configuração via YAML
- Segurança em produção
- Integração com Kubernetes
- Observabilidade

Retry e DLQ são responsabilidade da infraestrutura (Terraform / GCP ou similar).

---

# 🚀 Instalação

## 1️⃣ Publicar localmente

```bash
./gradlew publishToMavenLocal
# Pedidos360 — Backend (`Pedidos360-backend`)

Plataforma backend cloud-native basada en microservicios reactivos con **Java / Spring Boot**, diseñada para la asignatura **DSY1107 — Desarrollo de Aplicaciones Cloud Native** (Duoc UC, sección 002).

Combina **Apache Kafka (modo KRaft)** para streaming de eventos de negocio y auditoría con **Apache ActiveMQ (clúster Master/Slave con KahaDB)** para tareas asíncronas transaccionales, desplegado sobre **AWS** (EC2 privada, RDS PostgreSQL 18.3, ALB interno, VPC Link y API Gateway).

---

## Estructura del repositorio

```text
Pedidos360-backend/
├── Infraestructura/
│   ├── ActiveMQ/
│   │   ├── activemq.xml
│   │   └── docker-compose.activemq-cluster.yml
│   ├── Kafka/
│   │   └── docker-compose.kafka.yml
│   └── produccion/
│       ├── docker-compose.yml
│       └── docker-compose.prod.yml
├── ms-pedidos360-administrator/   # Control plane de Kafka y ActiveMQ
├── ms-pedidos360-audit/           # Auditoría en tiempo real (Kafka + SSE + PostgreSQL)
├── ms-pedidos360-bff/             # Backend for Frontend (Seguridad Entra ID, CORS y enrutamiento)
├── ms-pedidos360-catalog/         # Catálogo de productos (Cloudinary + PostgreSQL)
├── ms-pedidos360-notify/          # Notificaciones a clientes, cocina y facturación (ActiveMQ + SSE)
├── ms-pedidos360-order/           # Orquestación de pedidos (Productor Kafka y ActiveMQ + PostgreSQL)
└── ms-pedidos360-report/          # Reportes y KPIs de ventas (Consumidor Kafka + PostgreSQL)
```

---

## Microservicios

Cada microservicio cuenta con su propio `Dockerfile`, wrapper de Maven (`./mvnw`) y configuración en `src/main/resources/application.yml`.

| Microservicio | Responsabilidad principal | Persistencia (PostgreSQL / Flyway) | Rol en mensajería |
|---|---|---|---|
| **`ms-pedidos360-bff`** | Punto de entrada único para el frontend, validación JWT con Microsoft Entra ID y CORS. | Sin BD propia | Proxy REST y SSE hacia los microservicios internos. |
| **`ms-pedidos360-catalog`** | Gestión de productos, stock y carga de imágenes en Cloudinary. | BD `ms-catalog` (esquema `catalog`, `V1__esquema_inicial.sql`) | Consultado vía REST por `order`. |
| **`ms-pedidos360-order`** | Creación y ciclo de vida de pedidos. Declara los tópicos iniciales en `KafkaTopicConfig`. | BD `ms-orders` (esquema `orders`, migraciones `V1`–`V3`) | **Productor** en Kafka (`orders.events`, `audit.timeline`) y ActiveMQ (colas de cocina, correo y boleta). |
| **`ms-pedidos360-notify`** | Emisión de notificaciones por correo y streams SSE para clientes y cocina. | Sin BD relacional | **Consumidor** de colas de ActiveMQ (`NotifyMessageListener`). |
| **`ms-pedidos360-audit`** | Registro inmutable de eventos del sistema y streaming SSE de auditoría. | BD `ms-audit` (esquema `audit`, `V1__Init_audit_schema.sql`) | **Consumidor** Kafka (`audit-service-group`) sobre `audit.timeline` con DLT. |
| **`ms-pedidos360-report`** | Consolidación de métricas, KPIs y productos más vendidos. | BD `ms-reports` (esquema `reports`, `V1__Init_report_schema.sql`) | **Consumidor** Kafka (`report-service-group`) sobre `orders.events` con DLT. |
| **`ms-pedidos360-administrator`** | Inspección y administración controlada de clústeres, tópicos, grupos, offsets y colas. | Sin BD propia | Cliente administrativo de Kafka (`AdminClient`) y ActiveMQ (Jolokia). |

---

## Arquitectura de mensajería y confiabilidad

### Apache Kafka (Streaming de eventos)

* **Topología:** Clúster de **3 brokers en modo KRaft** (Apache Kafka `3.9.0`), eliminando la dependencia de ZooKeeper y soportando factor de replicación `3`.
* **Tópicos principales:**

| Tópico | Particiones | Réplicas | Política de limpieza (`cleanup.policy`) | Retención (`retention.ms`) | Uso |
|---|---|---|---|---|---|
| **`orders.events`** | 3 | 3 | `delete` | 7 días (`604800000`) | Eventos de pedidos consumidos por `ms-pedidos360-report`. |
| **`audit.timeline`** | 3 | 3 | `compact,delete` | 30 días (`2592000000`) | Línea de tiempo de eventos consumida por `ms-pedidos360-audit`. |
| **`orders.events.DLT`** | 3 | 3 | `delete` | 14 días (`1209600000`) | Dead Letter Topic para eventos fallidos de `orders.events`. |
| **`audit.timeline.DLT`** | 3 | 3 | `delete` | 14 días (`1209600000`) | Dead Letter Topic para eventos fallidos de `audit.timeline`. |

* **Resiliencia en consumidores (`report` y `audit`):**
    * **Paralelismo y confirmación:** `concurrency: 3` (1 hilo por partición), `ack-mode: record` y estrategia de rebalanceo incremental `CooperativeStickyAssignor`.
    * **Deserialización protegida:** `ErrorHandlingDeserializer` delegando a `JacksonJsonDeserializer` (`use.type.headers: false` para desacoplar DTOs entre microservicios).
    * **Validación y DLT:** validación automática de DTOs (`@Valid`) en `@KafkaListener` mediante `KafkaListenerConfigurer`, política de reintentos `FixedBackOff(2000L, 3)` para errores transitorios y desvío inmediato sin reintentos hacia `.DLT` vía `DeadLetterPublishingRecoverer` ante `DeserializationException`, `IllegalArgumentException` o `MethodArgumentNotValidException`.

### Apache ActiveMQ (Tareas asíncronas)

* **Topología:** Clúster **Master/Slave** con almacenamiento compartido **KahaDB** (`Infraestructura/ActiveMQ/activemq.xml`).
* **Responsabilidad:** Gestión de colas transaccionales para tickets de cocina, envío de correos y generación de comprobantes/boletas, manteniendo su propia **Dead Letter Queue (DLQ)** independiente de los DLT de Kafka.

---

## Requisitos previos

* **Java 21** (o superior compatible con los `pom.xml`)
* **Docker** y **Docker Compose v2**
* **PostgreSQL 16/18** (local o instancia Amazon RDS)
* *(Solo para despliegue/operación en AWS)* **AWS CLI v2** y **Session Manager Plugin**

---

## Variables de entorno

Crea un archivo `.env` en `Infraestructura/produccion/` (no versionar en Git) con las credenciales correspondientes a tu entorno:

```env
# Base de datos (PostgreSQL local o RDS)
DB_HOST=<HOST_POSTGRES_O_RDS>
DB_PORT=5432
DB_USER=<DB_USER>
DB_PASSWORD=<DB_PASSWORD>

# Seguridad / Microsoft Entra ID (BFF)
ENTRA_TENANT_ID=<ENTRA_TENANT_ID>
ENTRA_CLIENT_ID=<ENTRA_CLIENT_ID>
FRONTEND_URL=http://localhost:4200

# Cloudinary (ms-pedidos360-catalog)
CLOUDINARY_CLOUD_NAME=<CLOUD_NAME>
CLOUDINARY_API_KEY=<API_KEY>
CLOUDINARY_API_SECRET=<API_SECRET>

# Correo SMTP (ms-pedidos360-notify)
MAIL_HOST=<SMTP_HOST>
MAIL_PORT=587
MAIL_USERNAME=<MAIL_USER>
MAIL_PASSWORD=<MAIL_PASSWORD>

# Mensajería
KAFKA_BROKER_URL=kafka1:9092,kafka2:9092,kafka3:9092
ACTIVEMQ_USER=<ACTIVEMQ_USER>
ACTIVEMQ_PASSWORD=<ACTIVEMQ_PASSWORD>
```

> **Nota de bases de datos:** Antes del primer arranque, asegúrate de que existan las 4 bases de datos (`ms-catalog`, `ms-orders`, `ms-audit`, `ms-reports`) en PostgreSQL. Flyway crea automáticamente los esquemas y tablas al iniciar cada microservicio.

---

## Ejecución en entorno local

### 1. Compilar los microservicios (opcional si reconstruyes imágenes locales)

Desde la raíz de cada microservicio (`ms-pedidos360-*`), compila el `.jar` y genera la imagen Docker:

```bash
cd ms-pedidos360-order
./mvnw clean package -DskipTests
docker build -t <tu-usuario>/ms-pedidos360-order:latest .
```

Repite el proceso para los demás servicios (`bff`, `catalog`, `notify`, `audit`, `report`, `administrator`) si realizas cambios en el código fuente.

### 2. Levantar la infraestructura de mensajería y servicios

Puedes levantar el stack completo desde `Infraestructura/produccion/`:

```bash
cd Infraestructura/produccion
docker compose up -d
```

*(Si deseas levantar los clústeres de mensajería por separado en desarrollo, utiliza `Infraestructura/ActiveMQ/docker-compose.activemq-cluster.yml` y `Infraestructura/Kafka/docker-compose.kafka.yml`).*

> **Orden recomendado en el primer arranque:** Inicia primero los brokers de Kafka y `ms-pedidos360-order` para que `KafkaTopicConfig` cree los tópicos con **3 particiones y 3 réplicas** antes de que `ms-pedidos360-audit` y `ms-pedidos360-report` se suscriban (evitando que el broker los auto-cree con factor de replicación `1`).

---

## Despliegue en producción (AWS)

En la instancia **EC2 privada** (conectada vía AWS Systems Manager):

1. Posicionarse en la carpeta de despliegue con `docker-compose.prod.yml` y `.env` configurado:

```bash
cd ~/pedidos360
```

2. Descargar las imágenes actualizadas y levantar los contenedores:

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

3. Verificar que todos los contenedores estén activos:

```bash
docker compose -f docker-compose.prod.yml ps
```

### Acceso a herramientas internas mediante túneles SSM

Dado que la EC2 y el RDS residen en subredes privadas sin IP pública, el monitoreo y los streams SSE se exponen de forma segura mediante port-forwarding de **AWS SSM**:

```bash
export AWS_PROFILE=lab

# Túnel BFF (requerido para SSE en desarrollo desde http://localhost:4200)
aws ssm start-session --target <EC2_INSTANCE_ID> \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["8080"],"localPortNumber":["8080"]}'

# Túnel Kafka UI (http://localhost:8090)
aws ssm start-session --target <EC2_INSTANCE_ID> \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["8090"],"localPortNumber":["8090"]}'

# Túnel consola ActiveMQ Master (http://localhost:8161/admin o 8162 según nodo activo)
aws ssm start-session --target <EC2_INSTANCE_ID> \
  --document-name AWS-StartPortForwardingSession \
  --parameters '{"portNumber":["8162"],"localPortNumber":["8162"]}'
```

---

## Verificación de persistencia en PostgreSQL / RDS

Desde la EC2 privada (sin instalar `psql` en el host):

```bash
docker run --rm -it -e PGSSLMODE=require postgres:18-alpine \
  psql -h <RDS_ENDPOINT> -p 5432 -U <DB_USER> -d ms-orders
```

Comandos útiles dentro de `psql` (cada microservicio utiliza un esquema dedicado):

```sql
\c "ms-orders"   -- \dt orders.*
\c "ms-audit"    -- \dt audit.*
\c "ms-reports"  -- \dt reports.*
\c "ms-catalog"  -- \dt catalog.*
```

---

## Integrantes

* Sebastián González

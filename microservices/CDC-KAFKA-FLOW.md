# CDC Kafka Flow - Debezium PostgreSQL Integration

This document provides comprehensive instructions to set up and test the **Change Data Capture (CDC)** Kafka flow using Debezium, PostgreSQL, Kafka, and Spring Cloud Stream.

## Overview

The architecture consists of:
- **PostgreSQL 15**: Source database with logical replication enabled (wal_level=logical)
- **Apache Kafka & Zookeeper**: Message broker
- **Debezium Kafka Connect**: CDC connector that captures changes from PostgreSQL
- **pgAdmin**: Web-based PostgreSQL administration tool
- **Spring Cloud Stream Consumer**: notification-service listening for CDC events

## Architecture Flow

```
PostgreSQL (products table) 
    ↓ (WAL changes via pgoutput)
Debezium PostgreSQL Connector
    ↓ (transforms to Kafka messages)
Kafka Topic: product-service.public.products
    ↓ (Spring Cloud Stream bindings)
notification-service Consumer
    ↓ (processProductCdc Bean)
Console Output: "[CDC FLOW] ..."
```

## Prerequisites

- Docker & Docker Compose (v20+)
- curl (for API calls)
- PostgreSQL CLI tools (psql) - optional but recommended

## Step 1: Start the Infrastructure Stack

Navigate to the microservices directory and start all services:

```bash
cd d:\Code_projects\microservices-spring-gygy\microservices
docker compose -f docker-compose-cdc.yml up -d
```

**Expected output:**
```
✓ Network cdc-net created
✓ Container zookeeper created
✓ Container postgres created
✓ Container pgadmin created
✓ Container kafka created
✓ Container connect created
```

**Verify services are running:**

```bash
docker compose -f docker-compose-cdc.yml ps
```

**Expected status:**
```
NAME      IMAGE                          STATUS
zookeeper confluentinc/cp-zookeeper:7.4.0 Up 10 seconds
postgres  postgres:15                     Up 10 seconds
pgadmin   dpage/pgadmin4                  Up 10 seconds
kafka     confluentinc/cp-kafka:7.4.0     Up 8 seconds
connect   debezium/connect:2.3            Up 5 seconds
```

**Wait 30-40 seconds** for Kafka Connect to fully initialize.

## Step 2: Register the Debezium Connector

Once Kafka Connect is healthy, register the PostgreSQL CDC connector by sending a POST request to the Kafka Connect API:

```bash
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
  --data @product-connector.json http://localhost:8083/connectors
```

**Expected response (HTTP 201):**
```json
{
  "name": "product-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    ...
  },
  "tasks": [],
  "type": "source"
}
```

**Verify connector status:**

```bash
curl http://localhost:8083/connectors/product-connector/status
```

Expected `"state": "RUNNING"` and `"worker_id": "connect:8083"`.

## Step 3: Verify Kafka Topic Creation

Check that the Kafka topic was created with the expected name:

```bash
docker compose -f docker-compose-cdc.yml exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**Expected output includes:**
```
product-service.public.products
```

## Step 4: Test the CDC Flow

### 4.1 Access pgAdmin (Optional GUI)

- URL: [http://localhost:5050](http://localhost:5050)
- Email: `admin@local`
- Password: `admin`
- Add PostgreSQL server: hostname = `postgres`, port = `5432`, user = `postgres`, password = `postgres`

### 4.2 Insert a New Product (Test CREATE Operation)

**Option A: Using curl with docker exec**

```bash
docker compose -f docker-compose-cdc.yml exec postgres psql -U postgres -d productsdb -c \
  "INSERT INTO public.products (id, name, price, stock, description) \
   VALUES (2, 'Gaming Laptop', 1299.99, 15, 'High-performance laptop for gaming');"
```

**Option B: Using psql directly (if installed locally)**

Connect to the database:
```bash
psql -h localhost -U postgres -d productsdb
```

Then execute:
```sql
INSERT INTO public.products (id, name, price, stock, description) 
VALUES (2, 'Gaming Laptop', 1299.99, 15, 'High-performance laptop for gaming');
```

**Expected CDC message on Kafka:**
- Operation: `"c"` (Create)
- Topic: `product-service.public.products`
- Flattened payload (via ExtractNewRecordState SMT):
```json
{
  "id": 2,
  "name": "Gaming Laptop",
  "price": 1299.99,
  "stock": 15,
  "description": "High-performance laptop for gaming",
  "__op": "c",
  "__ts_ms": 1718025600000
}
```

**Expected notification-service console output:**
```
[CDC FLOW] Yeni Ürün Eklendi! Ürün Adı: Gaming Laptop
```

### 4.3 Update the Product (Test UPDATE Operation)

**Option A: Using curl with docker exec**

```bash
docker compose -f docker-compose-cdc.yml exec postgres psql -U postgres -d productsdb -c \
  "UPDATE public.products SET stock = 10 WHERE id = 2;"
```

**Option B: Using psql directly**

```sql
UPDATE public.products SET stock = 10 WHERE id = 2;
```

**Expected CDC message on Kafka:**
- Operation: `"u"` (Update)
- Flattened payload:
```json
{
  "id": 2,
  "name": "Gaming Laptop",
  "price": 1299.99,
  "stock": 10,
  "description": "High-performance laptop for gaming",
  "__op": "u",
  "__ts_ms": 1718025610000
}
```

**Expected notification-service console output:**
```
[CDC FLOW] Ürün Güncellendi! Yeni Stok: 10
```

### 4.4 Delete a Product (Test DELETE Operation)

**Option A: Using curl with docker exec**

```bash
docker compose -f docker-compose-cdc.yml exec postgres psql -U postgres -d productsdb -c \
  "DELETE FROM public.products WHERE id = 2;"
```

**Option B: Using psql directly**

```sql
DELETE FROM public.products WHERE id = 2;
```

**Expected CDC message on Kafka:**
- Operation: `"d"` (Delete)
- Before state is preserved in flattened payload

**Expected notification-service console output:**
```
[CDC FLOW] Ürün Silindi! Ürün ID: 2
```

## Step 5: Monitor Kafka Messages (Optional)

**Consume messages in real-time:**

```bash
docker compose -f docker-compose-cdc.yml exec kafka kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic product-service.public.products --from-beginning --property print.key=true
```

**Consume using Kafka UI (advanced setup):**
Add a kafka-ui service to docker-compose-cdc.yml and access at `http://localhost:8080`.

## Running notification-service

Start your Spring Boot notification-service:

```bash
cd notification-service
mvn clean spring-boot:run
```

**Expected startup logs:**
```
...
Started NotificationServiceApplication
Listening on port 8088
...
```

**When CDC events arrive, you should see in the console:**
```
[CDC FLOW] Yeni Ürün Eklendi! Ürün Adı: Gaming Laptop
```

## Troubleshooting

### Issue: Connector fails to start

**Symptom:** `curl` returns error or connector state is FAILED

**Solution:**
1. Check connector logs: `docker compose -f docker-compose-cdc.yml logs connect`
2. Verify PostgreSQL is healthy: `docker compose -f docker-compose-cdc.yml logs postgres`
3. Ensure `wal_level=logical` is set: `docker compose -f docker-compose-cdc.yml exec postgres psql -U postgres -c "SHOW wal_level;"`

### Issue: No Kafka messages appear

**Symptom:** Topic exists but no messages after INSERT/UPDATE

**Solution:**
1. Verify publication exists: 
```bash
docker compose -f docker-compose-cdc.yml exec postgres psql -U postgres -d productsdb -c \
  "SELECT * FROM pg_publication;"
```

2. Check replication slot status:
```bash
docker compose -f docker-compose-cdc.yml exec postgres psql -U postgres -d productsdb -c \
  "SELECT * FROM pg_replication_slots;"
```

3. Restart the connector:
```bash
curl -X POST http://localhost:8083/connectors/product-connector/restart
```

### Issue: Spring Cloud Stream cannot deserialize messages

**Symptom:** notification-service logs show deserialization errors

**Solution:**
1. Verify ExtractNewRecordState SMT is applied in `product-connector.json`
2. Check Kafka message format: 
```bash
docker compose -f docker-compose-cdc.yml exec kafka kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic product-service.public.products --max-messages 1
```

3. Ensure DTOs match the flattened schema (no nested `before`/`after`)

### Issue: Port already in use

**Symptom:** Docker container fails to start on port 5432, 9092, 5050, or 8083

**Solution:**
Find and kill existing process or modify port in `docker-compose-cdc.yml`.

## Cleanup

**Stop all services:**

```bash
docker compose -f docker-compose-cdc.yml down
```

**Remove volumes (optional):**

```bash
docker compose -f docker-compose-cdc.yml down -v
```

## Key Concepts

### ExtractNewRecordState SMT

The `transforms.unwrap.type: io.debezium.transforms.ExtractNewRecordState` Single Message Transform:
- **Removes envelope**: Flattens nested `before`/`after` structure
- **Adds metadata**: Includes `__op` (operation) and `__ts_ms` (timestamp)
- **Simplifies deserialization**: DTO receives flat JSON instead of complex nesting
- **`drop.tombstones: true`**: Ignores delete markers

### Spring Cloud Stream Bindings

In `notification-service/src/main/resources/application.yaml`:
```yaml
spring:
  cloud:
    stream:
      bindings:
        processProductCdc-in-0:
          destination: product-service.public.products  # Kafka topic
          group: notification-product-cdc-group        # Consumer group
```

Bean name `processProductCdc()` automatically binds to `processProductCdc-in-0` input channel.

## Files Reference

| File | Purpose |
|------|---------|
| `docker-compose-cdc.yml` | Infrastructure stack definition |
| `initdb/init.sql` | PostgreSQL initialization (products table + publication) |
| `product-connector.json` | Debezium PostgreSQL connector configuration |
| `notification-service/src/main/java/.../dto/event/cdc/*.java` | CDC Event DTOs |
| `notification-service/src/main/java/.../kafka/CdcConsumerConfig.java` | Spring Cloud Stream consumer bean |
| `notification-service/src/main/resources/application.yaml` | Spring Cloud Stream bindings |

## Next Steps

1. ✅ Start Docker stack
2. ✅ Register connector
3. ✅ Test INSERT/UPDATE/DELETE
4. ✅ Verify notification-service console output
5. 📊 Implement database persistence for CDC events (optional)
6. 📧 Send email/SMS notifications (optional)
7. 🔒 Add authentication/authorization (optional)

## Support & Documentation

- Debezium Docs: https://debezium.io/documentation/
- PostgreSQL CDC: https://debezium.io/documentation/reference/stable/connectors/postgresql.html
- Spring Cloud Stream: https://spring.io/projects/spring-cloud-stream
- Kafka Topics: https://kafka.apache.org/


Quick Start (Copy-Paste Ready)

# 1. Start infrastructure
cd d:\Code_projects\microservices-spring-gygy\microservices
docker compose -f docker-compose-cdc.yml up -d

# 2. Wait 30-40 seconds, then register connector
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
  --data @product-connector.json http://localhost:8083/connectors

# 3. Test CREATE operation
docker compose -f docker-compose-cdc.yml exec postgres psql -U postgres -d productsdb -c \
  "INSERT INTO public.products (id, name, price, stock, description) VALUES (2, 'Gaming Laptop', 1299.99, 15, 'High-performance');"

# 4. Test UPDATE operation
docker compose -f docker-compose-cdc.yml exec postgres psql -U postgres -d productsdb -c \
  "UPDATE public.products SET stock = 10 WHERE id = 2;"

When notification-service is running, you'll see:

[CDC FLOW] Yeni Ürün Eklendi! Ürün Adı: Gaming Laptop
[CDC FLOW] Ürün Güncellendi! Yeni Stok: 10

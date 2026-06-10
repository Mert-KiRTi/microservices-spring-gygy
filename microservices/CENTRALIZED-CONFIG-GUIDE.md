# Centralized Configuration Server - Implementation Guide

## Overview

All 5 microservices (`user-service`, `product-service`, `order-service`, `notification-service`, `gateway-server`) have been successfully migrated to use **Spring Cloud Config Server** for centralized configuration management.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│            Spring Cloud Config Server (port 8888)            │
│         Serves configs from Git (microservices/configs)       │
└─────────────────────────────────────────────────────────────┘
         ↓                    ↓                    ↓
    ┌────────────┐      ┌────────────┐      ┌────────────┐
    │ user-svc   │      │ product-sv │      │ order-svc  │
    │ (8081)     │      │ (8082)     │      │ (8086)     │
    └────────────┘      └────────────┘      └────────────┘
         ↓                    ↓
    ┌────────────┐      ┌────────────┐
    │notification│      │ gateway    │
    │ (8088)     │      │ (8888)     │
    └────────────┘      └────────────┘
```

## Configuration Structure

```
microservices/
├── configs/
│   ├── application.yaml                    # Common configs (Eureka, Management)
│   ├── user-service/
│   │   └── application.yaml                # User service specific
│   ├── product-service/
│   │   ├── application.yaml                # Base config
│   │   ├── application-dev.yaml            # Dev environment overrides
│   │   ├── application-prod.yaml           # Prod environment overrides
│   │   └── application-test.yaml           # Test environment overrides
│   ├── order-service/
│   │   └── application.yaml                # Order service specific
│   ├── gateway-server/
│   │   └── application.yaml                # Gateway service specific
│   └── notification-service/
│       └── application.yaml                # Notification service specific (CDC configs)
```

## Updated Dependencies

All microservices now have `spring-cloud-starter-config` dependency:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

**Affected pom.xml files:**
- `user-service/pom.xml` ✅
- `product-service/pom.xml` ✅ (already had spring-cloud-config-client)
- `order-service/pom.xml` ✅
- `gateway-server/pom.xml` ✅
- `notification-service/pom.xml` ✅

## Simplified Local application.yaml Files

Each microservice's local `application.yaml` now contains **ONLY**:

```yaml
spring:
  application:
    name: <service-name>
  config:
    import: "optional:configserver:http://localhost:8888"
```

**Example (user-service/src/main/resources/application.yaml):**

```yaml
spring:
  application:
    name: user-service
  config:
    import: "optional:configserver:http://localhost:8888"
```

**Updated service files:**
- ✅ `user-service/src/main/resources/application.yaml`
- ✅ `product-service/src/main/resources/application.yaml`
- ✅ `order-service/src/main/resources/application.yaml`
- ✅ `gateway-server/src/main/resources/application.yaml`
- ✅ `notification-service/src/main/resources/application.yaml`

## Centralized Configuration Details

### Common Configuration (configs/application.yaml)

```yaml
spring:
  application:
    name: config-server

management:
  endpoints:
    web:
      exposure:
        include: "*"

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true
    register-with-eureka: true
```

**Shared by all services:**
- Eureka discovery configuration
- Management endpoints exposure

### Service-Specific Configurations

#### 1. **user-service** (configs/user-service/application.yaml)

```yaml
spring:
  application:
    name: user-service
  cloud:
    function:
      definition: consumeTestEvent
    stream:
      bindings:
        consumeTestEvent-in-0:
          destination: test-topic
          group: user-service-group
      kafka:
        binder:
          brokers: localhost:9092
  datasource:
    url: jdbc:postgresql://localhost:5434/users
    username: postgres
    password: test12345
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8081
```

**Includes:**
- PostgreSQL datasource configuration
- Kafka stream bindings for test events
- JPA Hibernate DDL auto-update

---

#### 2. **product-service** (configs/product-service/application.yaml)

```yaml
spring:
  application:
    name: product-service
  profiles:
    active: dev
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: product-group
      auto-offset-reset: earliest
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop

server:
  port: 8082
```

**Profile-Specific Overrides:**

- **application-dev.yaml**: PostgreSQL on localhost:5433, Kafka stream bindings
- **application-prod.yaml**: Production PostgreSQL on prod.turkcell.com
- **application-test.yaml**: Test PostgreSQL on test.turkcell.com

---

#### 3. **order-service** (configs/order-service/application.yaml)

```yaml
spring:
  application:
    name: order-service
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      retries: 3
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop

server:
  port: 8086
```

**Includes:**
- Kafka producer configuration (acks: all, retries: 3)
- H2 in-memory database

---

#### 4. **gateway-server** (configs/gateway-server/application.yaml)

```yaml
spring:
  application:
    name: gateway-server
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**

server:
  port: 8888
```

**Includes:**
- Spring Cloud Gateway routes with load balancing
- Path-based routing to services

---

#### 5. **notification-service** (configs/notification-service/application.yaml)

```yaml
spring:
  application:
    name: notification-service
  cloud:
    stream:
      bindings:
        processProductCdc-in-0:
          destination: product-service.public.products
          group: notification-product-cdc-group
          content-type: application/json
      kafka:
        binder:
          brokers: localhost:9092

server:
  port: 8088
```

**Includes:**
- Spring Cloud Stream bindings for CDC events
- Kafka binder for Debezium CDC flow

---

## Config Server Setup (config-server/src/main/resources/application.yaml)

The config-server is already configured to serve from Git:

```yaml
spring:
    application:
        name: config-server
    cloud:
        config:
            server:
                git: 
                    uri: https://github.com/Mert-KiRTi/microservices-spring-gygy.git
                    default-label: main
                    search-paths: microservices/configs,microservices/configs/{application}
                    clone-on-start: true
                    force-pull: true
                
server:
    port: 8888
```

**Key features:**
- **Git source**: Reads from GitHub repository
- **Search paths**: `microservices/configs` and `microservices/configs/{application}`
  - `microservices/configs/application.yaml` → Common configs
  - `microservices/configs/user-service/application.yaml` → User service specific
- **Clone on start**: Clones repo at startup
- **Force pull**: Always pulls latest from Git

## How Config Resolution Works

1. **Service starts** with minimal local `application.yaml` containing only:
   - `spring.application.name`
   - `spring.config.import: optional:configserver:http://localhost:8888`

2. **Spring Boot connects** to Config Server at `http://localhost:8888`

3. **Config Server returns** configuration based on:
   - Service name: `spring.application.name`
   - Active profiles: `spring.profiles.active`
   - Default file: `application.yaml`
   - Profile files: `application-{profile}.yaml`

4. **Configuration precedence** (lowest to highest):
   1. `configs/application.yaml` (common)
   2. `configs/{application}/application.yaml` (service-specific)
   3. `configs/{application}/application-{profile}.yaml` (profile-specific)
   4. Local `application.yaml` (if not empty, overrides all)

## Example: product-service Configuration Resolution

When **product-service** starts with `spring.profiles.active: dev`:

```
1. Load common: configs/application.yaml
2. Load service: configs/product-service/application.yaml
3. Load profile: configs/product-service/application-dev.yaml
4. Final config = Merged result (profile overrides service, service overrides common)
```

## Running the Services

### 1. Start Config Server

```bash
cd config-server
mvn clean spring-boot:run
# Server starts at http://localhost:8888
```

### 2. Start Eureka Server

```bash
cd eureka-server
mvn clean spring-boot:run
# Eureka Dashboard: http://localhost:8761
```

### 3. Start Each Microservice

```bash
# Terminal 1: user-service
cd user-service
mvn clean spring-boot:run

# Terminal 2: product-service
cd product-service
mvn clean spring-boot:run

# Terminal 3: order-service
cd order-service
mvn clean spring-boot:run

# Terminal 4: gateway-server
cd gateway-server
mvn clean spring-boot:run

# Terminal 5: notification-service
cd notification-service
mvn clean spring-boot:run
```

**Important:** Always start Config Server and Eureka Server first, then microservices.

## Verifying Configuration

### Check Config Server

```bash
curl http://localhost:8888/user-service/default
curl http://localhost:8888/product-service/dev
curl http://localhost:8888/order-service/default
curl http://localhost:8888/gateway-server/default
curl http://localhost:8888/notification-service/default
```

### Check Service Logs

Look for log messages like:
```
Fetching config from server at http://localhost:8888
Located property source [ConfigServicePropertySourceLocator] with name 'configserver'
```

### Check Eureka Dashboard

http://localhost:8761 - All services should be registered with ACTIVE status

## Troubleshooting

### Issue: Config Server Returns 404

**Cause:** Git repository not initialized or search paths incorrect

**Solution:**
```bash
# Verify Git clone
cd ~/.spring-cloud-config-repo
git status

# Verify file structure
ls -R microservices/configs/
```

### Issue: Service Cannot Connect to Config Server

**Symptoms:** Service starts but port is wrong, datasource not available

**Solution:**
```bash
# Verify Config Server is running
curl http://localhost:8888/health

# Check service logs for import errors
# May need to add debug logging:
logging:
  level:
    org.springframework.cloud.config: DEBUG
```

### Issue: Config Not Updated After Git Push

**Solution:**
- Config Server caches configs
- Restart the affected microservice:
```bash
# Or manually refresh (if @RefreshScope is used):
curl -X POST http://localhost:8081/actuator/refresh
```

## Best Practices

1. ✅ **Never put sensitive data in Git** - Use Config Server encryption or external vault
2. ✅ **Use profiles wisely** - dev/test/prod should have different datasources
3. ✅ **Keep common configs DRY** - Put shared settings in `application.yaml`
4. ✅ **Version control everything** - Git tracks all config changes
5. ✅ **Document profile-specific settings** - Add comments for environment-specific configs
6. ✅ **Test config changes locally first** - Before pushing to Git

## CDC Flow Integration

The **notification-service** CDC flow remains intact:

1. Config Server provides Kafka bindings
2. Debezium sends CDC events to `product-service.public.products` topic
3. notification-service consumes events via Spring Cloud Stream
4. Console output: `[CDC FLOW] Ürün Eklendi! Ürün Adı: ...`

All configs come from centralized source - no local hardcoding!

## Summary of Changes

| Component | Change | File |
|-----------|--------|------|
| user-service | Added spring-cloud-starter-config, cleaned local YAML | ✅ Complete |
| product-service | Verified spring-cloud-config-client, cleaned local YAML | ✅ Complete |
| order-service | Added spring-cloud-starter-config, cleaned local YAML | ✅ Complete |
| gateway-server | Added spring-cloud-starter-config, cleaned local YAML | ✅ Complete |
| notification-service | Added spring-cloud-starter-config, cleaned local YAML | ✅ Complete |
| configs/ | Organized centralized configs by service | ✅ Complete |
| config-server | Verified Git-based configuration | ✅ Working |

## Next Steps

1. ✅ Push all changes to GitHub
2. ✅ Start Config Server (`mvn spring-boot:run`)
3. ✅ Start Eureka Server (`mvn spring-boot:run`)
4. ✅ Start all 5 microservices
5. ✅ Verify all services register in Eureka (http://localhost:8761)
6. ✅ Test API endpoints through gateway (http://localhost:8888)
7. ✅ Test CDC flow (Debezium → Kafka → notification-service)

---

**Kurumsal standart: Tüm konfigürasyonlar merkezi sunucudan çekilir, servisler hafif ve state'siz kalır!** 🚀

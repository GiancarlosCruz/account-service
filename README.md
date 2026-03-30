# Account Service

## Descripción

Este es un microservicio de gestión de cuentas bancarias desarrollado con Spring Boot 3.x y WebFlux. Forma parte del sistema bancario "BancarySystem" y permite la creación, consulta y gestión de cuentas bancarias con reglas de negocio específicas según el tipo de cliente.

## Tecnologías Utilizadas

- **Java 17**
- **Spring Boot 3.2.5**
- **Spring WebFlux** (programación reactiva)
- **Spring Data MongoDB Reactive** (base de datos MongoDB reactiva)
- **Spring Cloud Config** (configuración centralizada)
- **Netflix Eureka Client** (descubrimiento de servicios)
- **Resilience4j** (patrones de resiliencia: circuit breaker, retry, etc.)
- **SpringDoc OpenAPI** (documentación de API)
- **Lombok** (reducción de código boilerplate)
- **Docker** (containerización)

## Arquitectura

El microservicio sigue una arquitectura hexagonal con las siguientes capas:

- **Controller**: Exposición de endpoints REST
- **Service**: Lógica de negocio
- **Repository**: Acceso a datos con MongoDB
- **Client**: Comunicación con otros microservicios (Customer, Card)
- **Mapper**: Transformación de objetos
- **Exception**: Manejo de excepciones personalizadas
- **Config**: Configuraciones de WebClient y otros beans

## Reglas de Negocio

### Tipos de Cliente

1. **PERSONAL**:
   - Máximo 1 cuenta de AHORRO
   - Máximo 1 cuenta CORRIENTE
   - Ilimitadas cuentas de PLAZO_FIJO

2. **EMPRESARIAL**:
   - Solo cuentas CORRIENTE
   - Múltiples cuentas permitidas

## Prerrequisitos

- Java 17
- Maven 3.6+
- MongoDB
- Config Server (puerto 8888)
- Eureka Server
- Docker (opcional)

## Configuración

El servicio utiliza Spring Cloud Config para la configuración centralizada. El archivo `application.yaml` importa configuraciones desde `http://localhost:8888`.

Variables de entorno importantes:
- `SPRING_PROFILES_ACTIVE`: Perfil activo (default, dev, prod)
- `JAVA_OPTS`: Opciones JVM

## Ejecución

### Local

1. Asegúrate de que MongoDB esté corriendo
2. Inicia el Config Server en puerto 8888
3. Inicia el Eureka Server
4. Ejecuta el servicio:

```bash
mvn spring-boot:run
```

### Con Docker

1. Construye la imagen:

```bash
mvn clean package
docker build -t accountservice .
```

2. Ejecuta el contenedor:

```bash
docker run -p 8082:8082 accountservice
```

## API

La API está documentada con OpenAPI 3.0 y disponible en:
- Swagger UI: `http://localhost:8082/webjars/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

### Endpoints Principales

- `GET /accounts`: Lista todas las cuentas activas
- `POST /accounts`: Crea una nueva cuenta
- `GET /accounts/{id}`: Obtiene una cuenta por ID
- `PUT /accounts/{id}`: Actualiza una cuenta
- `DELETE /accounts/{id}`: Elimina una cuenta (lógicamente)

## Monitoreo

El servicio incluye Spring Boot Actuator para monitoreo:
- Health: `http://localhost:8082/actuator/health`
- Metrics: `http://localhost:8082/actuator/metrics`
- Info: `http://localhost:8082/actuator/info`

## Pruebas

Ejecuta las pruebas con:

```bash
mvn test
```

Incluye pruebas unitarias y de integración con MongoDB embebido.

## Despliegue

El servicio está preparado para despliegue en contenedores Docker y puede integrarse con Kubernetes u otros orquestadores.

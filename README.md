# MPCT Backend

**Última Actualización:** 2026-07-21

Backend principal del proyecto MPCT, construido con Spring Boot 3 y Java 21. Esta API REST proporciona servicios de autenticación, integración con pasarelas de pago, generación de documentos (PDF/QR) y gestión de datos.

## 🏗️ Arquitectura y Tecnologías

El proyecto sigue una arquitectura por capas (N-Tier Architecture), asegurando una separación clara entre los controladores, la lógica de negocio y el acceso a datos.

**Stack Tecnológico:**
- **Core:** Java 21, Spring Boot 3.2.5
- **Base de Datos:** PostgreSQL (Spring Data JPA / Hibernate)
- **Caché:** Redis (Spring Data Redis)
- **Seguridad:** Spring Security, JWT (jjwt)
- **Generación de Documentos:** OpenPDF, Flying Saucer (PDFs HTML-to-PDF vía Thymeleaf), ZXing (Códigos QR)
- **Pagos:** Mercado Pago SDK (v2.1.26)
- **Rate Limiting:** Bucket4j
- **Documentación API:** OpenAPI / Swagger (springdoc)
- **Testing:** JUnit 5, Mockito, Spring Boot Test

## 📁 Estructura del Proyecto (Codemap)

```text
src/main/java/com/example/mpct
├── api/          # Controladores REST (Endpoints)
├── config/       # Configuraciones (Seguridad, Redis, Swagger, CORS)
├── dto/          # Data Transfer Objects para request/response
├── exception/    # Manejadores de excepciones globales (ControllerAdvice)
├── model/        # Entidades JPA (Dominio)
├── repository/   # Interfaces de Spring Data JPA
├── security/     # Filtros de JWT, UserDetails y Auth providers
├── service/      # Lógica de negocio e integraciones externas
└── util/         # Clases utilitarias (generación de QR, fechas, etc.)
```

## 🚀 Requisitos Previos

- **Java 21** (JDK)
- **PostgreSQL** 15+
- **Redis Server** (para gestión de caché y Rate Limiting)
- Docker y Docker Compose (opcional, para despliegue rápido)

## 🛠️ Configuración y Ejecución local

1. **Base de Datos y Configuración:**
   Asegúrate de configurar tus credenciales en `src/main/resources/application.properties` (o el equivalente `.yml`). Necesitarás variables para:
   - Conexión a PostgreSQL (`spring.datasource.*`)
   - Conexión a Redis (`spring.data.redis.*`)
   - Secreto de firma JWT
   - Token de Acceso de Mercado Pago (`mercadopago.access.token`)

2. **Ejecutar usando el Maven Wrapper:**
   
   En Windows:
   ```cmd
   .\mvnw.cmd spring-boot:run
   ```

   En macOS/Linux:
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Acceder a la Documentación de la API:**
   Una vez que el servidor inicie, visita la interfaz interactiva de Swagger:
   `http://localhost:8080/swagger-ui.html` (El puerto puede variar según tu configuración).

## 🐳 Docker y Despliegue

El repositorio incluye un `Dockerfile` y un `docker-compose.yml`. Puedes orquestar la base de datos, Redis y la aplicación de Spring Boot en contenedores utilizando:

```bash
docker-compose up --build -d
```

## 🔒 Seguridad y Manejo de Peticiones

- **Autenticación (JWT):** Los endpoints privados requieren un token Bearer en la cabecera `Authorization`.
- **Rate Limiting:** Se utiliza `Bucket4j` en conjunto con la API para proteger los endpoints de abusos o ataques DDoS a nivel de aplicación.

## 💳 Mercado Pago

Para la pasarela de pagos se utiliza el SDK nativo de Java. Recuerda configurar correctamente la URL base en el portal de desarrolladores de Mercado Pago para la recepción de Webhooks.


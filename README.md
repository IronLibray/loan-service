# 📚 Loan Service - Iron Library

> Microservicio de gestión de préstamos y devoluciones para el sistema Iron Library

## 🎯 Descripción

Este microservicio forma parte de la arquitectura distribuida de Iron Library y se encarga de **gestionar todo el ciclo de vida de los préstamos de libros**. Coordina la comunicación entre usuarios y libros, controla las reglas de negocio, fechas de vencimiento, extensiones, y estadísticas de préstamos. Es el corazón de las operaciones bibliotecarias del sistema.

## 🚀 Características

- ✅ **CRUD completo** de préstamos con validaciones
- ✅ **Integración con microservicios** (User Service y Book Service)
- ✅ **Gestión de estados** (activo, devuelto, vencido, cancelado)
- ✅ **Control de fechas** y vencimientos automáticos
- ✅ **Extensiones de préstamos** con límites configurables
- ✅ **Validaciones de negocio** (límites por usuario, disponibilidad)
- ✅ **Estadísticas avanzadas** de préstamos
- ✅ **Búsquedas especializadas** (vencidos, próximos a vencer)
- ✅ **API REST** documentada con endpoints específicos
- ✅ **Comunicación con Feign Clients** para validaciones

## 🛠️ Stack Tecnológico

- **Spring Boot** 3.4.6
- **Spring Data JPA** - Persistencia de datos
- **Spring Web** - API REST
- **Spring Cloud Netflix Eureka Client** - Service Discovery
- **Spring Cloud OpenFeign** - Comunicación entre servicios
- **MySQL** - Base de datos relacional
- **Bean Validation** - Validaciones de entrada
- **Lombok** - Reducción de código boilerplate
- **JUnit 5** - Testing unitario
- **Mockito** - Mocking para tests

## 📡 Endpoints Principales

### Base URL: `http://localhost:8083/api/loans`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/loans` | Obtener todos los préstamos |
| GET | `/api/loans/{id}` | Obtener préstamo por ID |
| GET | `/api/loans/user/{userId}` | Obtener préstamos por usuario |
| GET | `/api/loans/user/{userId}/active` | Préstamos activos de un usuario |
| GET | `/api/loans/book/{bookId}` | Obtener préstamos por libro |
| GET | `/api/loans/overdue` | Obtener préstamos vencidos |
| GET | `/api/loans/due-soon?days=3` | Préstamos que vencen pronto |
| GET | `/api/loans/stats` | Estadísticas de préstamos |
| POST | `/api/loans` | Crear nuevo préstamo |
| POST | `/api/loans/quick?userId=1&bookId=1` | Crear préstamo rápido |
| PUT | `/api/loans/{id}` | Actualizar préstamo completo |
| PATCH | `/api/loans/{id}/return` | Devolver libro |
| PATCH | `/api/loans/{id}/extend?days=7` | Extender préstamo |
| DELETE | `/api/loans/{id}` | Eliminar préstamo |
| GET | `/api/loans/health` | Health check del servicio |

## 📊 Modelo de Datos

### Entidad Principal: Loan
```java
@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "book_id", nullable = false)
    private Long bookId;
    
    @Column(name = "loan_date", nullable = false)
    private LocalDate loanDate;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "return_date")
    private LocalDate returnDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;
    
    @Column(length = 500)
    private String notes;
    
    // Métodos de negocio
    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE && 
               LocalDate.now().isAfter(dueDate);
    }
    
    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return LocalDate.now().toEpochDay() - dueDate.toEpochDay();
    }
    
    public boolean canBeReturned() {
        return status == LoanStatus.ACTIVE || status == LoanStatus.OVERDUE;
    }
}
```

### Enum LoanStatus
```java
public enum LoanStatus {
    ACTIVE("Activo", "El libro está prestado"),
    RETURNED("Devuelto", "El libro ha sido devuelto"),
    OVERDUE("Vencido", "El préstamo está vencido"),
    CANCELLED("Cancelado", "El préstamo fue cancelado");
    
    private final String displayName;
    private final String description;
    
    public boolean allowsReturn() {
        return this == ACTIVE || this == OVERDUE;
    }
    
    public boolean isCompleted() {
        return this == RETURNED || this == CANCELLED;
    }
}
```

## 🔧 Configuración

### Variables de Entorno
```properties
# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/loan_service
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update

# Puerto del servicio
server.port=8083

# Eureka
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/
spring.application.name=loan-service
```

### Configuración de Base de Datos
```sql
CREATE DATABASE loan_service;
USE loan_service;

-- La tabla se crea automáticamente por JPA
-- Estructura resultante:
-- loans (id, user_id, book_id, loan_date, due_date, return_date, status, notes)
```

## 🚀 Instalación y Ejecución

### Prerrequisitos
- Java 21
- Maven 3.6+
- MySQL 8.0+
- Discovery Server ejecutándose en puerto 8761
- User Service ejecutándose en puerto 8082
- Book Service ejecutándose en puerto 8081

### Pasos de Instalación
```bash
# Clonar el repositorio
git clone https://github.com/IronLibray/loan-service.git
cd loan-service

# Configurar base de datos
mysql -u root -p -e "CREATE DATABASE loan_service;"

# Instalar dependencias
./mvnw clean install

# Ejecutar el servicio
./mvnw spring-boot:run
```

### Verificar Instalación
```bash
# Health check
curl http://localhost:8083/api/loans/health

# Verificar registro en Eureka
# Ir a http://localhost:8761 y verificar que aparece LOAN-SERVICE
```

## 🧪 Testing

### Ejecutar Tests
```bash
# Todos los tests
./mvnw test

# Solo tests unitarios
./mvnw test -Dtest="*Test"

# Solo tests de integración
./mvnw test -Dtest="*IntegrationTest"
```

### Cobertura de Tests
- ✅ **LoanController** - Tests con MockMvc (@WebMvcTest)
- ✅ **LoanService** - Tests unitarios con @Mock
- ✅ **LoanRepository** - Tests de integración con @DataJpaTest
- ✅ **Feign Clients** - Tests de comunicación
- ✅ **Validaciones** - Tests de reglas de negocio

## 🔗 Comunicación con Otros Servicios

### Feign Clients Configurados

#### UserServiceClient
```java
@FeignClient(name = "user-service")
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
    
    @GetMapping("/api/users/{id}/validate")
    Boolean validateUser(@PathVariable("id") Long id);
}
```

#### BookServiceClient
```java
@FeignClient(name = "book-service")
public interface BookServiceClient {
    @GetMapping("/api/books/{id}")
    BookDto getBookById(@PathVariable("id") Long id);
    
    @GetMapping("/api/books/{id}/available")
    Boolean isBookAvailable(@PathVariable("id") Long id);
    
    @PutMapping("/api/books/{id}/availability")
    void updateAvailability(@PathVariable("id") Long id, 
                           @RequestParam("copies") int copies);
}
```

### DTOs para Comunicación
```java
// UserDto - Información del usuario
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String membershipType;
    private Boolean isActive;
    
    public int getMaxBooksAllowed() {
        return switch (membershipType) {
            case "BASIC" -> 3;
            case "PREMIUM" -> 10;
            case "STUDENT" -> 5;
            default -> 0;
        };
    }
    
    public int getLoanDurationDays() {
        return switch (membershipType) {
            case "BASIC" -> 14;
            case "PREMIUM" -> 30;
            case "STUDENT" -> 21;
            default -> 14;
        };
    }
}

// BookDto - Información del libro
public class BookDto {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private Integer availableCopies;
    
    public boolean isAvailable() {
        return availableCopies != null && availableCopies > 0;
    }
}
```

## 📈 Lógica de Negocio

### Reglas de Préstamo
| Validación | Descripción |
|------------|-------------|
| **Usuario activo** | El usuario debe estar activo y validado |
| **Usuario no exceda límite** | Según membresía (BASIC: 3, PREMIUM: 10, STUDENT: 5) |
| **Libro disponible** | Debe haber copias disponibles |
| **No duplicar préstamo** | Usuario no puede tener el mismo libro prestado |
| **Duración por membresía** | BASIC: 14 días, PREMIUM: 30 días, STUDENT: 21 días |

### Flujo de Creación de Préstamo
1. **Validar usuario** → Verificar estado activo y límites
2. **Validar libro** → Verificar disponibilidad
3. **Verificar límites** → Comprobar préstamos activos vs máximo permitido
4. **Verificar duplicados** → Usuario no puede tener el mismo libro
5. **Crear préstamo** → Establecer fechas según membresía
6. **Actualizar inventario** → Reducir copias disponibles del libro
7. **Guardar registro** → Persistir en base de datos

### Flujo de Devolución
1. **Buscar préstamo** → Verificar que existe y puede devolverse
2. **Actualizar estado** → Cambiar a RETURNED y establecer fecha
3. **Restaurar inventario** → Aumentar copias disponibles del libro
4. **Guardar cambios** → Persistir actualización

### Estados de Préstamo
- **ACTIVE** → Libro prestado, dentro del plazo
- **OVERDUE** → Libro prestado, fuera del plazo (se actualiza automáticamente)
- **RETURNED** → Libro devuelto exitosamente
- **CANCELLED** → Préstamo cancelado por administrador

## 📚 Documentación API

### Crear Préstamo Completo
```bash
curl -X POST http://localhost:8083/api/loans \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "bookId": 1,
    "notes": "Préstamo para estudio de arquitectura"
  }'
```

### Crear Préstamo Rápido
```bash
curl -X POST "http://localhost:8083/api/loans/quick?userId=1&bookId=1"
```

### Respuesta Exitosa
```json
{
  "id": 1,
  "userId": 1,
  "bookId": 1,
  "loanDate": "2025-01-27",
  "dueDate": "2025-02-26",
  "returnDate": null,
  "status": "ACTIVE",
  "notes": "Préstamo para estudio de arquitectura"
}
```

### Devolver Libro
```bash
curl -X PATCH http://localhost:8083/api/loans/1/return
```

### Extender Préstamo
```bash
curl -X PATCH "http://localhost:8083/api/loans/1/extend?days=7"
```

### Obtener Préstamos Vencidos
```bash
curl http://localhost:8083/api/loans/overdue
```

### Obtener Estadísticas
```bash
curl http://localhost:8083/api/loans/stats
```

### Respuesta de Estadísticas
```json
{
  "totalLoans": 500,
  "activeLoans": 120,
  "overdueLoans": 15,
  "returnedLoans": 365
}
```

### Buscar Préstamos por Usuario
```bash
curl http://localhost:8083/api/loans/user/1
curl http://localhost:8083/api/loans/user/1/active
```

### Buscar Préstamos que Vencen Pronto
```bash
curl "http://localhost:8083/api/loans/due-soon?days=3"
```

## 🔒 Validaciones y Excepciones

### Validaciones de Entrada
- **UserId**: Debe existir y ser válido
- **BookId**: Debe existir y estar disponible
- **Notes**: Opcional, máximo 500 caracteres
- **Days (extensión)**: Entre 1 y 30 días

### Excepciones Personalizadas
```java
// Usuario no válido para préstamos
public class UserNotValidException extends RuntimeException

// Libro no disponible
public class BookNotAvailableException extends RuntimeException

// Préstamo no encontrado
public class LoanNotFoundException extends RuntimeException

// Préstamo ya devuelto
public class LoanAlreadyReturnedException extends RuntimeException
```

### Manejo de Errores
- **400 Bad Request**: Datos de entrada inválidos
- **403 Forbidden**: Usuario no puede pedir prestado
- **404 Not Found**: Préstamo/Usuario/Libro no encontrado
- **409 Conflict**: Libro no disponible o ya prestado al usuario
- **503 Service Unavailable**: Error comunicando con otros servicios

## 🔍 Búsquedas Avanzadas

### Repositorio Especializado
```java
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    // Préstamos por usuario
    List<Loan> findByUserId(Long userId);
    
    // Préstamos activos por usuario
    List<Loan> findByUserIdAndStatus(Long userId, LoanStatus status);
    
    // Préstamos vencidos
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate < CURRENT_DATE")
    List<Loan> findOverdueLoans();
    
    // Préstamos que vencen pronto
    @Query("SELECT l FROM Loan l WHERE l.status = 'ACTIVE' AND l.dueDate BETWEEN CURRENT_DATE AND :endDate")
    List<Loan> findLoansDueSoon(@Param("endDate") LocalDate endDate);
    
    // Contar préstamos activos por usuario
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.userId = :userId AND l.status = 'ACTIVE'")
    Long countActiveLoansForUser(@Param("userId") Long userId);
    
    // Verificar si usuario tiene préstamo activo del libro
    @Query("SELECT COUNT(l) > 0 FROM Loan l WHERE l.userId = :userId AND l.bookId = :bookId AND l.status = 'ACTIVE'")
    boolean hasActiveLoanForBook(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
```

### Consultas Especializadas
```bash
# Préstamos por rango de fechas
GET /api/loans?startDate=2025-01-01&endDate=2025-01-31

# Préstamos por estado
GET /api/loans?status=OVERDUE

# Préstamos de un libro específico
GET /api/loans/book/1

# Estadísticas por período
GET /api/loans/stats?month=1&year=2025
```

## 🚀 Características Avanzadas

### Actualización Automática de Estados
```java
@Scheduled(fixedRate = 3600000) // Cada hora
public void updateOverdueLoans() {
    List<Loan> overdueLoans = loanRepository.findOverdueLoans();
    overdueLoans.forEach(loan -> {
        if (loan.getStatus() == LoanStatus.ACTIVE) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);
        }
    });
}
```

### Extensiones con Límites
```java
public Loan extendLoan(Long loanId, int days) {
    // Validaciones
    if (days <= 0 || days > 30) {
        throw new IllegalArgumentException("Los días deben estar entre 1 y 30");
    }
    
    Loan loan = findLoanById(loanId);
    if (loan.getStatus() != LoanStatus.ACTIVE) {
        throw new IllegalArgumentException("Solo se pueden extender préstamos activos");
    }
    
    // Extender fecha
    loan.setDueDate(loan.getDueDate().plusDays(days));
    return loanRepository.save(loan);
}
```

### Integración con Circuit Breaker (Futura)
```java
@CircuitBreaker(name = "user-service", fallbackMethod = "fallbackValidateUser")
public UserDto validateUser(Long userId) {
    return userServiceClient.getUserById(userId);
}

public UserDto fallbackValidateUser(Long userId, Exception ex) {
    // Implementar lógica de fallback
    throw new UserNotValidException("Servicio de usuarios no disponible");
}
```

## 📊 Métricas y Monitoreo

### Métricas de Negocio
- Número total de préstamos
- Préstamos activos vs devueltos
- Préstamos vencidos
- Tiempo promedio de préstamo
- Libros más prestados
- Usuarios más activos

### Health Checks
```bash
# Health check básico
curl http://localhost:8083/api/loans/health

# Health check con dependencias
curl http://localhost:8083/actuator/health
```

### Logging Estructurado
```java
@Slf4j
public class LoanService {
    
    public Loan createLoan(Long userId, Long bookId, String notes) {
        log.info("Creando préstamo - Usuario: {}, Libro: {}", userId, bookId);
        
        try {
            // Lógica de negocio
            log.info("Préstamo creado exitosamente con ID: {}", savedLoan.getId());
            return savedLoan;
        } catch (Exception e) {
            log.error("Error creando préstamo - Usuario: {}, Libro: {}, Error: {}", 
                     userId, bookId, e.getMessage());
            throw e;
        }
    }
}
```

## 🛠️ Troubleshooting

### Problemas Comunes

**Error: "Usuario no puede pedir prestado"**
```bash
# Verificar estado del usuario
curl http://localhost:8082/api/users/1/validate

# Verificar préstamos activos del usuario
curl http://localhost:8083/api/loans/user/1/active
```

**Error: "Libro no disponible"**
```bash
# Verificar disponibilidad del libro
curl http://localhost:8081/api/books/1/available

# Verificar inventario del libro
curl http://localhost:8081/api/books/1
```

**Error de comunicación con servicios**
```bash
# Verificar servicios en Eureka
curl http://localhost:8761/eureka/apps

# Verificar conectividad directa
curl http://localhost:8082/api/users/health
curl http://localhost:8081/api/books/health
```

**Préstamos no se actualizan a OVERDUE**
- Verificar configuración de zona horaria
- Confirmar que el scheduler está habilitado
- Revisar logs para errores en actualización automática

## 🚀 Próximas Mejoras

- [ ] **Notificaciones** - Alertas por email/SMS para vencimientos
- [ ] **Reservas** - Sistema de reservas cuando no hay copias
- [ ] **Multas** - Cálculo automático de penalizaciones
- [ ] **Renovaciones automáticas** - Para usuarios premium
- [ ] **Integración con calendario** - Eventos de vencimiento
- [ ] **Análisis predictivo** - ML para predecir demanda
- [ ] **API de terceros** - Integración con sistemas bibliotecarios
- [ ] **Audit Trail** - Tracking completo de cambios
- [ ] **Bulk operations** - Operaciones masivas de préstamos

# 📚 Loan Service - Iron Library

> Microservicio de gestión de préstamos y devoluciones para el sistema Iron Library

## 🎯 Descripción

Este microservicio forma parte de la arquitectura distribuida de Iron Library y se encarga de **gestionar todo el ciclo de vida de los préstamos de libros**. Coordina la comunicación entre usuarios y libros, controla las reglas de negocio, fechas de vencimiento, extensiones, y estadísticas de préstamos. Es el corazón de las operaciones bibliotecarias del sistema.

## 🚀 Características

- ✅ **CRUD completo** de préstamos con validaciones robustas
- ✅ **Integración con microservicios** (User Service y Book Service)
- ✅ **Gestión de estados** (activo, devuelto, vencido, cancelado)
- ✅ **Control de fechas** y vencimientos automáticos
- ✅ **Extensiones de préstamos** con límites configurables
- ✅ **Validaciones de negocio** (límites por usuario, disponibilidad)
- ✅ **Estadísticas avanzadas** de préstamos
- ✅ **Búsquedas especializadas** (vencidos, próximos a vencer)
- ✅ **API REST** documentada con endpoints específicos
- ✅ **Comunicación con Feign Clients** para validaciones
- ✅ **Manejo de excepciones** centralizado
- ✅ **Testing comprehensivo** con múltiples estrategias

## 🛠️ Stack Tecnológico

- **Spring Boot** 3.4.6
- **Spring Data JPA** - Persistencia de datos
- **Spring Web** - API REST
- **Spring Cloud Netflix Eureka Client** - Service Discovery
- **Spring Cloud OpenFeign** - Comunicación entre servicios
- **MySQL** - Base de datos relacional
- **H2** - Base de datos en memoria para testing
- **TestContainers** - Tests de integración con contenedores
- **Lombok** - Reducción de código boilerplate
- **JUnit 5** - Testing unitario moderno
- **Mockito** - Mocking avanzado para tests

## 📡 Endpoints Principales

### Base URL: `http://localhost:8083/api/loans`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| **GET** | `/api/loans` | Obtener todos los préstamos |
| **GET** | `/api/loans/{id}` | Obtener préstamo por ID |
| **GET** | `/api/loans/user/{userId}` | Obtener préstamos por usuario |
| **GET** | `/api/loans/user/{userId}/active` | Préstamos activos de un usuario |
| **GET** | `/api/loans/book/{bookId}` | Obtener préstamos por libro |
| **GET** | `/api/loans/overdue` | Obtener préstamos vencidos |
| **GET** | `/api/loans/due-soon?days=3` | Préstamos que vencen pronto |
| **GET** | `/api/loans/stats` | Estadísticas de préstamos |
| **POST** | `/api/loans` | Crear nuevo préstamo |
| **POST** | `/api/loans/quick?userId=1&bookId=1` | Crear préstamo rápido |
| **PUT** | `/api/loans/{id}` | Actualizar préstamo completo |
| **PATCH** | `/api/loans/{id}/return` | Devolver libro |
| **PATCH** | `/api/loans/{id}/extend?days=7` | Extender préstamo |
| **DELETE** | `/api/loans/{id}` | Eliminar préstamo |
| **GET** | `/api/loans/health` | Health check del servicio |

## 📊 Modelo de Datos

### Entidad Principal: Loan
```java
@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @Column(name = "status", nullable = false, length = 20)
    private LoanStatus status;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    // Métodos de negocio
    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE && 
               dueDate != null &&
               LocalDate.now().isAfter(dueDate);
    }
    
    public long getDaysOverdue() {
        if (!isOverdue() || dueDate == null) return 0;
        return LocalDate.now().toEpochDay() - dueDate.toEpochDay();
    }
    
    public boolean canBeReturned() {
        return status == LoanStatus.ACTIVE || status == LoanStatus.OVERDUE;
    }
    
    public long getLoanDurationDays() {
        if (dueDate == null || loanDate == null) return 0;
        return dueDate.toEpochDay() - loanDate.toEpochDay();
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
# Aplicación
spring.application.name=loan-service
server.port=8083

# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/loan_service
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update

# Eureka Service Discovery
eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/
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
git clone https://github.com/IronLibrary/loan-service.git
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

# Probar endpoint básico
curl http://localhost:8083/api/loans
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

# Tests end-to-end
./mvnw test -Dtest="*E2ETest"

# Tests con perfiles específicos
./mvnw test -Dspring.profiles.active=test
```

### Cobertura de Tests
- ✅ **LoanController** - Tests con MockMvc (@WebMvcTest)
- ✅ **LoanService** - Tests unitarios con @Mock
- ✅ **LoanRepository** - Tests de integración con @DataJpaTest
- ✅ **Feign Clients** - Tests de comunicación entre servicios
- ✅ **Integration Tests** - Tests de integración completos
- ✅ **E2E Tests** - Tests end-to-end con configuración manual
- ✅ **Business Logic** - Tests de reglas de negocio

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
@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private String membershipType;
    private Boolean isActive;
    private LocalDate registrationDate;
    private String phone;
    private String address;
    
    public boolean canBorrowBooks() {
        return isActive != null && isActive && membershipType != null;
    }
    
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
@Data
public class BookDto {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private Integer totalCopies;
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
```java
public Loan createLoan(Long userId, Long bookId, String notes) {
    // 1. Validar usuario
    UserDto user = validateUser(userId);
    
    // 2. Validar libro
    BookDto book = validateBook(bookId);
    
    // 3. Verificar límites del usuario
    validateUserLimits(userId, user);
    
    // 4. Verificar que no tenga ya este libro
    if (loanRepository.hasActiveLoanForBook(userId, bookId)) {
        throw new IllegalArgumentException("El usuario ya tiene este libro prestado");
    }
    
    // 5. Crear el préstamo
    LocalDate loanDate = LocalDate.now();
    LocalDate dueDate = loanDate.plusDays(user.getLoanDurationDays());
    Loan loan = new Loan(userId, bookId, loanDate, dueDate, notes);
    
    // 6. Actualizar disponibilidad del libro
    bookServiceClient.updateAvailability(bookId, -1);
    
    // 7. Guardar y retornar
    return loanRepository.save(loan);
}
```

### Flujo de Devolución
```java
public Loan returnBook(Long loanId) {
    Loan loan = findLoanById(loanId);
    
    // Verificar que puede devolverse
    if (!loan.canBeReturned()) {
        throw new LoanAlreadyReturnedException("El préstamo ya fue devuelto o cancelado");
    }
    
    // Actualizar el préstamo
    loan.setReturnDate(LocalDate.now());
    loan.setStatus(LoanStatus.RETURNED);
    
    // Restaurar inventario del libro
    bookServiceClient.updateAvailability(loan.getBookId(), 1);
    
    return loanRepository.save(loan);
}
```

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
  "loanDate": "2025-01-29",
  "dueDate": "2025-02-28",
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

### Consultas Especializadas
```bash
# Préstamos vencidos
curl http://localhost:8083/api/loans/overdue

# Préstamos que vencen pronto
curl "http://localhost:8083/api/loans/due-soon?days=3"

# Préstamos por usuario
curl http://localhost:8083/api/loans/user/1
curl http://localhost:8083/api/loans/user/1/active

# Préstamos por libro
curl http://localhost:8083/api/loans/book/1
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

## 🔒 Validaciones y Manejo de Errores

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

### Manejo de Errores HTTP
- **400 Bad Request**: Datos de entrada inválidos
- **403 Forbidden**: Usuario no puede pedir prestado
- **404 Not Found**: Préstamo/Usuario/Libro no encontrado
- **409 Conflict**: Libro no disponible o ya prestado al usuario
- **503 Service Unavailable**: Error comunicando con otros servicios

### Manejo de Errores de Feign
```java
@ExceptionHandler(FeignException.class)
public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex, WebRequest request) {
    String message = "Error comunicando con otro servicio";
    HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
    
    if (ex.status() == 404) {
        message = "Recurso no encontrado en servicio externo";
        status = HttpStatus.NOT_FOUND;
    } else if (ex.status() == 409) {
        message = "Conflicto con recurso en servicio externo";
        status = HttpStatus.CONFLICT;
    }
    
    ErrorResponse error = new ErrorResponse(
        status.value(), message, LocalDateTime.now(),
        request.getDescription(false).replace("uri=", "")
    );
    return ResponseEntity.status(status).body(error);
}
```

## 🔍 Búsquedas Avanzadas

### Repositorio Especializado
```java
@Repository
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
    
    // Estadísticas agrupadas por estado
    @Query("SELECT l.status, COUNT(l) FROM Loan l GROUP BY l.status")
    List<Object[]> getLoanStatistics();
}
```

### Consultas Dinámicas
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
    log.info("Actualizados {} préstamos vencidos", overdueLoans.size());
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

### Servicios de Estadísticas
```java
public LoanStatistics getLoanStatistics() {
    List<Object[]> stats = loanRepository.getLoanStatistics();
    Map<LoanStatus, Long> statusCounts = stats.stream()
            .collect(Collectors.toMap(
                    row -> (LoanStatus) row[0],
                    row -> (Long) row[1]
            ));
    
    long totalLoans = loanRepository.count();
    long activeLoans = statusCounts.getOrDefault(LoanStatus.ACTIVE, 0L);
    long overdueLoans = statusCounts.getOrDefault(LoanStatus.OVERDUE, 0L);
    long returnedLoans = statusCounts.getOrDefault(LoanStatus.RETURNED, 0L);
    
    return new LoanStatistics(totalLoans, activeLoans, overdueLoans, returnedLoans);
}
```

## 📊 Monitoreo y Observabilidad

### Health Check
```bash
curl http://localhost:8083/api/loans/health
# Respuesta: "Loan Service is running on port 8083"
```

### Logging Estructurado
```java
@Slf4j
public class LoanService {
    
    public Loan createLoan(Long userId, Long bookId, String notes) {
        log.info("Creando préstamo - Usuario: {}, Libro: {}", userId, bookId);
        
        try {
            Loan savedLoan = loanRepository.save(loan);
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

### Métricas de Negocio
- Número total de préstamos
- Préstamos activos vs devueltos
- Préstamos vencidos
- Tiempo promedio de préstamo
- Libros más prestados
- Usuarios más activos

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

### Configuración de Timeouts
```properties
# application.properties
spring.cloud.openfeign.client.config.default.connect-timeout=5000
spring.cloud.openfeign.client.config.default.read-timeout=5000

# Para testing
spring.cloud.openfeign.client.config.default.connect-timeout=1000
spring.cloud.openfeign.client.config.default.read-timeout=1000
eureka.client.enabled=false
```

## 🧪 Estrategias de Testing

### Testing Unitario
```java
@ExtendWith(MockitoExtension.class)
class LoanServiceTest {
    
    @Mock
    private LoanRepository loanRepository;
    
    @Mock
    private BookServiceClient bookServiceClient;
    
    @Mock
    private UserServiceClient userServiceClient;
    
    @InjectMocks
    private LoanService loanService;
    
    @Test
    void createLoan_ShouldCreateLoan_WhenValidData() {
        // Given
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(userServiceClient.validateUser(1L)).thenReturn(true);
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(bookServiceClient.isBookAvailable(1L)).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        
        // When
        Loan result = loanService.createLoan(1L, 1L);
        
        // Then
        assertNotNull(result);
        verify(bookServiceClient).updateAvailability(1L, -1);
        verify(loanRepository).save(any(Loan.class));
    }
}
```

### Testing de Integración
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanServiceIntegrationTest {
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Test
    void loanRepository_ShouldPersistAndRetrieveLoans() {
        // Given
        Loan loan = createTestLoan(1L, 101L, "Préstamo de integración", LoanStatus.ACTIVE);
        
        // When
        Loan savedLoan = loanRepository.save(loan);
        
        // Then
        assertNotNull(savedLoan.getId());
        assertEquals(LoanStatus.ACTIVE, savedLoan.getStatus());
        
        // Verificar que se puede recuperar
        Loan retrievedLoan = loanRepository.findById(savedLoan.getId()).orElseThrow();
        assertEquals(savedLoan.getUserId(), retrievedLoan.getUserId());
    }
}
```

### Testing End-to-End
```java
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class LoanServiceE2ETest {
    
    @Mock
    private LoanService loanService;
    
    private MockMvc mockMvc;
    private LoanController loanController;
    
    @BeforeEach
    void setUp() {
        loanController = new LoanController(loanService);
        mockMvc = MockMvcBuilders.standaloneSetup(loanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
    
    @Test
    void createLoan_FullFlow_ShouldWork() throws Exception {
        // Given
        when(loanService.createLoan(1L, 1L, "Test")).thenReturn(testLoan);
        
        // When & Then
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.bookId").value(1));
    }
}
```

## 📝 Notas de Desarrollo

### Configuraciones de Testing
```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
spring.cloud.openfeign.client.config.default.connect-timeout=1000
spring.cloud.openfeign.client.config.default.read-timeout=1000
eureka.client.enabled=false
```

### Profiles Disponibles
- **default** - Configuración para desarrollo local
- **test** - Configuración para ejecución de tests
- **prod** - Configuración para producción

### Plugin de Testing Configurado
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <argLine>
            -Xmx1024m
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.util=ALL-UNNAMED
        </argLine>
        <systemPropertyVariables>
            <spring.profiles.active>test</spring.profiles.active>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

## 🚀 Próximas Mejoras

- [ ] **Notificaciones** - Alertas por email/SMS para vencimientos
- [ ] **Reservas** - Sistema de reservas cuando no hay copias disponibles
- [ ] **Multas** - Cálculo automático de penalizaciones por retraso
- [ ] **Renovaciones automáticas** - Para usuarios premium sin penalizaciones
- [ ] **Integración con calendario** - Eventos de vencimiento en calendario personal
- [ ] **Análisis predictivo** - ML para predecir demanda de libros
- [ ] **API de terceros** - Integración con sistemas bibliotecarios externos
- [ ] **Audit Trail** - Tracking completo de cambios en préstamos
- [ ] **Bulk operations** - Operaciones masivas de préstamos
- [ ] **Circuit Breaker** - Resilience4j para tolerancia a fallos
- [ ] **Cache** - Redis para mejorar performance de consultas frecuentes
- [ ] **Métricas avanzadas** - Dashboard con Grafana/Prometheus

## 📊 Arquitectura del Sistema

### Flujo de Comunicación
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   User Service  │    │  Loan Service   │    │  Book Service   │
│     :8082       │    │     :8083       │    │     :8081       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │  1. Validate User     │                       │
         │◄──────────────────────│                       │
         │                       │                       │
         │                       │  2. Check Availability│
         │                       │──────────────────────►│
         │                       │                       │
         │                       │  3. Update Inventory  │
         │                       │──────────────────────►│
         │                       │                       │
```

### Capas de la Aplicación
```
LoanController -> LoanService -> [UserServiceClient, BookServiceClient]
                     ↓                           ↓
             LoanRepository                [User Service, Book Service]
                     ↓
                 Database
```

---

## 📞 Soporte

Para reportar bugs o solicitar nuevas características, crear un issue en el repositorio del proyecto.

**Puerto del servicio**: 8083  
**Base de datos**: loan_service  
**Nombre en Eureka**: LOAN-SERVICE

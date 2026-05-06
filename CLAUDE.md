# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## O que é

PoC simplificada de uma plataforma LMS com IA embarcada.
Sem segurança, sem autenticação — foco total no fluxo de criação de conteúdo com IA.
Professor único seedado via Flyway. Sem login, sem token.

## Stack

Java 21 · Spring Boot 3.4 · PostgreSQL 16 · Flyway
Spring AI · Groq (llama-3.3-70b-versatile) · Apache PDFBox · Apache Tika
MapStruct · Lombok · SpringDoc OpenAPI · JUnit 5 · Mockito · Maven

---

## Desenvolvimento Rápido

### Setup Inicial

```bash
# 1. Criar banco de dados PostgreSQL
psql -U postgres
CREATE USER poc_user WITH PASSWORD 'poc123';
CREATE DATABASE poc_llm_simples OWNER poc_user;
\q

# 2. Configurar variáveis de ambiente (.env)
export DB_URL=jdbc:postgresql://localhost:5432/poc_llm_simples
export DB_USERNAME=poc_user
export DB_PASSWORD=poc123
export GROQ_API_KEY=gsk_sua_chave_aqui
export UPLOAD_DIR=uploads

# 3. Ou criar arquivo .env na raiz
# DB_URL=jdbc:postgresql://localhost:5432/poc_llm_simples
# DB_USERNAME=poc_user
# DB_PASSWORD=poc123
# GROQ_API_KEY=gsk_sua_chave_aqui
# UPLOAD_DIR=uploads
```

### Comandos Essenciais

```bash
# Testes
mvn test                              # Todos os testes
mvn test -Dtest=CourseServiceTest     # Teste específico
mvn test -Dtest=*Service              # Todos os *Service

# Servidor dev
mvn spring-boot:run                   # Start da aplicação (http://localhost:8080)

# Migrations (automáticas ao iniciar, mas pode rodar manualmente)
mvn flyway:info                       # Info sobre migrations
mvn flyway:migrate                    # Aplicar migrations
mvn flyway:clean                      # Limpar banco (dev only)

# Build
mvn clean compile                     # Compilar
mvn clean package                     # Build JAR
java -jar target/*.jar                # Rodar JAR

# Swagger UI
http://localhost:8080/swagger-ui.html
```

---

## Modelo de Dados

```
Course    (1) ──> (N) Module
Module    (1) ──> (N) Lesson
Module    (1) ──> (0..1) Quiz
Quiz      (1) ──> (N) Question
Question  (1) ──> (N) Alternative
```

---

## Estrutura de Pacotes

```
src/main/java/br/ufc/llm/
├── PocLlmUfcSimplesApplication.java   → Aplicação Spring Boot
├── course/
│   ├── controller/                    → Endpoints REST
│   ├── service/                       → Lógica de negócio
│   ├── repository/                    → Acesso a dados (Spring Data JPA)
│   ├── domain/                        → Entity JPA + anotações Lombok
│   ├── dto/                           → XxxRequest (entrada), XxxResponse (saída)
│   └── exception/                     → Exceções do domínio
├── module/, lesson/, quiz/            → Mesma estrutura (domínio-driven)
└── shared/
    ├── config/                        → Configurações (Spring Beans, Filters)
    ├── dto/                           → ApiResponse padrão
    └── exception/                     → Custom exceptions + handlers

src/main/resources/
├── application.yml                    → Configuração Spring (DB, logging, etc.)
├── db/migration/                      → Scripts Flyway (V001__, V002__, etc.)

src/test/java/br/ufc/llm/
├── *ServiceTest.java                  → Testes unitários com Mockito
└── api/*ApiTest.java                  → Testes de integração (BD real em testes)
```

---

## Convenções de Código

- **Idioma**: inglês para entidades, métodos, variáveis
- **Pacote raiz**: `br.ufc.llm`
- **DTOs separados**: `XxxRequest` (entrada) e `XxxResponse` (saída)
- **Entities**: Todas possuem `@Id`, `createdAt`, `updatedAt` com `@CreationTimestamp` e `@UpdateTimestamp`
- **API response padrão**:
  ```json
  { "sucesso": true, "mensagem": "", "dados": {}, "timestamp": "" }
  ```
- **Commits**: Português imperativo (e.g., "Adiciona endpoint de criação de curso")

---

## Padrões de Código

### Estrutura de Domínio (Domain-Driven Design)

Cada domínio (`course`, `module`, `lesson`, `quiz`) segue:

- **domain/XxxEntity.java**: JPA Entity com `@Entity`, `@Table`, `@Column`, Lombok `@Data`/`@Getter`/`@Setter`
- **dto/XxxRequest.java, XxxResponse.java**: Pydantic-like com `@NotNull`, `@NotBlank` (javax.validation)
- **repository/XxxRepository.java**: Spring Data JPA, estende `JpaRepository<Entity, ID>`
- **service/XxxService.java**: Lógica de negócio, usa repository
- **controller/XxxController.java**: FastAPI-like com `@RestController`, `@PostMapping`, etc.
- **exception/XxxException.java** (opcional): Exceções customizadas do domínio

### Padrão Entity + DTO

```java
// domain/Course.java
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

// dto/CourseRequest.java
public record CourseRequest(
    @NotBlank String title,
    @NotBlank String category,
    @NotBlank String description
) {}

// dto/CourseResponse.java
public record CourseResponse(
    Long id,
    String title,
    String category,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

### Repository Pattern

```java
// repository/CourseRepository.java
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByTitle(String title);
    List<Course> findByCategory(String category);
}

// service/CourseService.java
@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;
    
    public CourseResponse create(CourseRequest request) {
        Course course = new Course(request.title(), request.category(), request.description());
        Course saved = courseRepository.save(course);
        return mapper.toResponse(saved);
    }
}

// controller/CourseController.java
@RestController
@RequestMapping("/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;
    
    @PostMapping
    public ApiResponse<CourseResponse> create(@RequestBody CourseRequest request) {
        CourseResponse data = courseService.create(request);
        return new ApiResponse<>(true, "Curso criado com sucesso", data);
    }
}
```

### MapStruct para Mapeamento

```java
// mapper/CourseMapper.java
@Mapper(componentModel = "spring")
public interface CourseMapper {
    CourseResponse toResponse(Course entity);
    Course toEntity(CourseRequest request);
    List<CourseResponse> toResponseList(List<Course> entities);
}

// service/CourseService.java
@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private CourseMapper courseMapper;
    
    public CourseResponse create(CourseRequest request) {
        Course course = courseMapper.toEntity(request);
        Course saved = courseRepository.save(course);
        return courseMapper.toResponse(saved);
    }
}
```

### Exceções Customizadas + Handler

```java
// shared/exception/RecursoNaoEncontradoException.java
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String recurso, Long id) {
        super(String.format("%s com ID %d não encontrado", recurso, id));
    }
}

// shared/config/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
            false,
            ex.getMessage(),
            null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}

// service/CourseService.java
public CourseResponse getById(Long id) {
    Course course = courseRepository.findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Course", id));
    return courseMapper.toResponse(course);
}
```

---

## Testing (TDD)

### Estrutura de Testes

```java
// CourseServiceTest.java
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {
    @Mock
    private CourseRepository courseRepository;
    
    @InjectMocks
    private CourseService courseService;
    
    @Test
    void testCreateCourse() {
        // Arrange
        CourseRequest request = new CourseRequest("Java 101", "Linguagem", "Aprenda Java");
        Course saved = new Course(1L, "Java 101", "Linguagem", "Aprenda Java", now(), now());
        when(courseRepository.save(any())).thenReturn(saved);
        
        // Act
        CourseResponse response = courseService.create(request);
        
        // Assert
        assertNotNull(response.id());
        assertEquals("Java 101", response.title());
        verify(courseRepository, times(1)).save(any());
    }
}
```

### Padrão TDD

1. **Escreve o teste** (deve falhar)
2. **Roda `mvn test`** e confirma falha
3. **Implementa mínimo** para passar
4. **Roda `mvn test`** novamente
5. Refatora se necessário

**Importante**: Testes usam Mockito para services, H2 em memória para testes de integração (não PostgreSQL real em testes).

---

## Método de Trabalho

### TDD — Test Driven Development

- Testes escritos ANTES da implementação
- Ordem: escreve o teste → roda (falha) → implementa → roda (passa)
- Quando a IA errar: descreva o erro, não corrija manualmente

### Método Akita

- Nunca pule etapas — implemente na ordem definida
- Uma responsabilidade por classe
- Não antecipe funcionalidades futuras
- Código simples e direto; complexidade só quando necessária
- Ao encontrar um problema: pare, entenda a causa raiz, resolva na origem

---

## Comandos Customizados

```
/arquitetura            → Entidades, tabelas SQL, endpoints REST
/regras-negocio         → Regras de negócio do sistema
/requisitos-funcionais  → Requisitos funcionais da PoC
```

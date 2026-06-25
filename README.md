# 🏆 CasaDeportiva — Backend

> Plataforma web de predicciones y apuestas deportivas con Inteligencia Artificial.  
> Proyecto Final — Desarrollo Web I · Universidad de Caldas · 2026

---

## 👥 Equipo de Desarrollo

| Rol | Nombre | GitHub |
|---|---|---|
| Product Owner | Santiago Cardona Bernal | @santiagocardona56652-code |
| Scrum Master / Tech Lead | Alejandro Orjuela Moreno | @joseorjuela66755-arch |
| Docente Evaluador | Leonardo Montes Marín | Universidad de Caldas |

---

## 📌 Descripción del Proyecto

CasaDeportiva es una plataforma web académica que permite a los usuarios:

- Registrarse e iniciar sesión con autenticación segura JWT + BCrypt
- Consultar partidos de fútbol en tiempo real desde football-data.org
- Ver predicciones generadas por Google Gemini 2.0 Flash AI
- Realizar apuestas virtuales con saldo inicial de $50.000

> No usa dinero real. Es una plataforma con fines estrictamente académicos.

---

## 🛠️ Tecnologías Utilizadas

| Componente | Tecnología | Versión |
|---|---|---|
| Framework | Spring Boot | 3.3.12 |
| Lenguaje | Java | 17 LTS |
| Base de datos | MySQL | 8.0 |
| ORM | Hibernate / Spring Data JPA | 6.5.x |
| Autenticación | JWT (jjwt) | 0.12.6 |
| Seguridad | BCrypt + Spring Security | 6.x |
| IA Generativa | Google Gemini 2.0 Flash | API REST |
| Datos deportivos | football-data.org | v4 |
| Frontend | HTML5 + CSS3 + JavaScript | Vanilla |
| Build | Maven Wrapper | 3.9.x |

---

## 📁 Estructura del Proyecto

casadeportiva-backend/

├── src/main/java/com/casadesportiva/

│   ├── SecurityConfig.java

│   ├── controller/

│   ├── model/

│   ├── repository/

│   ├── service/

│   │   ├── PartidoService.java     (football-data.org)

│   │   └── PrediccionService.java  (Gemini AI)

│   └── security/

├── src/main/resources/

│   ├── application.properties

│   └── static/

│       ├── index.html

│       ├── partidos.html

│       └── predicciones.html

├── docs/

│   ├── Manual_Usuario_CasaDeportiva_2026.docx

│   └── Manual_Tecnico_CasaDeportiva_2026.docx

└── pom.xml

---

## ⚙️ Instalación paso a paso

### Prerrequisitos
- Java 17 LTS: https://adoptium.net
- MySQL 8.0: https://dev.mysql.com/downloads
- Git 2.x

### 1. Clonar el repositorio en rama dev
```bash
git clone https://github.com/santiagocardona56652-code/casadeportiva-backend.git
cd casadeportiva-backend
git checkout dev
```

### 2. Crear la base de datos en MySQL
```sql
CREATE DATABASE IF NOT EXISTS casadeportiva_db
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configurar application.properties
Editar el archivo `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3307/casadeportiva_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=TU_PASSWORD_MYSQL
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
server.port=8080
jwt.secret=casadeportiva-clave-secreta-jwt-minimo-32-caracteres-2026
gemini.api.key=TU_GEMINI_API_KEY
football.api.key=TU_FOOTBALL_API_KEY
```

### 4. Ejecutar el proyecto
```bash
.\mvnw clean spring-boot:run
```

### 5. Acceder a la aplicación
http://localhost:8080

---

## 🔗 Endpoints API REST

| Método | Endpoint | Auth | Descripción |
|---|---|---|---|
| POST | /api/usuarios/registro | Pública | Crear cuenta nueva |
| POST | /api/usuarios/login | Pública | Iniciar sesión |
| GET | /api/partidos | Pública | Listar todos los partidos |
| GET | /api/partidos/estado/{estado} | Pública | Filtrar por PENDIENTE, EN_VIVO, FINALIZADO |
| POST | /api/apuestas | JWT | Crear apuesta virtual |
| GET | /api/apuestas/usuario/{id} | JWT | Ver apuestas del usuario |
| GET | /api/predicciones/{partidoId} | Pública | Predicción IA para un partido |

---

## 🌿 Ramas del Repositorio

| Rama | Propósito |
|---|---|
| `principal` | Versión estable |
| `dev` | Desarrollo activo — versión más reciente |

---

## 📄 Documentación Completa

Disponible en la carpeta `/docs`:
- Manual de Usuario con capturas de pantalla reales
- Manual Técnico con arquitectura, BD y referencia de API

---

## 📋 Tablero Kanban

Gestión ágil del proyecto disponible en:
https://github.com/users/joseorjuela66755-arch/projects/2

---

## 📜 Licencia

Proyecto académico — Universidad de Caldas 2026.

# E-Learning Platform Backend API

A production-ready Python backend API for an e-learning platform with an intelligent RAG-powered chatbot that answers student questions based on course materials.

## Features

- 🔐 **JWT Authentication** - Secure user authentication with role-based access control
- 👥 **User Roles** - Teacher and Student roles with specific permissions
- 📚 **Course Management** - Teachers can create and manage courses
- 📄 **File Upload** - Batch upload of PDF and TXT course materials
- 🤖 **RAG-Powered Chatbot** - AI chatbot using Retrieval-Augmented Generation
- 🔍 **Vector Search** - ChromaDB with cosine similarity for semantic search
- 🎓 **Enrollment System** - Students can enroll in courses
- 🗑️ **Cascade Deletion** - Clean deletion of courses with all associated data

## Tech Stack

- **Framework**: FastAPI
- **Database**: PostgreSQL with SQLAlchemy ORM
- **Vector Store**: ChromaDB (persistent)
- **AI Models**: Ollama (llama3.2 for chat, nomic-embed-text for embeddings)
- **RAG Framework**: LlamaIndex
- **Authentication**: JWT with bcrypt password hashing
- **Package Manager**: uv

## Architecture

### RAG Pipeline

1. **Document Indexing**:
   - Teacher uploads PDF/TXT files
   - Text extraction using pypdf
   - Chunking with SentenceSplitter (1024 tokens, 200 overlap)
   - Embedding generation with nomic-embed-text
   - Storage in ChromaDB with metadata (courseId, fileId, fileName)

2. **Query Processing**:
   - Student submits question for enrolled course
   - Similarity search (cosine) filtered by courseId
   - Top-K relevant chunks retrieved (default: 5)
   - Context + question sent to llama3.2
   - Streaming response back to client

3. **Cascade Deletion**:
   - When course is deleted, all associated data is cleaned:
     - Vector documents in ChromaDB
     - Physical files on disk
     - Database records (enrollments, files)

## Prerequisites

1. **Python 3.11+**
2. **PostgreSQL** - Database server
3. **Ollama** - AI model runtime
4. **uv** - Python package manager

### Install Ollama

```bash
# Visit https://ollama.com/download and install for your OS

# Pull required models
ollama pull llama3.2
ollama pull nomic-embed-text
```

### Install uv

```bash
# Windows (PowerShell)
powershell -c "irm https://astral.sh/uv/install.ps1 | iex"

# macOS/Linux
curl -LsSf https://astral.sh/uv/install.sh | sh
```

## Setup Instructions

### 1. Clone and Navigate

```bash
cd Backend
```

### 2. Create Virtual Environment

```bash
uv venv
```

### 3. Activate Virtual Environment

**Windows (cmd.exe):**
```cmd
.venv\Scripts\activate
```

**Windows (PowerShell):**
```powershell
.venv\Scripts\Activate.ps1
```

**macOS/Linux:**
```bash
source .venv/bin/activate
```

### 4. Install Dependencies

```bash
uv pip install -e .
```

### 5. Configure Environment

Copy the example environment file:

```bash
copy .env.example .env
```

Edit `.env` and configure:

```env
# Database Configuration
DATABASE_URL=postgresql://username:password@localhost:5432/elearning_db

# JWT Configuration
SECRET_KEY=your-super-secret-key-change-this-in-production
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=30

# File Storage
UPLOAD_DIR=./uploads
MAX_FILE_SIZE=52428800  # 50MB

# Ollama Configuration
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=llama3.2
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
OLLAMA_REQUEST_TIMEOUT=120

# ChromaDB Configuration
CHROMA_PERSIST_DIR=./chroma_db
CHROMA_COLLECTION_NAME=course_materials

# RAG Configuration
CHUNK_SIZE=1024
CHUNK_OVERLAP=200
TOP_K_RETRIEVAL=5
SIMILARITY_METRIC=cosine

# API Configuration
API_V1_PREFIX=/api/v1
PROJECT_NAME=E-Learning Platform API
DEBUG=True
```

### 6. Create PostgreSQL Database

```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE elearning_db;

# Exit
\q
```

### 7. Run the Application

```bash
python main.py
```

The API will be available at `http://localhost:8000`

## API Documentation

Once the server is running, access:

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

## API Endpoints

### Authentication

- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login and get JWT token

### Users

- `GET /api/v1/users/me` - Get current user info
- `PUT /api/v1/users/me` - Update current user
- `GET /api/v1/users/{user_id}` - Get user by ID
- `GET /api/v1/users/` - List all users

### Courses

- `POST /api/v1/courses/` - Create course (teacher only)
- `GET /api/v1/courses/` - List all courses
- `GET /api/v1/courses/my-courses` - List teacher's courses
- `GET /api/v1/courses/{course_id}` - Get course details
- `PUT /api/v1/courses/{course_id}` - Update course (teacher only)
- `DELETE /api/v1/courses/{course_id}` - Delete course with cascade (teacher only)

### Enrollments

- `POST /api/v1/enrollments/` - Enroll in course (student only)
- `GET /api/v1/enrollments/my-enrollments` - Get student's enrollments
- `GET /api/v1/enrollments/course/{course_id}` - Get course enrollments
- `DELETE /api/v1/enrollments/{enrollment_id}` - Unenroll from course (student only)

### Course Materials

- `POST /api/v1/files/upload/{course_id}` - Upload files (teacher only, batch supported)
- `GET /api/v1/files/course/{course_id}` - List course materials (teacher only)
- `DELETE /api/v1/files/{file_id}` - Delete file (teacher only)

### AI Chat

- `POST /api/v1/chat/` - Chat with course materials (student only, enrolled required)
- `POST /api/v1/chat/stream` - Chat with streaming response (student only, enrolled required)

## Usage Example

### 1. Register Users

```bash
# Register a teacher
curl -X POST "http://localhost:8000/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teacher@example.com",
    "username": "teacher1",
    "password": "password123",
    "role": "teacher"
  }'

# Register a student
curl -X POST "http://localhost:8000/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@example.com",
    "username": "student1",
    "password": "password123",
    "role": "student"
  }'
```

### 2. Login

```bash
# Login as teacher
curl -X POST "http://localhost:8000/api/v1/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=teacher1&password=password123"
```

Response:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

### 3. Create Course (Teacher)

```bash
curl -X POST "http://localhost:8000/api/v1/courses/" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Introduction to Python",
    "description": "Learn Python programming basics"
  }'
```

### 4. Upload Course Materials (Teacher)

```bash
curl -X POST "http://localhost:8000/api/v1/files/upload/1" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "files=@lecture1.pdf" \
  -F "files=@lecture2.pdf" \
  -F "files=@notes.txt"
```

### 5. Enroll in Course (Student)

```bash
curl -X POST "http://localhost:8000/api/v1/enrollments/" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "course_id": 1
  }'
```

### 6. Chat with Course Materials (Student)

```bash
curl -X POST "http://localhost:8000/api/v1/chat/" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "course_id": 1,
    "question": "What are the main concepts of object-oriented programming?"
  }'
```

Response:
```json
{
  "answer": "Based on the course materials, object-oriented programming has four main concepts: encapsulation, inheritance, polymorphism, and abstraction...",
  "course_id": 1,
  "retrieved_chunks": 5
}
```

## Project Structure

```
Backend/
├── app/
│   ├── api/                    # API endpoints
│   │   ├── auth.py            # Authentication endpoints
│   │   ├── users.py           # User management
│   │   ├── courses.py         # Course management
│   │   ├── enrollments.py    # Enrollment management
│   │   ├── files.py           # File upload/management
│   │   └── chat.py            # AI chat endpoints
│   ├── models/                 # SQLAlchemy models
│   │   ├── user.py
│   │   ├── course.py
│   │   ├── enrollment.py
│   │   └── course_material_file.py
│   ├── repositories/           # Data access layer
│   │   ├── user_repository.py
│   │   ├── course_repository.py
│   │   ├── enrollment_repository.py
│   │   └── file_repository.py
│   ├── schemas/                # Pydantic schemas
│   │   ├── user.py
│   │   ├── course.py
│   │   ├── enrollment.py
│   │   ├── file.py
│   │   └── chat.py
│   ├── services/               # Business logic
│   │   ├── vector_store.py    # RAG pipeline & ChromaDB
│   │   └── file_service.py    # File processing
│   ├── utils/                  # Utilities
│   │   └── security.py        # Auth & JWT
│   ├── config.py              # Configuration
│   └── database.py            # Database setup
├── main.py                     # Application entry point
├── pyproject.toml             # Project dependencies
├── .env.example               # Environment template
├── .gitignore
└── README.md
```

## Key Features Explained

### RAG Pipeline

The application uses a proper RAG (Retrieval-Augmented Generation) architecture:

1. **Indexing**: Documents are split into chunks, embedded, and stored in ChromaDB
2. **Retrieval**: User queries trigger similarity search with course filtering
3. **Generation**: Retrieved context is passed to LLM for answer generation

This is NOT simple prompt concatenation - it's a full RAG implementation with:
- Vector embeddings for semantic search
- Persistent vector store (ChromaDB)
- Metadata filtering (courseId, fileId, fileName)
- Cosine similarity matching
- Chunking with overlap for context preservation

### Cascade Deletion

When a course is deleted, the system performs comprehensive cleanup:

1. **Vector Store**: All document chunks for the course are removed from ChromaDB
2. **File System**: All physical files are deleted from disk
3. **Database**: All related records (enrollments, file metadata) are cascade-deleted

This ensures no orphaned data remains in any layer of the system.

### Security

- JWT-based authentication
- Role-based access control (RBAC)
- Password hashing with bcrypt
- Protected endpoints with dependency injection
- Ownership verification for resources

## Development

### Run in Debug Mode

```bash
# Set DEBUG=True in .env
python main.py
```

### Database Migrations (Optional)

To use Alembic for migrations:

```bash
# Initialize Alembic
alembic init alembic

# Create migration
alembic revision --autogenerate -m "Initial migration"

# Apply migration
alembic upgrade head
```

## Troubleshooting

### Ollama Connection Issues

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama service if needed
ollama serve
```

### Database Connection Issues

```bash
# Test PostgreSQL connection
psql -U username -d elearning_db -h localhost

# Check if database exists
psql -U postgres -c "\l"
```

### ChromaDB Issues

```bash
# Delete and recreate ChromaDB
rm -rf chroma_db/
# Restart application to reinitialize
```

## Production Deployment

For production deployment:

1. Set `DEBUG=False` in `.env`
2. Use a strong `SECRET_KEY`
3. Configure CORS properly (restrict origins)
4. Use environment-specific database
5. Set up HTTPS/SSL
6. Configure proper logging
7. Use a production WSGI server (e.g., Gunicorn)
8. Set up monitoring and health checks

## License

MIT License

## Support

For issues and questions, please open an issue on the repository.

# System Architecture & Flow Diagrams

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Layer                              │
│  (Mobile App / Web Frontend / API Clients like Postman)         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ HTTP/REST API
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      FastAPI Application                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  API Layer (Routers)                                      │  │
│  │  • auth.py    • users.py   • courses.py                  │  │
│  │  • enrollments.py  • files.py  • chat.py                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Service Layer                                            │  │
│  │  • vector_store.py (RAG Pipeline)                         │  │
│  │  • file_service.py (File Processing)                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Repository Layer                                         │  │
│  │  • user_repository.py  • course_repository.py            │  │
│  │  • enrollment_repository.py  • file_repository.py        │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Utils & Security                                         │  │
│  │  • JWT Authentication  • Password Hashing  • RBAC        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────┬───────────────────────┬──────────────────┬──────────┬────┘
      │                       │                  │          │
      │                       │                  │          │
      ▼                       ▼                  ▼          ▼
┌──────────┐         ┌─────────────┐    ┌──────────┐  ┌────────┐
│PostgreSQL│         │  ChromaDB   │    │  Ollama  │  │  File  │
│ Database │         │Vector Store │    │  Models  │  │ System │
│          │         │             │    │          │  │        │
│• Users   │         │• Embeddings │    │• llama3.2│  │• PDFs  │
│• Courses │         │• Metadata   │    │• nomic-  │  │• TXTs  │
│• Enrolls │         │• Cosine     │    │  embed   │  │        │
│• Files   │         │  Similarity │    │  -text   │  │        │
└──────────┘         └─────────────┘    └──────────┘  └────────┘
```

## RAG Pipeline Flow

### Document Indexing Flow

```
Teacher Action: Upload Files
         │
         ▼
┌─────────────────────────────────────────┐
│  1. File Upload API                     │
│     POST /api/v1/files/upload/{id}      │
│     • Batch support (multiple files)    │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  2. File Service                        │
│     • Validate file type (PDF/TXT)      │
│     • Save to disk                      │
│     • Create DB record                  │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  3. Text Extraction                     │
│     • PDF: pypdf.PdfReader              │
│     • TXT: native read                  │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  4. Text Chunking                       │
│     • SentenceSplitter                  │
│     • Chunk size: 1024 tokens           │
│     • Overlap: 200 tokens               │
│     • Preserve context                  │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  5. Embedding Generation                │
│     • Model: nomic-embed-text           │
│     • Via Ollama API                    │
│     • Per-chunk embeddings              │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  6. ChromaDB Storage                    │
│     • Store vector + metadata           │
│     • Metadata: {                       │
│         courseId: int,                  │
│         fileId: int,                    │
│         fileName: string                │
│       }                                 │
│     • Cosine similarity space           │
└─────────────────────────────────────────┘
```

### Query & Chat Flow

```
Student Action: Ask Question
         │
         ▼
┌─────────────────────────────────────────┐
│  1. Chat API                            │
│     POST /api/v1/chat/                  │
│     • Verify enrollment                 │
│     • Extract question + course_id      │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  2. Vector Store Service                │
│     • Receive: question + course_id     │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  3. Query Embedding                     │
│     • Embed question                    │
│     • Model: nomic-embed-text           │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  4. Similarity Search                   │
│     • Search ChromaDB                   │
│     • Filter: courseId == course_id     │
│     • Metric: Cosine similarity         │
│     • Retrieve: top-K chunks (K=5)      │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  5. Context Construction                │
│     • Combine retrieved chunks          │
│     • Format: "Context: ... Question:.."│
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  6. LLM Generation                      │
│     • Model: llama3.2                   │
│     • Via Ollama API                    │
│     • Generate answer from context      │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  7. Stream Response                     │
│     • Token-by-token streaming          │
│     • Return to client                  │
└─────────────────────────────────────────┘
```

## User Roles & Permissions

```
┌─────────────────────────────────────────────────────────────┐
│                          TEACHER                             │
├─────────────────────────────────────────────────────────────┤
│  Can:                                                        │
│  ✓ Create courses                                           │
│  ✓ Update own courses                                       │
│  ✓ Delete own courses (cascade)                            │
│  ✓ Upload course materials (batch)                          │
│  ✓ Delete course materials                                  │
│  ✓ View course enrollments                                  │
│                                                              │
│  Cannot:                                                     │
│  ✗ Enroll in courses                                        │
│  ✗ Chat with materials                                      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                          STUDENT                             │
├─────────────────────────────────────────────────────────────┤
│  Can:                                                        │
│  ✓ View all courses                                         │
│  ✓ Enroll in courses                                        │
│  ✓ Unenroll from courses                                    │
│  ✓ Chat with enrolled course materials                      │
│  ✓ View own enrollments                                     │
│                                                              │
│  Cannot:                                                     │
│  ✗ Create courses                                           │
│  ✗ Upload materials                                         │
│  ✗ Chat with non-enrolled courses                           │
└─────────────────────────────────────────────────────────────┘
```

## Database Schema

```
┌──────────────────────────┐
│         Users            │
├──────────────────────────┤
│ PK  id                   │
│     email (unique)       │
│     username (unique)    │
│     hashed_password      │
│     role (enum)          │
│     created_at           │
│     updated_at           │
└────────┬─────────────────┘
         │ 1
         │
         │ M
┌────────▼─────────────────┐
│        Courses           │
├──────────────────────────┤
│ PK  id                   │
│     title                │
│     description          │
│ FK  teacher_id           │
│     created_at           │
│     updated_at           │
└────┬────┬────────────────┘
     │ 1  │ 1
     │    │
   M │    │ M
┌────▼───────────────────┐  ┌──────────▼────────────────┐
│    Enrollments         │  │  CourseMaterialFiles      │
├────────────────────────┤  ├───────────────────────────┤
│ PK  id                 │  │ PK  id                    │
│ FK  student_id         │  │ FK  course_id             │
│ FK  course_id          │  │     filename              │
│     enrolled_at        │  │     original_filename     │
│                        │  │     file_path             │
│ UNIQUE(student,course) │  │     file_size             │
└────────────────────────┘  │     mime_type             │
                            │     uploaded_at           │
                            └───────────────────────────┘
```

## ChromaDB Vector Store Structure

```
┌───────────────────────────────────────────────────────────────┐
│                    ChromaDB Collection                         │
│                  "course_materials"                            │
├───────────────────────────────────────────────────────────────┤
│                                                                │
│  Document 1:                                                   │
│  ├─ id: "uuid-1"                                              │
│  ├─ embedding: [0.123, 0.456, ..., 0.789] (vector)           │
│  ├─ text: "Python is a high-level programming language..."    │
│  └─ metadata: {                                               │
│       course_id: 1,                                           │
│       file_id: 5,                                             │
│       filename: "intro_to_python.pdf"                         │
│     }                                                          │
│                                                                │
│  Document 2:                                                   │
│  ├─ id: "uuid-2"                                              │
│  ├─ embedding: [0.234, 0.567, ..., 0.890] (vector)           │
│  ├─ text: "Object-oriented programming has four pillars..."   │
│  └─ metadata: {                                               │
│       course_id: 1,                                           │
│       file_id: 5,                                             │
│       filename: "intro_to_python.pdf"                         │
│     }                                                          │
│                                                                │
│  ... (many more document chunks)                              │
│                                                                │
│  Similarity Metric: COSINE                                     │
│  Index Type: HNSW (fast approximate nearest neighbor)         │
└───────────────────────────────────────────────────────────────┘
```

## Cascade Deletion Flow

```
Teacher Action: Delete Course
         │
         ▼
┌─────────────────────────────────────────┐
│  1. Course API                          │
│     DELETE /api/v1/courses/{id}         │
│     • Verify teacher ownership          │
└─────────────┬───────────────────────────┘
              │
              ├─────────────────────┐
              │                     │
              ▼                     ▼
┌──────────────────────┐  ┌─────────────────────┐
│  2a. ChromaDB        │  │  2b. File System    │
│      Deletion        │  │      Deletion       │
│                      │  │                     │
│  • Query: all docs   │  │  • Delete directory │
│    where course_id   │  │    ./uploads/{id}/  │
│  • Delete vectors    │  │  • All PDFs/TXTs    │
└──────────────────────┘  └─────────────────────┘
              │                     │
              └──────────┬──────────┘
                         ▼
              ┌─────────────────────┐
              │  3. Database        │
              │     Cascade Delete  │
              │                     │
              │  • Enrollments      │
              │    (ON DELETE       │
              │     CASCADE)        │
              │                     │
              │  • Material Files   │
              │    (ON DELETE       │
              │     CASCADE)        │
              │                     │
              │  • Course Record    │
              └─────────────────────┘
                         │
                         ▼
              ┌─────────────────────┐
              │  4. Complete        │
              │     All data clean  │
              └─────────────────────┘
```

## Authentication Flow

```
User Registration/Login
         │
         ▼
┌─────────────────────────────────────────┐
│  1. Register                            │
│     POST /api/v1/auth/register          │
│     • Email, username, password, role   │
│     • Hash password (bcrypt)            │
│     • Store in database                 │
└─────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│  2. Login                               │
│     POST /api/v1/auth/login             │
│     • Verify credentials                │
│     • Generate JWT token                │
│     • Return: { access_token, type }    │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  3. Protected Request                   │
│     Header: Authorization: Bearer TOKEN │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  4. JWT Validation                      │
│     • Decode token                      │
│     • Verify signature                  │
│     • Check expiration                  │
│     • Extract user_id, role             │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  5. Role-Based Access Control           │
│     • Check user role                   │
│     • Verify permissions                │
│     • Allow/Deny access                 │
└─────────────────────────────────────────┘
```

## File Upload Flow (Batch)

```
Teacher: Upload Multiple Files
         │
         ▼
┌─────────────────────────────────────────┐
│  POST /api/v1/files/upload/{course_id}  │
│  files: [file1.pdf, file2.pdf, ...]    │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  For Each File:                         │
│  ┌───────────────────────────────────┐ │
│  │ 1. Create DB record (get file_id) │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ 2. Validate (PDF/TXT, size)       │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ 3. Save to disk                   │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ 4. Extract text                   │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ 5. Index in ChromaDB              │ │
│  └───────────────────────────────────┘ │
│  ┌───────────────────────────────────┐ │
│  │ 6. Update DB record               │ │
│  └───────────────────────────────────┘ │
│                                         │
│  If error: rollback & add to failed    │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  Return Response:                       │
│  {                                      │
│    uploaded_files: [...],               │
│    total_files: 5,                      │
│    failed_files: ["file3.pdf: error"]  │
│  }                                      │
└─────────────────────────────────────────┘
```

## Technology Stack Layers

```
┌─────────────────────────────────────────────────────────┐
│                   Presentation Layer                     │
│  • FastAPI (REST API)                                   │
│  • Pydantic (Validation & Serialization)                │
│  • OAuth2 + JWT (Authentication)                        │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    Business Layer                        │
│  • Services (RAG, File Processing)                      │
│  • Repositories (Data Access)                           │
│  • Domain Logic                                         │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                     Data Layer                           │
│  • SQLAlchemy ORM                                       │
│  • PostgreSQL (Relational Data)                         │
│  • ChromaDB (Vector Data)                               │
│  • File System (Binary Data)                            │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                  External Services                       │
│  • Ollama (LLM & Embeddings)                            │
│    - llama3.2 (Chat Model)                              │
│    - nomic-embed-text (Embedding Model)                 │
└─────────────────────────────────────────────────────────┘
```

This architecture ensures:
- ✅ Separation of concerns
- ✅ Scalability
- ✅ Maintainability
- ✅ Testability
- ✅ Security
- ✅ Performance

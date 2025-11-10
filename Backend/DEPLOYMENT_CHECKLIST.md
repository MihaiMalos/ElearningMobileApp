# Deployment Checklist

## Pre-Deployment Setup

### 1. System Requirements
- [ ] Python 3.11 or higher installed
- [ ] PostgreSQL 12 or higher installed and running
- [ ] Ollama installed and running
- [ ] At least 4GB free disk space
- [ ] At least 8GB RAM (for Ollama models)

### 2. Install Required Models
```bash
# Pull Ollama models (required)
ollama pull llama3.2
ollama pull nomic-embed-text

# Verify models are available
ollama list
```

Expected output:
```
NAME                      ID              SIZE
llama3.2:latest          ...             2.0 GB
nomic-embed-text:latest  ...             274 MB
```

### 3. Database Setup
```bash
# Create PostgreSQL database
psql -U postgres
```
```sql
CREATE DATABASE elearning_db;
CREATE USER elearning_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE elearning_db TO elearning_user;
\q
```

### 4. Environment Configuration
- [ ] Copy `.env.example` to `.env`
- [ ] Update `DATABASE_URL` with your credentials
- [ ] Generate strong `SECRET_KEY` (use: `openssl rand -hex 32`)
- [ ] Review and adjust other settings

```env
DATABASE_URL=postgresql://elearning_user:secure_password@localhost:5432/elearning_db
SECRET_KEY=<your-generated-secret-key>
```

## Installation Steps

### Option 1: Using uv (Recommended)
```bash
# 1. Create virtual environment
uv venv

# 2. Activate virtual environment
# Windows:
.venv\Scripts\activate
# Unix/macOS:
source .venv/bin/activate

# 3. Install dependencies
uv pip install -e .
```

### Option 2: Using pip
```bash
# 1. Create virtual environment
python -m venv .venv

# 2. Activate virtual environment
# Windows:
.venv\Scripts\activate
# Unix/macOS:
source .venv/bin/activate

# 3. Install dependencies
pip install -r requirements.txt
```

## Verification Steps

### 1. Check Ollama Connection
```bash
curl http://localhost:11434/api/tags
```
- [ ] Ollama responds with model list

### 2. Check Database Connection
```bash
# Windows:
python -c "from app.database import engine; print('Database connected!' if engine.connect() else 'Connection failed')"

# Unix/macOS:
python -c "from app.database import engine; print('Database connected!' if engine.connect() else 'Connection failed')"
```
- [ ] Database connection successful

### 3. Start Application
```bash
python main.py
```

Expected output:
```
Initializing database...
Database initialized successfully!
Initializing vector store...
Vector store initialized successfully!
INFO:     Started server process [...]
INFO:     Uvicorn running on http://0.0.0.0:8000
```

- [ ] Application starts without errors
- [ ] No database errors
- [ ] ChromaDB initializes successfully

### 4. Test Endpoints

#### Health Check
```bash
curl http://localhost:8000/health
```
Expected: `{"status":"healthy"}`
- [ ] Health endpoint responds

#### API Documentation
- [ ] Open http://localhost:8000/docs
- [ ] Swagger UI loads correctly
- [ ] All endpoints visible

## Functional Testing

### 1. Authentication Flow
```bash
# Register a teacher
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "teacher@test.com",
    "username": "teacher_test",
    "password": "Test123456",
    "role": "teacher"
  }'
```
- [ ] Teacher registration successful

```bash
# Login as teacher
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=teacher_test&password=Test123456"
```
- [ ] Login returns access token
- [ ] Save token for next steps

### 2. Course Management
```bash
# Create a course (replace YOUR_TOKEN)
curl -X POST http://localhost:8000/api/v1/courses/ \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Course",
    "description": "Testing course creation"
  }'
```
- [ ] Course created successfully
- [ ] Note the course ID

### 3. File Upload
Create a test file `test.txt`:
```
This is a test course about Python programming.
Python is a high-level, interpreted programming language.
It emphasizes code readability and simplicity.
```

```bash
# Upload file (replace YOUR_TOKEN and COURSE_ID)
curl -X POST http://localhost:8000/api/v1/files/upload/COURSE_ID \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "files=@test.txt"
```
- [ ] File uploaded successfully
- [ ] Response shows file was indexed
- [ ] Check `uploads/` directory created

### 4. Student Enrollment
```bash
# Register a student
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "student@test.com",
    "username": "student_test",
    "password": "Test123456",
    "role": "student"
  }'
```
- [ ] Student registration successful

```bash
# Login as student
curl -X POST http://localhost:8000/api/v1/auth/login \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=student_test&password=Test123456"
```
- [ ] Student login successful
- [ ] Save student token

```bash
# Enroll in course (replace STUDENT_TOKEN and COURSE_ID)
curl -X POST http://localhost:8000/api/v1/enrollments/ \
  -H "Authorization: Bearer STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"course_id": COURSE_ID}'
```
- [ ] Enrollment successful

### 5. AI Chat (RAG Test)
```bash
# Chat with course materials (replace STUDENT_TOKEN and COURSE_ID)
curl -X POST http://localhost:8000/api/v1/chat/ \
  -H "Authorization: Bearer STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "course_id": COURSE_ID,
    "question": "What is Python?"
  }'
```
- [ ] Response received
- [ ] Answer mentions Python programming
- [ ] `retrieved_chunks` > 0
- [ ] Answer is relevant to uploaded content

## ChromaDB Verification

### Check Vector Store
```python
# Run this Python script to verify ChromaDB
import chromadb
client = chromadb.PersistentClient(path="./chroma_db")
collection = client.get_collection("course_materials")
print(f"Total documents: {collection.count()}")
```
- [ ] ChromaDB directory `chroma_db/` exists
- [ ] Collection contains documents
- [ ] Document count matches uploaded files

## Cascade Deletion Test

```bash
# Delete course (replace TEACHER_TOKEN and COURSE_ID)
curl -X DELETE http://localhost:8000/api/v1/courses/COURSE_ID \
  -H "Authorization: Bearer TEACHER_TOKEN"
```

Verify cleanup:
- [ ] Course deleted from database
- [ ] Enrollments deleted
- [ ] File records deleted
- [ ] Physical files deleted (`uploads/COURSE_ID/` removed)
- [ ] Vector documents removed from ChromaDB

## Production Deployment Checklist

### Security
- [ ] Change `SECRET_KEY` to strong random value
- [ ] Set `DEBUG=False` in production
- [ ] Configure CORS with specific origins (not `["*"]`)
- [ ] Use HTTPS/SSL certificates
- [ ] Set up firewall rules
- [ ] Enable PostgreSQL SSL connections
- [ ] Restrict database access to application only

### Performance
- [ ] Use production WSGI server (Gunicorn)
- [ ] Configure connection pooling
- [ ] Set up database indexes
- [ ] Configure file upload size limits
- [ ] Set up CDN for static files (if any)

### Monitoring
- [ ] Set up logging (file + remote)
- [ ] Configure error tracking (e.g., Sentry)
- [ ] Set up health check monitoring
- [ ] Configure database backup automation
- [ ] Set up disk space monitoring

### Scaling
- [ ] Consider load balancer setup
- [ ] Plan for horizontal scaling
- [ ] Set up Redis for session management (if needed)
- [ ] Configure backup/disaster recovery

### Documentation
- [ ] Document deployment process
- [ ] Create runbooks for common issues
- [ ] Document backup/restore procedures
- [ ] Create user guides

## Troubleshooting Common Issues

### Issue: Database connection fails
**Solution:**
1. Check PostgreSQL is running: `pg_isready`
2. Verify credentials in `.env`
3. Check database exists: `psql -l`
4. Verify network access: `telnet localhost 5432`

### Issue: Ollama connection fails
**Solution:**
1. Start Ollama: `ollama serve`
2. Check models: `ollama list`
3. Verify port: `curl http://localhost:11434`
4. Check firewall settings

### Issue: ChromaDB errors
**Solution:**
1. Delete ChromaDB: `rm -rf chroma_db/`
2. Restart application
3. Re-upload files to reindex

### Issue: File upload fails
**Solution:**
1. Check `uploads/` directory permissions
2. Verify file size under limit (50MB default)
3. Check disk space
4. Verify file type (PDF/TXT only)

### Issue: RAG returns no results
**Solution:**
1. Verify files are uploaded
2. Check ChromaDB has documents
3. Verify student is enrolled
4. Check Ollama models are loaded

## Performance Benchmarks

Expected performance on recommended hardware:

- [ ] File upload (1MB): < 2 seconds
- [ ] Text extraction (10-page PDF): < 3 seconds
- [ ] Document indexing (1000 tokens): < 5 seconds
- [ ] Chat query response: < 10 seconds
- [ ] API response time (simple): < 100ms

## Sign-Off

- [ ] All system requirements met
- [ ] All installation steps completed
- [ ] All verification tests passed
- [ ] All functional tests passed
- [ ] RAG pipeline working correctly
- [ ] Cascade deletion working correctly
- [ ] Security checklist reviewed
- [ ] Production deployment plan reviewed

**Deployed by:** ___________________  
**Date:** ___________________  
**Environment:** [ ] Development [ ] Staging [ ] Production  
**Version:** 1.0.0  

## Support & Maintenance

For issues:
1. Check logs in console output
2. Review `ARCHITECTURE.md` for system design
3. Consult `README.md` for detailed documentation
4. Check `QUICKSTART.md` for common setup issues

**System Status:** ✅ Ready for Production

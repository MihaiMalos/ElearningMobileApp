# API Testing Guide

Complete guide for testing the E-Learning Platform API endpoints.

## Prerequisites

- Application running on `http://localhost:8000`
- PostgreSQL database configured
- Ollama with models loaded
- Test files ready (PDF/TXT)

## Test Environment Setup

### 1. Verify Application is Running

```bash
curl http://localhost:8000/health
```

Expected response:
```json
{"status": "healthy"}
```

### 2. Access API Documentation

Open in browser:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Test Scenarios

### Scenario 1: Complete Teacher Workflow

#### Step 1.1: Register Teacher Account

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.teacher@university.edu",
    "username": "johnteacher",
    "password": "SecurePass123!",
    "role": "teacher"
  }'
```

**Expected Response (201):**
```json
{
  "id": 1,
  "email": "john.teacher@university.edu",
  "username": "johnteacher",
  "role": "teacher",
  "created_at": "2025-11-10T10:30:00",
  "updated_at": "2025-11-10T10:30:00"
}
```

**Validation:**
- ✓ Status code is 201
- ✓ User ID is returned
- ✓ Role is "teacher"
- ✓ Timestamps are present

#### Step 1.2: Login as Teacher

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=johnteacher&password=SecurePass123!"
```

**Expected Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

**Save token for subsequent requests:**
```bash
export TEACHER_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Validation:**
- ✓ Status code is 200
- ✓ Access token is present
- ✓ Token type is "bearer"

#### Step 1.3: Get Current User Info

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/users/me" \
  -H "Authorization: Bearer $TEACHER_TOKEN"
```

**Expected Response (200):**
```json
{
  "id": 1,
  "email": "john.teacher@university.edu",
  "username": "johnteacher",
  "role": "teacher",
  "created_at": "2025-11-10T10:30:00",
  "updated_at": "2025-11-10T10:30:00"
}
```

**Validation:**
- ✓ Correct user information returned
- ✓ Authentication working

#### Step 1.4: Create a Course

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/courses/" \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Introduction to Machine Learning",
    "description": "Learn the fundamentals of ML, including supervised and unsupervised learning, neural networks, and practical applications."
  }'
```

**Expected Response (201):**
```json
{
  "id": 1,
  "title": "Introduction to Machine Learning",
  "description": "Learn the fundamentals of ML...",
  "teacher_id": 1,
  "created_at": "2025-11-10T10:35:00",
  "updated_at": "2025-11-10T10:35:00"
}
```

**Save course ID:**
```bash
export COURSE_ID=1
```

**Validation:**
- ✓ Status code is 201
- ✓ Course ID returned
- ✓ Teacher ID matches logged-in user

#### Step 1.5: Create Test Files

Create `ml_basics.txt`:
```
Machine Learning Basics

Machine learning is a branch of artificial intelligence that enables 
computers to learn from data without explicit programming.

Types of Machine Learning:
1. Supervised Learning - Learning from labeled data
2. Unsupervised Learning - Finding patterns in unlabeled data
3. Reinforcement Learning - Learning through trial and error

Key Concepts:
- Training data and test data
- Features and labels
- Model evaluation metrics
- Overfitting and underfitting
```

Create `neural_networks.txt`:
```
Neural Networks Overview

Neural networks are computing systems inspired by biological neural networks.

Architecture:
- Input Layer: Receives the input features
- Hidden Layers: Process information
- Output Layer: Produces predictions

Activation Functions:
- ReLU (Rectified Linear Unit)
- Sigmoid
- Tanh
- Softmax for classification

Training Process:
1. Forward propagation
2. Loss calculation
3. Backpropagation
4. Weight updates
```

#### Step 1.6: Upload Course Materials (Batch)

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/files/upload/$COURSE_ID" \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -F "files=@ml_basics.txt" \
  -F "files=@neural_networks.txt"
```

**Expected Response (200):**
```json
{
  "uploaded_files": [
    {
      "id": 1,
      "course_id": 1,
      "filename": "uuid1.txt",
      "original_filename": "ml_basics.txt",
      "file_size": 532,
      "mime_type": "text/plain",
      "uploaded_at": "2025-11-10T10:40:00"
    },
    {
      "id": 2,
      "course_id": 1,
      "filename": "uuid2.txt",
      "original_filename": "neural_networks.txt",
      "file_size": 456,
      "mime_type": "text/plain",
      "uploaded_at": "2025-11-10T10:40:00"
    }
  ],
  "total_files": 2,
  "failed_files": []
}
```

**Validation:**
- ✓ Both files uploaded successfully
- ✓ No failed files
- ✓ File IDs returned
- ✓ Check `uploads/1/` directory created
- ✓ Files are being indexed in background

#### Step 1.7: List Course Materials

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/files/course/$COURSE_ID" \
  -H "Authorization: Bearer $TEACHER_TOKEN"
```

**Expected Response (200):**
```json
[
  {
    "id": 1,
    "course_id": 1,
    "filename": "uuid1.txt",
    "original_filename": "ml_basics.txt",
    "file_size": 532,
    "mime_type": "text/plain",
    "uploaded_at": "2025-11-10T10:40:00"
  },
  {
    "id": 2,
    "course_id": 1,
    "filename": "uuid2.txt",
    "original_filename": "neural_networks.txt",
    "file_size": 456,
    "mime_type": "text/plain",
    "uploaded_at": "2025-11-10T10:40:00"
  }
]
```

**Validation:**
- ✓ All uploaded files listed
- ✓ Correct metadata returned

#### Step 1.8: Get Course Details

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/courses/$COURSE_ID" \
  -H "Authorization: Bearer $TEACHER_TOKEN"
```

**Expected Response (200):**
```json
{
  "id": 1,
  "title": "Introduction to Machine Learning",
  "description": "Learn the fundamentals of ML...",
  "teacher_id": 1,
  "created_at": "2025-11-10T10:35:00",
  "updated_at": "2025-11-10T10:35:00",
  "teacher_username": "johnteacher",
  "materials_count": 2,
  "enrollments_count": 0
}
```

**Validation:**
- ✓ Materials count is 2
- ✓ Enrollments count is 0
- ✓ Teacher username shown

---

### Scenario 2: Complete Student Workflow

#### Step 2.1: Register Student Account

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice.student@university.edu",
    "username": "alicestudent",
    "password": "StudentPass123!",
    "role": "student"
  }'
```

**Expected Response (201):**
```json
{
  "id": 2,
  "email": "alice.student@university.edu",
  "username": "alicestudent",
  "role": "student",
  "created_at": "2025-11-10T10:45:00",
  "updated_at": "2025-11-10T10:45:00"
}
```

**Validation:**
- ✓ Status code is 201
- ✓ Role is "student"

#### Step 2.2: Login as Student

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/auth/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=alicestudent&password=StudentPass123!"
```

**Save token:**
```bash
export STUDENT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### Step 2.3: List Available Courses

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/courses/" \
  -H "Authorization: Bearer $STUDENT_TOKEN"
```

**Expected Response (200):**
```json
[
  {
    "id": 1,
    "title": "Introduction to Machine Learning",
    "description": "Learn the fundamentals of ML...",
    "teacher_id": 1,
    "created_at": "2025-11-10T10:35:00",
    "updated_at": "2025-11-10T10:35:00"
  }
]
```

**Validation:**
- ✓ Course is visible to students
- ✓ Can browse available courses

#### Step 2.4: Enroll in Course

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/enrollments/" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "course_id": 1
  }'
```

**Expected Response (201):**
```json
{
  "id": 1,
  "student_id": 2,
  "course_id": 1,
  "enrolled_at": "2025-11-10T10:50:00"
}
```

**Validation:**
- ✓ Status code is 201
- ✓ Enrollment ID returned
- ✓ Correct student and course IDs

#### Step 2.5: Verify Enrollment

**Request:**
```bash
curl -X GET "http://localhost:8000/api/v1/enrollments/my-enrollments" \
  -H "Authorization: Bearer $STUDENT_TOKEN"
```

**Expected Response (200):**
```json
[
  {
    "id": 1,
    "student_id": 2,
    "course_id": 1,
    "enrolled_at": "2025-11-10T10:50:00",
    "course_title": "Introduction to Machine Learning",
    "course_description": "Learn the fundamentals of ML...",
    "teacher_username": "johnteacher"
  }
]
```

**Validation:**
- ✓ Enrollment listed
- ✓ Course details included

#### Step 2.6: Chat with Course Materials

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/chat/" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "course_id": 1,
    "question": "What are the three main types of machine learning?"
  }'
```

**Expected Response (200):**
```json
{
  "answer": "Based on the course materials, the three main types of machine learning are:\n\n1. Supervised Learning - Learning from labeled data where the algorithm learns to map inputs to outputs using training examples.\n\n2. Unsupervised Learning - Finding patterns and structures in unlabeled data without predefined categories.\n\n3. Reinforcement Learning - Learning through trial and error, where the algorithm learns optimal actions through rewards and penalties.",
  "course_id": 1,
  "retrieved_chunks": 5
}
```

**Validation:**
- ✓ Answer is relevant and based on uploaded materials
- ✓ Retrieved chunks > 0
- ✓ Answer mentions the three types correctly

#### Step 2.7: Ask Another Question

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/chat/" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "course_id": 1,
    "question": "Explain the architecture of a neural network"
  }'
```

**Expected Response (200):**
```json
{
  "answer": "According to the course materials, a neural network architecture consists of three main components:\n\n1. Input Layer - This layer receives the input features or data that you want to process.\n\n2. Hidden Layers - These intermediate layers process the information through various transformations and computations.\n\n3. Output Layer - This final layer produces the predictions or outputs of the network.\n\nThe network uses activation functions like ReLU, Sigmoid, Tanh, and Softmax to introduce non-linearity and enable the network to learn complex patterns.",
  "course_id": 1,
  "retrieved_chunks": 4
}
```

**Validation:**
- ✓ Answer references the uploaded content
- ✓ Provides accurate information from materials

---

### Scenario 3: Access Control Testing

#### Test 3.1: Student Cannot Upload Files

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/files/upload/$COURSE_ID" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -F "files=@test.txt"
```

**Expected Response (403):**
```json
{
  "detail": "Only teachers can access this resource"
}
```

**Validation:**
- ✓ Access denied
- ✓ Proper error message

#### Test 3.2: Teacher Cannot Enroll

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/enrollments/" \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"course_id": 1}'
```

**Expected Response (403):**
```json
{
  "detail": "Only students can access this resource"
}
```

**Validation:**
- ✓ Access denied
- ✓ Role enforcement working

#### Test 3.3: Non-Enrolled Student Cannot Chat

Register another student and try to chat without enrolling:

**Request:**
```bash
curl -X POST "http://localhost:8000/api/v1/chat/" \
  -H "Authorization: Bearer $OTHER_STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "course_id": 1,
    "question": "Test question"
  }'
```

**Expected Response (403):**
```json
{
  "detail": "You must be enrolled in this course to chat"
}
```

**Validation:**
- ✓ Enrollment check working
- ✓ Proper authorization

---

### Scenario 4: Cascade Deletion Testing

#### Step 4.1: Verify Data Before Deletion

Check database and file system:

**Course exists:**
```bash
curl -X GET "http://localhost:8000/api/v1/courses/$COURSE_ID" \
  -H "Authorization: Bearer $TEACHER_TOKEN"
```

**Files exist:**
```bash
ls uploads/1/
# Should show uploaded files
```

**ChromaDB has documents:**
```python
import chromadb
client = chromadb.PersistentClient(path="./chroma_db")
collection = client.get_collection("course_materials")
results = collection.get(where={"course_id": 1})
print(f"Documents in ChromaDB: {len(results['ids'])}")
```

**Enrollments exist:**
```bash
curl -X GET "http://localhost:8000/api/v1/enrollments/course/$COURSE_ID" \
  -H "Authorization: Bearer $TEACHER_TOKEN"
```

#### Step 4.2: Delete Course

**Request:**
```bash
curl -X DELETE "http://localhost:8000/api/v1/courses/$COURSE_ID" \
  -H "Authorization: Bearer $TEACHER_TOKEN"
```

**Expected Response (204):**
```
(No content)
```

#### Step 4.3: Verify Cascade Deletion

**Course no longer exists:**
```bash
curl -X GET "http://localhost:8000/api/v1/courses/$COURSE_ID" \
  -H "Authorization: Bearer $TEACHER_TOKEN"
```
Expected: 404 Not Found

**Files deleted:**
```bash
ls uploads/1/
# Directory should not exist or be empty
```

**ChromaDB documents removed:**
```python
import chromadb
client = chromadb.PersistentClient(path="./chroma_db")
collection = client.get_collection("course_materials")
results = collection.get(where={"course_id": 1})
print(f"Documents remaining: {len(results['ids'])}")  # Should be 0
```

**Enrollments deleted:**
```bash
curl -X GET "http://localhost:8000/api/v1/enrollments/my-enrollments" \
  -H "Authorization: Bearer $STUDENT_TOKEN"
```
Expected: Empty array `[]`

**Validation:**
- ✓ Course deleted from database
- ✓ All files deleted from disk
- ✓ All ChromaDB vectors removed
- ✓ All enrollments deleted
- ✓ Complete cascade cleanup

---

## Edge Cases & Error Testing

### Test: Invalid File Type

```bash
curl -X POST "http://localhost:8000/api/v1/files/upload/$COURSE_ID" \
  -H "Authorization: Bearer $TEACHER_TOKEN" \
  -F "files=@image.jpg"
```

**Expected:** 400 Bad Request - Unsupported file type

### Test: Duplicate Enrollment

Try enrolling in the same course twice:

```bash
curl -X POST "http://localhost:8000/api/v1/enrollments/" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"course_id": 1}'
```

**Expected:** 400 Bad Request - Already enrolled

### Test: Invalid JWT

```bash
curl -X GET "http://localhost:8000/api/v1/users/me" \
  -H "Authorization: Bearer invalid_token"
```

**Expected:** 401 Unauthorized

### Test: Missing Authorization

```bash
curl -X GET "http://localhost:8000/api/v1/users/me"
```

**Expected:** 401 Unauthorized

---

## Performance Testing

### Load Test: Multiple File Uploads

Upload 10 files simultaneously:

```bash
for i in {1..10}; do
  curl -X POST "http://localhost:8000/api/v1/files/upload/$COURSE_ID" \
    -H "Authorization: Bearer $TEACHER_TOKEN" \
    -F "files=@test$i.txt" &
done
wait
```

**Validation:**
- ✓ All files processed
- ✓ No database conflicts
- ✓ All documents indexed

### Load Test: Concurrent Chat Requests

Send multiple chat requests:

```bash
for i in {1..5}; do
  curl -X POST "http://localhost:8000/api/v1/chat/" \
    -H "Authorization: Bearer $STUDENT_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"course_id": 1, "question": "What is machine learning?"}' &
done
wait
```

**Validation:**
- ✓ All requests complete
- ✓ Responses are consistent
- ✓ No rate limiting issues (unless configured)

---

## Success Criteria

### Functional Requirements
- [x] User registration (teacher/student)
- [x] JWT authentication
- [x] Course CRUD operations
- [x] Batch file upload
- [x] File processing (PDF/TXT)
- [x] Student enrollment
- [x] RAG chatbot functionality
- [x] Cascade deletion

### Non-Functional Requirements
- [x] API response < 100ms (simple queries)
- [x] Chat response < 10s
- [x] File upload < 5s (1MB file)
- [x] Proper error handling
- [x] Security (JWT, RBAC)
- [x] Clean data lifecycle

### RAG Requirements
- [x] Vector embeddings generated
- [x] ChromaDB storage working
- [x] Similarity search functional
- [x] Course-filtered retrieval
- [x] Relevant answers generated
- [x] Context from uploaded materials

---

## Conclusion

After completing all test scenarios, the application should demonstrate:

1. ✅ Complete user authentication flow
2. ✅ Role-based access control
3. ✅ Course management capabilities
4. ✅ Batch file upload and processing
5. ✅ Vector indexing and storage
6. ✅ RAG-powered Q&A with course filtering
7. ✅ Complete cascade deletion
8. ✅ Proper error handling
9. ✅ Security enforcement

**Result: Production-ready API** 🎉

# Quick Start Guide

## Prerequisites Checklist

- [ ] Python 3.11+ installed
- [ ] PostgreSQL installed and running
- [ ] Ollama installed
- [ ] uv installed (or pip)

## Quick Setup (5 steps)

### 1. Install Ollama Models

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

### 2. Create Database

```bash
# Windows (cmd)
psql -U postgres -c "CREATE DATABASE elearning_db;"

# macOS/Linux
psql -U postgres -c "CREATE DATABASE elearning_db;"
```

### 3. Configure Environment

```bash
# Windows
copy .env.example .env

# macOS/Linux
cp .env.example .env
```

Edit `.env` and set your `DATABASE_URL`:
```
DATABASE_URL=postgresql://postgres:your_password@localhost:5432/elearning_db
```

### 4. Install Dependencies

**Using uv (recommended):**
```bash
uv venv
# Windows
.venv\Scripts\activate
# macOS/Linux
source .venv/bin/activate

uv pip install -e .
```

**Using pip:**
```bash
python -m venv .venv
# Windows
.venv\Scripts\activate
# macOS/Linux
source .venv/bin/activate

pip install -r requirements.txt
```

### 5. Run the Application

```bash
# Direct
python main.py

# Or use startup script
# Windows
run.bat
# macOS/Linux
chmod +x run.sh && ./run.sh
```

## Access the API

- API: http://localhost:8000
- Docs: http://localhost:8000/docs
- Health: http://localhost:8000/health

## First Steps

1. Open http://localhost:8000/docs
2. Use `/api/v1/auth/register` to create a teacher account
3. Use `/api/v1/auth/login` to get a token
4. Click "Authorize" and enter: `Bearer YOUR_TOKEN`
5. Create a course with `/api/v1/courses/`
6. Upload files with `/api/v1/files/upload/{course_id}`
7. Register a student and enroll them
8. Test the chat with `/api/v1/chat/`

## Troubleshooting

**Database connection error:**
- Check PostgreSQL is running
- Verify DATABASE_URL in .env

**Ollama connection error:**
- Run `ollama serve`
- Check OLLAMA_BASE_URL in .env

**Import errors:**
- Reinstall dependencies
- Check virtual environment is activated

## Need Help?

See README.md for detailed documentation.

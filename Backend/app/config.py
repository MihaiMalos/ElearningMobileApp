from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application configuration settings."""
    
    # Database
    DATABASE_URL: str = "postgresql://postgres:1q2w3e@localhost:5432/elearning_db"
    
    # File Storage
    UPLOAD_DIR: str = "./uploads"
    MAX_FILE_SIZE: int = 52428800
    
    # Ollama
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_CHAT_MODEL: str = "qwen2.5:0.5b"
    OLLAMA_EMBEDDING_MODEL: str = "nomic-embed-text"
    OLLAMA_REQUEST_TIMEOUT: int = 120
    
    # ChromaDB
    CHROMA_PERSIST_DIR: str = "./chroma_db"
    CHROMA_COLLECTION_NAME: str = "course_materials"
    
    # RAG
    CHUNK_SIZE: int = 1024
    CHUNK_OVERLAP: int = 200
    TOP_K_RETRIEVAL: int = 5
    SIMILARITY_METRIC: str = "cosine"
    
    # API
    API_V1_PREFIX: str = "/api/v1"
    PROJECT_NAME: str = "E-Learning Platform API"
    DEBUG: bool = True


settings = Settings()

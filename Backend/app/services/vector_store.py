import os
from typing import List, Optional
import chromadb
from chromadb.config import Settings as ChromaSettings
from llama_index.core import Document, VectorStoreIndex, StorageContext
from llama_index.core.node_parser import SentenceSplitter
from llama_index.vector_stores.chroma import ChromaVectorStore
from llama_index.embeddings.ollama import OllamaEmbedding
from llama_index.llms.ollama import Ollama
from llama_index.core import Settings
from app.config import settings as app_settings


class VectorStoreService:
    """Service for managing vector store operations with ChromaDB."""
    
    def __init__(self):
        """Initialize vector store service."""
        self._chroma_client = None
        self._collection = None
        self._vector_store = None
        self._embedding_model = None
        self._llm = None
        self._initialize()
    
    def _initialize(self):
        """Initialize ChromaDB, embedding model, and LLM."""
        # Ensure persist directory exists
        os.makedirs(app_settings.CHROMA_PERSIST_DIR, exist_ok=True)
        
        # Initialize ChromaDB client
        self._chroma_client = chromadb.PersistentClient(
            path=app_settings.CHROMA_PERSIST_DIR,
            settings=ChromaSettings(
                anonymized_telemetry=False,
                allow_reset=True
            )
        )
        
        # Get or create collection
        self._collection = self._chroma_client.get_or_create_collection(
            name=app_settings.CHROMA_COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"}
        )
        
        # Initialize vector store
        self._vector_store = ChromaVectorStore(chroma_collection=self._collection)
        
        # Initialize embedding model
        self._embedding_model = OllamaEmbedding(
            model_name=app_settings.OLLAMA_EMBEDDING_MODEL,
            base_url=app_settings.OLLAMA_BASE_URL,
            request_timeout=app_settings.OLLAMA_REQUEST_TIMEOUT
        )
        
        # Initialize LLM
        self._llm = Ollama(
            model=app_settings.OLLAMA_CHAT_MODEL,
            base_url=app_settings.OLLAMA_BASE_URL,
            request_timeout=app_settings.OLLAMA_REQUEST_TIMEOUT,
            temperature=0.7
        )
        
        # Configure global settings
        Settings.embed_model = self._embedding_model
        Settings.llm = self._llm
        Settings.chunk_size = app_settings.CHUNK_SIZE
        Settings.chunk_overlap = app_settings.CHUNK_OVERLAP
    
    def index_document(
        self,
        text: str,
        course_id: int,
        file_id: int,
        filename: str
    ) -> int:
        """
        Index a document by splitting it into chunks and storing in vector database.
        
        Args:
            text: The text content to index
            course_id: The course ID
            file_id: The file ID
            filename: The original filename
            
        Returns:
            Number of chunks indexed
        """
        # Create document with metadata
        document = Document(
            text=text,
            metadata={
                "course_id": course_id,
                "file_id": file_id,
                "filename": filename
            }
        )
        
        # Split document into chunks
        text_splitter = SentenceSplitter(
            chunk_size=app_settings.CHUNK_SIZE,
            chunk_overlap=app_settings.CHUNK_OVERLAP
        )
        nodes = text_splitter.get_nodes_from_documents([document])
        
        # Create storage context
        storage_context = StorageContext.from_defaults(vector_store=self._vector_store)
        
        # Create index and insert nodes
        index = VectorStoreIndex(
            nodes=[],
            storage_context=storage_context,
            show_progress=False
        )
        
        # Insert nodes
        for node in nodes:
            index.insert_nodes([node])
        
        return len(nodes)
    
    def query_course_materials(
        self,
        query: str,
        course_id: int,
        top_k: Optional[int] = None
    ) -> tuple[str, int]:
        """
        Query course materials using RAG pipeline.
        
        Args:
            query: The user's question
            course_id: The course ID to filter by
            top_k: Number of chunks to retrieve (default from settings)
            
        Returns:
            Tuple of (answer, number of retrieved chunks)
        """
        if top_k is None:
            top_k = app_settings.TOP_K_RETRIEVAL
        
        # Create storage context
        storage_context = StorageContext.from_defaults(vector_store=self._vector_store)
        
        # Create index from existing vector store
        index = VectorStoreIndex.from_vector_store(
            vector_store=self._vector_store,
            storage_context=storage_context
        )
        
        # Create query engine with metadata filters
        query_engine = index.as_query_engine(
            similarity_top_k=top_k,
            filters={"course_id": course_id}
        )
        
        # Execute query
        response = query_engine.query(query)
        
        # Count retrieved source nodes
        retrieved_count = len(response.source_nodes) if hasattr(response, 'source_nodes') else 0
        
        return str(response), retrieved_count
    
    async def query_course_materials_streaming(
        self,
        query: str,
        course_id: int,
        top_k: Optional[int] = None
    ):
        """
        Query course materials using RAG pipeline with streaming response.
        
        Args:
            query: The user's question
            course_id: The course ID to filter by
            top_k: Number of chunks to retrieve (default from settings)
            
        Yields:
            Chunks of the response text
        """
        if top_k is None:
            top_k = app_settings.TOP_K_RETRIEVAL
        
        # Create storage context
        storage_context = StorageContext.from_defaults(vector_store=self._vector_store)
        
        # Create index from existing vector store
        index = VectorStoreIndex.from_vector_store(
            vector_store=self._vector_store,
            storage_context=storage_context
        )
        
        # Create query engine with metadata filters
        query_engine = index.as_query_engine(
            similarity_top_k=top_k,
            filters={"course_id": course_id},
            streaming=True
        )
        
        # Execute query
        response = query_engine.query(query)
        
        # Stream response
        for token in response.response_gen:
            yield token
    
    def delete_course_documents(self, course_id: int) -> int:
        """
        Delete all documents associated with a course.
        
        Args:
            course_id: The course ID
            
        Returns:
            Number of documents deleted
        """
        try:
            # Query all documents for this course
            results = self._collection.get(
                where={"course_id": course_id}
            )
            
            if results and results['ids']:
                # Delete documents by IDs
                self._collection.delete(ids=results['ids'])
                return len(results['ids'])
            
            return 0
        except Exception as e:
            print(f"Error deleting course documents: {e}")
            return 0
    
    def delete_file_documents(self, file_id: int) -> int:
        """
        Delete all documents associated with a file.
        
        Args:
            file_id: The file ID
            
        Returns:
            Number of documents deleted
        """
        try:
            # Query all documents for this file
            results = self._collection.get(
                where={"file_id": file_id}
            )
            
            if results and results['ids']:
                # Delete documents by IDs
                self._collection.delete(ids=results['ids'])
                return len(results['ids'])
            
            return 0
        except Exception as e:
            print(f"Error deleting file documents: {e}")
            return 0
    
    def get_course_document_count(self, course_id: int) -> int:
        """
        Get the number of document chunks indexed for a course.
        
        Args:
            course_id: The course ID
            
        Returns:
            Number of document chunks
        """
        try:
            results = self._collection.get(
                where={"course_id": course_id}
            )
            return len(results['ids']) if results and results['ids'] else 0
        except Exception:
            return 0


# Singleton instance
vector_store_service = VectorStoreService()

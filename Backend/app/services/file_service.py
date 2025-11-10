import os
import uuid
import aiofiles
from typing import List, Tuple
from fastapi import UploadFile, HTTPException, status
from pypdf import PdfReader
from io import BytesIO
from app.config import settings
from app.services.vector_store import vector_store_service


class FileService:
    """Service for handling file uploads and text extraction."""
    
    ALLOWED_EXTENSIONS = {'.pdf', '.txt'}
    ALLOWED_MIME_TYPES = {
        'application/pdf',
        'text/plain',
        'application/octet-stream'  # Sometimes PDFs are uploaded as this
    }
    
    def __init__(self):
        """Initialize file service."""
        os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
    
    def _validate_file(self, file: UploadFile) -> None:
        """
        Validate uploaded file.
        
        Args:
            file: The uploaded file
            
        Raises:
            HTTPException: If file is invalid
        """
        # Check file extension
        file_ext = os.path.splitext(file.filename)[1].lower()
        if file_ext not in self.ALLOWED_EXTENSIONS:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"File type not supported. Allowed types: {', '.join(self.ALLOWED_EXTENSIONS)}"
            )
        
        # Check MIME type (optional, as it can be unreliable)
        if file.content_type and file.content_type not in self.ALLOWED_MIME_TYPES:
            # Be lenient with content type checking
            pass
    
    def _generate_unique_filename(self, original_filename: str) -> str:
        """
        Generate a unique filename.
        
        Args:
            original_filename: The original filename
            
        Returns:
            Unique filename
        """
        file_ext = os.path.splitext(original_filename)[1].lower()
        unique_name = f"{uuid.uuid4()}{file_ext}"
        return unique_name
    
    async def save_file(
        self,
        file: UploadFile,
        course_id: int
    ) -> Tuple[str, str, int]:
        """
        Save uploaded file to disk.
        
        Args:
            file: The uploaded file
            course_id: The course ID
            
        Returns:
            Tuple of (file_path, unique_filename, file_size)
        """
        # Validate file
        self._validate_file(file)
        
        # Create course directory
        course_dir = os.path.join(settings.UPLOAD_DIR, str(course_id))
        os.makedirs(course_dir, exist_ok=True)
        
        # Generate unique filename
        unique_filename = self._generate_unique_filename(file.filename)
        file_path = os.path.join(course_dir, unique_filename)
        
        # Save file
        file_size = 0
        async with aiofiles.open(file_path, 'wb') as f:
            content = await file.read()
            file_size = len(content)
            
            # Check file size
            if file_size > settings.MAX_FILE_SIZE:
                raise HTTPException(
                    status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                    detail=f"File too large. Maximum size: {settings.MAX_FILE_SIZE} bytes"
                )
            
            await f.write(content)
        
        return file_path, unique_filename, file_size
    
    def extract_text(self, file_path: str) -> str:
        """
        Extract text from a file.
        
        Args:
            file_path: Path to the file
            
        Returns:
            Extracted text content
            
        Raises:
            HTTPException: If text extraction fails
        """
        file_ext = os.path.splitext(file_path)[1].lower()
        
        try:
            if file_ext == '.pdf':
                return self._extract_text_from_pdf(file_path)
            elif file_ext == '.txt':
                return self._extract_text_from_txt(file_path)
            else:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Unsupported file type: {file_ext}"
                )
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to extract text from file: {str(e)}"
            )
    
    def _extract_text_from_pdf(self, file_path: str) -> str:
        """
        Extract text from PDF file.
        
        Args:
            file_path: Path to the PDF file
            
        Returns:
            Extracted text
        """
        text_content = []
        
        with open(file_path, 'rb') as f:
            pdf_reader = PdfReader(f)
            
            for page in pdf_reader.pages:
                text = page.extract_text()
                if text:
                    text_content.append(text)
        
        return '\n\n'.join(text_content)
    
    def _extract_text_from_txt(self, file_path: str) -> str:
        """
        Extract text from TXT file.
        
        Args:
            file_path: Path to the TXT file
            
        Returns:
            Extracted text
        """
        with open(file_path, 'r', encoding='utf-8') as f:
            return f.read()
    
    async def process_and_index_file(
        self,
        file: UploadFile,
        course_id: int,
        file_id: int
    ) -> Tuple[str, str, int, int]:
        """
        Process file: save, extract text, and index in vector store.
        
        Args:
            file: The uploaded file
            course_id: The course ID
            file_id: The file ID from database
            
        Returns:
            Tuple of (file_path, unique_filename, file_size, chunks_indexed)
        """
        # Save file
        file_path, unique_filename, file_size = await self.save_file(file, course_id)
        
        try:
            # Extract text
            text_content = self.extract_text(file_path)
            
            # Index in vector store
            chunks_indexed = vector_store_service.index_document(
                text=text_content,
                course_id=course_id,
                file_id=file_id,
                filename=file.filename
            )
            
            return file_path, unique_filename, file_size, chunks_indexed
        
        except Exception as e:
            # Clean up file if indexing fails
            if os.path.exists(file_path):
                os.remove(file_path)
            raise e
    
    def delete_file(self, file_path: str) -> None:
        """
        Delete a file from disk.
        
        Args:
            file_path: Path to the file
        """
        try:
            if os.path.exists(file_path):
                os.remove(file_path)
        except Exception as e:
            print(f"Error deleting file {file_path}: {e}")
    
    def delete_course_files(self, course_id: int) -> None:
        """
        Delete all files for a course.
        
        Args:
            course_id: The course ID
        """
        course_dir = os.path.join(settings.UPLOAD_DIR, str(course_id))
        try:
            if os.path.exists(course_dir):
                import shutil
                shutil.rmtree(course_dir)
        except Exception as e:
            print(f"Error deleting course directory {course_dir}: {e}")


# Singleton instance
file_service = FileService()

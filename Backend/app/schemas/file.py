from datetime import datetime
from pydantic import BaseModel


class CourseMaterialFileResponse(BaseModel):
    """Course material file response schema."""
    id: int
    course_id: int
    filename: str
    original_filename: str
    file_size: int
    mime_type: str
    uploaded_at: datetime
    
    class Config:
        from_attributes = True


class FileUploadResponse(BaseModel):
    """File upload response schema."""
    uploaded_files: list[CourseMaterialFileResponse]
    total_files: int
    failed_files: list[str] = []

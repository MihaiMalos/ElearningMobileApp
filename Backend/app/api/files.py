from typing import List
from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User
from app.schemas.file import CourseMaterialFileResponse, FileUploadResponse
from app.repositories.course_repository import CourseRepository
from app.repositories.file_repository import CourseMaterialFileRepository
from app.utils.security import get_current_teacher
from app.services.file_service import file_service
from app.services.vector_store import vector_store_service

router = APIRouter(prefix="/files", tags=["Course Materials"])


@router.post("/upload/{course_id}", response_model=FileUploadResponse)
async def upload_course_materials(
    course_id: int,
    files: List[UploadFile] = File(...),
    current_user: User = Depends(get_current_teacher),
    db: Session = Depends(get_db)
):
    """
    Upload multiple course material files (teachers only).
    Supports batch upload of PDF and TXT files.
    """
    course_repo = CourseRepository(db)
    file_repo = CourseMaterialFileRepository(db)
    
    # Check if course exists and user is the teacher
    course = course_repo.get_by_id(course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    if course.teacher_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the course teacher can upload materials"
        )
    
    uploaded_files = []
    failed_files = []
    
    for file in files:
        try:
            # Create file record first to get ID
            db_file = file_repo.create(
                course_id=course_id,
                filename="",  # Will be updated after processing
                original_filename=file.filename,
                file_path="",  # Will be updated after processing
                file_size=0,  # Will be updated after processing
                mime_type=file.content_type or "application/octet-stream"
            )
            
            # Process and index file
            file_path, unique_filename, file_size, chunks_indexed = await file_service.process_and_index_file(
                file=file,
                course_id=course_id,
                file_id=db_file.id
            )
            
            # Update file record with actual values
            db_file.filename = unique_filename
            db_file.file_path = file_path
            db_file.file_size = file_size
            db.commit()
            db.refresh(db_file)
            
            uploaded_files.append(db_file)
            
        except Exception as e:
            failed_files.append(f"{file.filename}: {str(e)}")
            # Rollback this file's database entry if it was created
            db.rollback()
            continue
    
    return FileUploadResponse(
        uploaded_files=uploaded_files,
        total_files=len(uploaded_files),
        failed_files=failed_files
    )


@router.get("/course/{course_id}", response_model=List[CourseMaterialFileResponse])
def list_course_materials(
    course_id: int,
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_teacher),
    db: Session = Depends(get_db)
):
    """List all materials for a course (teacher only)."""
    course_repo = CourseRepository(db)
    file_repo = CourseMaterialFileRepository(db)
    
    # Check if course exists and user is the teacher
    course = course_repo.get_by_id(course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    if course.teacher_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the course teacher can view materials"
        )
    
    files = file_repo.get_by_course(course_id, skip=skip, limit=limit)
    return files


@router.delete("/{file_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_course_material(
    file_id: int,
    current_user: User = Depends(get_current_teacher),
    db: Session = Depends(get_db)
):
    """Delete a course material file (teacher only)."""
    course_repo = CourseRepository(db)
    file_repo = CourseMaterialFileRepository(db)
    
    # Get file
    db_file = file_repo.get_by_id(file_id)
    if not db_file:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="File not found"
        )
    
    # Check if user is the course teacher
    course = course_repo.get_by_id(db_file.course_id)
    if course.teacher_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the course teacher can delete materials"
        )
    
    # Delete from vector store
    vector_store_service.delete_file_documents(file_id)
    
    # Delete physical file
    file_service.delete_file(db_file.file_path)
    
    # Delete database record
    file_repo.delete(db_file)
    
    return None

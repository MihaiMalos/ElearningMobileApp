from typing import List, Optional
from sqlalchemy.orm import Session
from app.models.course_material_file import CourseMaterialFile


class CourseMaterialFileRepository:
    """Repository for CourseMaterialFile entity operations."""
    
    def __init__(self, db: Session):
        self.db = db
    
    def get_by_id(self, file_id: int) -> Optional[CourseMaterialFile]:
        """Get file by ID."""
        return (
            self.db.query(CourseMaterialFile)
            .filter(CourseMaterialFile.id == file_id)
            .first()
        )
    
    def get_by_course(
        self,
        course_id: int,
        skip: int = 0,
        limit: int = 100
    ) -> List[CourseMaterialFile]:
        """Get all files for a course."""
        return (
            self.db.query(CourseMaterialFile)
            .filter(CourseMaterialFile.course_id == course_id)
            .offset(skip)
            .limit(limit)
            .all()
        )
    
    def count_by_course(self, course_id: int) -> int:
        """Count files for a course."""
        return (
            self.db.query(CourseMaterialFile)
            .filter(CourseMaterialFile.course_id == course_id)
            .count()
        )
    
    def create(
        self,
        course_id: int,
        filename: str,
        original_filename: str,
        file_path: str,
        file_size: int,
        mime_type: str
    ) -> CourseMaterialFile:
        """Create a new file record."""
        db_file = CourseMaterialFile(
            course_id=course_id,
            filename=filename,
            original_filename=original_filename,
            file_path=file_path,
            file_size=file_size,
            mime_type=mime_type
        )
        self.db.add(db_file)
        self.db.commit()
        self.db.refresh(db_file)
        return db_file
    
    def delete(self, file: CourseMaterialFile) -> None:
        """Delete a file record."""
        self.db.delete(file)
        self.db.commit()

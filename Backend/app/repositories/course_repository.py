from typing import List, Optional
from sqlalchemy.orm import Session
from app.models.course import Course


class CourseRepository:
    """Repository for Course entity operations."""
    
    def __init__(self, db: Session):
        self.db = db
    
    def get_by_id(self, course_id: int) -> Optional[Course]:
        """Get course by ID."""
        return self.db.query(Course).filter(Course.id == course_id).first()
    
    def get_all(self, skip: int = 0, limit: int = 100) -> List[Course]:
        """Get all courses with pagination."""
        return self.db.query(Course).offset(skip).limit(limit).all()
    
    def get_by_teacher(
        self,
        teacher_id: int,
        skip: int = 0,
        limit: int = 100
    ) -> List[Course]:
        """Get courses by teacher ID with pagination."""
        return (
            self.db.query(Course)
            .filter(Course.teacher_id == teacher_id)
            .offset(skip)
            .limit(limit)
            .all()
        )
    
    def search_by_title(
        self,
        search_term: str,
        skip: int = 0,
        limit: int = 100
    ) -> List[Course]:
        """Search courses by title."""
        return (
            self.db.query(Course)
            .filter(Course.title.ilike(f"%{search_term}%"))
            .offset(skip)
            .limit(limit)
            .all()
        )
    
    def create(
        self,
        title: str,
        teacher_id: int,
        description: Optional[str] = None
    ) -> Course:
        """Create a new course."""
        db_course = Course(
            title=title,
            description=description,
            teacher_id=teacher_id
        )
        self.db.add(db_course)
        self.db.commit()
        self.db.refresh(db_course)
        return db_course
    
    def update(
        self,
        course: Course,
        title: Optional[str] = None,
        description: Optional[str] = None
    ) -> Course:
        """Update course information."""
        if title is not None:
            course.title = title
        if description is not None:
            course.description = description
        
        self.db.commit()
        self.db.refresh(course)
        return course
    
    def delete(self, course: Course) -> None:
        """Delete a course (cascades to enrollments and files)."""
        self.db.delete(course)
        self.db.commit()

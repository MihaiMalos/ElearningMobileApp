from typing import List, Optional
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from fastapi import HTTPException, status
from app.models.enrollment import Enrollment


class EnrollmentRepository:
    """Repository for Enrollment entity operations."""
    
    def __init__(self, db: Session):
        self.db = db
    
    def get_by_id(self, enrollment_id: int) -> Optional[Enrollment]:
        """Get enrollment by ID."""
        return self.db.query(Enrollment).filter(Enrollment.id == enrollment_id).first()
    
    def get_by_student_and_course(
        self,
        student_id: int,
        course_id: int
    ) -> Optional[Enrollment]:
        """Get enrollment by student and course."""
        return (
            self.db.query(Enrollment)
            .filter(
                Enrollment.student_id == student_id,
                Enrollment.course_id == course_id
            )
            .first()
        )
    
    def get_by_student(
        self,
        student_id: int,
        skip: int = 0,
        limit: int = 100
    ) -> List[Enrollment]:
        """Get all enrollments for a student."""
        return (
            self.db.query(Enrollment)
            .filter(Enrollment.student_id == student_id)
            .offset(skip)
            .limit(limit)
            .all()
        )
    
    def get_by_course(
        self,
        course_id: int,
        skip: int = 0,
        limit: int = 100
    ) -> List[Enrollment]:
        """Get all enrollments for a course."""
        return (
            self.db.query(Enrollment)
            .filter(Enrollment.course_id == course_id)
            .offset(skip)
            .limit(limit)
            .all()
        )
    
    def is_student_enrolled(self, student_id: int, course_id: int) -> bool:
        """Check if student is enrolled in course."""
        return self.get_by_student_and_course(student_id, course_id) is not None
    
    def create(self, student_id: int, course_id: int) -> Enrollment:
        """Create a new enrollment."""
        # Check if already enrolled
        existing = self.get_by_student_and_course(student_id, course_id)
        if existing:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Student is already enrolled in this course"
            )
        
        db_enrollment = Enrollment(
            student_id=student_id,
            course_id=course_id
        )
        
        try:
            self.db.add(db_enrollment)
            self.db.commit()
            self.db.refresh(db_enrollment)
            return db_enrollment
        except IntegrityError:
            self.db.rollback()
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Student is already enrolled in this course"
            )
    
    def delete(self, enrollment: Enrollment) -> None:
        """Delete an enrollment."""
        self.db.delete(enrollment)
        self.db.commit()
    
    def delete_by_student_and_course(self, student_id: int, course_id: int) -> bool:
        """Delete enrollment by student and course. Returns True if deleted."""
        enrollment = self.get_by_student_and_course(student_id, course_id)
        if enrollment:
            self.delete(enrollment)
            return True
        return False

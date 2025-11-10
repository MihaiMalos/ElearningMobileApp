from datetime import datetime
from sqlalchemy import Column, Integer, DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.orm import relationship
from app.database import Base


class Enrollment(Base):
    """Enrollment model representing student enrollments in courses."""
    
    __tablename__ = "enrollments"
    
    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    course_id = Column(Integer, ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    enrolled_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    
    # Relationships
    student = relationship("User", back_populates="enrollments")
    course = relationship("Course", back_populates="enrollments")
    
    # Constraints
    __table_args__ = (
        UniqueConstraint('student_id', 'course_id', name='unique_student_course'),
    )
    
    def __repr__(self):
        return f"<Enrollment student_id={self.student_id} course_id={self.course_id}>"

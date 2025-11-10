from datetime import datetime
from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, BigInteger
from sqlalchemy.orm import relationship
from app.database import Base


class CourseMaterialFile(Base):
    """Course material file model representing uploaded files."""
    
    __tablename__ = "course_material_files"
    
    id = Column(Integer, primary_key=True, index=True)
    course_id = Column(Integer, ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    filename = Column(String, nullable=False)
    original_filename = Column(String, nullable=False)
    file_path = Column(String, nullable=False)
    file_size = Column(BigInteger, nullable=False)
    mime_type = Column(String, nullable=False)
    uploaded_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    
    # Relationships
    course = relationship("Course", back_populates="material_files")
    
    def __repr__(self):
        return f"<CourseMaterialFile {self.filename} course_id={self.course_id}>"

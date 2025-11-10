from datetime import datetime
from pydantic import BaseModel


class EnrollmentBase(BaseModel):
    """Base enrollment schema."""
    course_id: int


class EnrollmentCreate(EnrollmentBase):
    """Enrollment creation schema."""
    pass


class EnrollmentResponse(BaseModel):
    """Enrollment response schema."""
    id: int
    student_id: int
    course_id: int
    enrolled_at: datetime
    
    class Config:
        from_attributes = True


class EnrollmentDetailResponse(EnrollmentResponse):
    """Detailed enrollment response with course info."""
    course_title: str | None = None
    course_description: str | None = None
    teacher_username: str | None = None

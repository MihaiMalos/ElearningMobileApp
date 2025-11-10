from datetime import datetime
from pydantic import BaseModel, Field


class CourseBase(BaseModel):
    """Base course schema."""
    title: str = Field(..., min_length=1, max_length=200)
    description: str | None = None


class CourseCreate(CourseBase):
    """Course creation schema."""
    pass


class CourseUpdate(BaseModel):
    """Course update schema."""
    title: str | None = Field(None, min_length=1, max_length=200)
    description: str | None = None


class CourseResponse(CourseBase):
    """Course response schema."""
    id: int
    teacher_id: int
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True


class CourseDetailResponse(CourseResponse):
    """Detailed course response with teacher info."""
    teacher_username: str | None = None
    materials_count: int = 0
    enrollments_count: int = 0

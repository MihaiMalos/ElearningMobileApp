from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    """Chat request schema."""
    course_id: int = Field(..., gt=0)
    question: str = Field(..., min_length=1, max_length=2000)


class ChatResponse(BaseModel):
    """Chat response schema."""
    answer: str
    course_id: int
    retrieved_chunks: int = 0

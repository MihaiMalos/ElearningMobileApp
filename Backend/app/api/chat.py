from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User
from app.schemas.chat import ChatRequest, ChatResponse
from app.repositories.course_repository import CourseRepository
from app.repositories.enrollment_repository import EnrollmentRepository
from app.utils.security import get_current_student, get_current_user
from app.services.vector_store import vector_store_service

router = APIRouter(prefix="/chat", tags=["AI Chat"])


@router.post("/", response_model=ChatResponse)
def chat_with_course_materials(
    chat_request: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Chat with course materials using RAG pipeline.
    Student must be enrolled in the course to chat.
    """
    course_repo = CourseRepository(db)
    enrollment_repo = EnrollmentRepository(db)
    
    # Check if course exists
    course = course_repo.get_by_id(chat_request.course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    # Check if student is enrolled
    if not enrollment_repo.is_student_enrolled(current_user.id, chat_request.course_id):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You must be enrolled in this course to chat"
        )
    
    # Query RAG pipeline
    try:
        answer, retrieved_count = vector_store_service.query_course_materials(
            query=chat_request.question,
            course_id=chat_request.course_id
        )
        
        return ChatResponse(
            answer=answer,
            course_id=chat_request.course_id,
            retrieved_chunks=retrieved_count
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing chat request: {str(e)}"
        )


@router.post("/stream")
async def chat_with_course_materials_streaming(
    chat_request: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Chat with course materials using RAG pipeline with streaming response.
    Student must be enrolled in the course to chat.
    """
    course_repo = CourseRepository(db)
    enrollment_repo = EnrollmentRepository(db)
    
    # Check if course exists
    course = course_repo.get_by_id(chat_request.course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    # Check if student is enrolled
    if not enrollment_repo.is_student_enrolled(current_user.id, chat_request.course_id):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You must be enrolled in this course to chat"
        )
    
    # Query RAG pipeline with streaming
    try:
        async def generate():
            async for chunk in vector_store_service.query_course_materials_streaming(
                query=chat_request.question,
                course_id=chat_request.course_id
            ):
                yield chunk
        
        return StreamingResponse(generate(), media_type="text/plain")
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error processing chat request: {str(e)}"
        )

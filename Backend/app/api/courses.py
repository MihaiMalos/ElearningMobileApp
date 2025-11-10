from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User
from app.schemas.course import CourseCreate, CourseResponse, CourseUpdate, CourseDetailResponse
from app.repositories.course_repository import CourseRepository
from app.repositories.enrollment_repository import EnrollmentRepository
from app.repositories.file_repository import CourseMaterialFileRepository
from app.utils.security import get_current_user, get_current_teacher
from app.services.vector_store import vector_store_service
from app.services.file_service import file_service

router = APIRouter(prefix="/courses", tags=["Courses"])


@router.post("/", response_model=CourseResponse, status_code=status.HTTP_201_CREATED)
def create_course(
    course_data: CourseCreate,
    current_user: User = Depends(get_current_teacher),
    db: Session = Depends(get_db)
):
    """Create a new course (teachers only)."""
    course_repo = CourseRepository(db)
    
    course = course_repo.create(
        title=course_data.title,
        description=course_data.description,
        teacher_id=current_user.id
    )
    
    return course


@router.get("/", response_model=List[CourseResponse])
def list_courses(
    skip: int = 0,
    limit: int = 100,
    search: str = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """List all courses (paginated, with optional search)."""
    course_repo = CourseRepository(db)
    
    if search:
        courses = course_repo.search_by_title(search, skip=skip, limit=limit)
    else:
        courses = course_repo.get_all(skip=skip, limit=limit)
    
    return courses


@router.get("/my-courses", response_model=List[CourseResponse])
def list_my_courses(
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_teacher),
    db: Session = Depends(get_db)
):
    """List courses created by current teacher."""
    course_repo = CourseRepository(db)
    courses = course_repo.get_by_teacher(current_user.id, skip=skip, limit=limit)
    return courses


@router.get("/{course_id}", response_model=CourseDetailResponse)
def get_course(
    course_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get course details by ID."""
    course_repo = CourseRepository(db)
    file_repo = CourseMaterialFileRepository(db)
    enrollment_repo = EnrollmentRepository(db)
    
    course = course_repo.get_by_id(course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    # Get additional details
    materials_count = file_repo.count_by_course(course_id)
    enrollments_count = len(enrollment_repo.get_by_course(course_id))
    
    return CourseDetailResponse(
        **course.__dict__,
        teacher_username=course.teacher.username,
        materials_count=materials_count,
        enrollments_count=enrollments_count
    )


@router.put("/{course_id}", response_model=CourseResponse)
def update_course(
    course_id: int,
    course_data: CourseUpdate,
    current_user: User = Depends(get_current_teacher),
    db: Session = Depends(get_db)
):
    """Update a course (only by the teacher who created it)."""
    course_repo = CourseRepository(db)
    
    course = course_repo.get_by_id(course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    # Check if current user is the course teacher
    if course.teacher_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the course teacher can update this course"
        )
    
    # Update course
    updated_course = course_repo.update(
        course=course,
        title=course_data.title,
        description=course_data.description
    )
    
    return updated_course


@router.delete("/{course_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_course(
    course_id: int,
    current_user: User = Depends(get_current_teacher),
    db: Session = Depends(get_db)
):
    """Delete a course with cascade (only by the teacher who created it)."""
    course_repo = CourseRepository(db)
    
    course = course_repo.get_by_id(course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    # Check if current user is the course teacher
    if course.teacher_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the course teacher can delete this course"
        )
    
    # Delete vector store documents
    vector_store_service.delete_course_documents(course_id)
    
    # Delete physical files
    file_service.delete_course_files(course_id)
    
    # Delete course (cascades to enrollments and file records)
    course_repo.delete(course)
    
    return None

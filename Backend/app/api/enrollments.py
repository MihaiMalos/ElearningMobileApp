from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User, UserRole
from app.schemas.enrollment import EnrollmentCreate, EnrollmentResponse, EnrollmentDetailResponse
from app.repositories.enrollment_repository import EnrollmentRepository
from app.repositories.course_repository import CourseRepository
from app.utils.security import get_current_user, get_current_student

router = APIRouter(prefix="/enrollments", tags=["Enrollments"])


@router.post("/", response_model=EnrollmentResponse, status_code=status.HTTP_201_CREATED)
def enroll_in_course(
    enrollment_data: EnrollmentCreate,
    current_user: User = Depends(get_current_student),
    db: Session = Depends(get_db)
):
    """Enroll in a course (students only)."""
    course_repo = CourseRepository(db)
    enrollment_repo = EnrollmentRepository(db)
    
    # Check if course exists
    course = course_repo.get_by_id(enrollment_data.course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    # Create enrollment
    enrollment = enrollment_repo.create(
        student_id=current_user.id,
        course_id=enrollment_data.course_id
    )
    
    return enrollment


@router.get("/my-enrollments", response_model=List[EnrollmentDetailResponse])
def get_my_enrollments(
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_student),
    db: Session = Depends(get_db)
):
    """Get enrollments for current student."""
    enrollment_repo = EnrollmentRepository(db)
    enrollments = enrollment_repo.get_by_student(current_user.id, skip=skip, limit=limit)
    
    # Build detailed response
    detailed_enrollments = []
    for enrollment in enrollments:
        detailed_enrollments.append(
            EnrollmentDetailResponse(
                **enrollment.__dict__,
                course_title=enrollment.course.title,
                course_description=enrollment.course.description,
                teacher_username=enrollment.course.teacher.username
            )
        )
    
    return detailed_enrollments


@router.get("/course/{course_id}", response_model=List[EnrollmentResponse])
def get_course_enrollments(
    course_id: int,
    skip: int = 0,
    limit: int = 100,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get enrollments for a course."""
    course_repo = CourseRepository(db)
    enrollment_repo = EnrollmentRepository(db)
    
    # Check if course exists
    course = course_repo.get_by_id(course_id)
    if not course:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Course not found"
        )
    
    # Only teacher of the course can view enrollments
    if current_user.role == UserRole.TEACHER and course.teacher_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the course teacher can view enrollments"
        )
    
    enrollments = enrollment_repo.get_by_course(course_id, skip=skip, limit=limit)
    return enrollments


@router.delete("/{enrollment_id}", status_code=status.HTTP_204_NO_CONTENT)
def unenroll_from_course(
    enrollment_id: int,
    current_user: User = Depends(get_current_student),
    db: Session = Depends(get_db)
):
    """Unenroll from a course (students only)."""
    enrollment_repo = EnrollmentRepository(db)
    
    enrollment = enrollment_repo.get_by_id(enrollment_id)
    if not enrollment:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Enrollment not found"
        )
    
    # Check if current user is the enrolled student
    if enrollment.student_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You can only unenroll yourself"
        )
    
    enrollment_repo.delete(enrollment)
    return None

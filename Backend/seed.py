import os
import shutil
import asyncio
from datetime import datetime
import uuid
from fastapi import UploadFile
from app.database import SessionLocal, init_db
from app.models.user import User, UserRole
from app.models.course import Course
from app.models.enrollment import Enrollment
from app.models.course_material_file import CourseMaterialFile
from app.utils.security import get_password_hash
from app.services.file_service import file_service  # Import the service

async def seed_data_async():
    db = SessionLocal()
    
    # Reset Database
    print("Dropping all tables to ensure clean seed...")
    try:
        from app.database import engine, Base
        Base.metadata.drop_all(bind=engine)
        Base.metadata.create_all(bind=engine)
    except Exception as e:
        print(f"Error resetting database: {e}")

    print("Seeding data...")

    # Create Teachers
    teachers = []
    for i in range(1, 5):
        teacher = User(
            email=f"teacher{i}@example.com",
            username=f"teacher{i}",
            hashed_password=get_password_hash("password123"),
            role=UserRole.TEACHER
        )
        db.add(teacher)
        teachers.append(teacher)
    
    db.commit() # Commit to get IDs
    for t in teachers: db.refresh(t)

    # Create Students
    students = []
    for i in range(1, 6):
        student = User(
            email=f"student{i}@example.com",
            username=f"student{i}",
            hashed_password=get_password_hash("password123"),
            role=UserRole.STUDENT
        )
        db.add(student)
        students.append(student)

    db.commit()
    for s in students: db.refresh(s)

    # Create Courses
    courses_data = [
        {"title": "Introduction to Python", "desc": "Learn the basics of Python programming.", "teacher": teachers[0]},
        {"title": "Advanced Python", "desc": "Deep dive into Python features.", "teacher": teachers[0]},
        {"title": "Data Science with Python", "desc": "Analyze data using Python libraries.", "teacher": teachers[0]},
        {"title": "Web Development Fundamentals", "desc": "HTML, CSS, and JS basics.", "teacher": teachers[1]},
        {"title": "Database Design", "desc": "SQL and NoSQL database concepts.", "teacher": teachers[2]},
        {"title": "Machine Learning 101", "desc": "Introduction to ML algorithms.", "teacher": teachers[3]},
    ]
    
    courses = []
    for data in courses_data:
        course = Course(
            title=data["title"],
            description=data["desc"],
            teacher_id=data["teacher"].id
        )
        db.add(course)
        courses.append(course)
        
    db.commit()
    for c in courses: db.refresh(c)

    # Enroll Students
    enrollments_data = [
        # Student 1 enrolled in 3 courses
        (students[0], courses[0]), (students[0], courses[3]), (students[0], courses[5]),
        # Student 2 enrolled in 2 courses
        (students[1], courses[1]), (students[1], courses[4]),
        # Student 3 enrolled in 2 courses
        (students[2], courses[0]), (students[2], courses[2]),
        # Student 4 enrolled in 1 course
        (students[3], courses[3]),
        # Student 5 enrolled in all teacher 1's courses
        (students[4], courses[0]), (students[4], courses[1]), (students[4], courses[2]),
    ]

    for student, course in enrollments_data:
        enrollment = Enrollment(student_id=student.id, course_id=course.id)
        db.add(enrollment)

    db.commit()
    
    # Create Dummy Files -> Real Files
    # Ensure uploads directory exists
    uploads_dir = "uploads"
    # Clean uploads directory
    if os.path.exists(uploads_dir):
        shutil.rmtree(uploads_dir)
    os.makedirs(uploads_dir)
    
    seed_content_dir = "seed_content"

    # Map courses to files
    course_files_map = {
        0: ["intro_python_vars.txt", "intro_python_control.txt"],
        1: ["adv_python_decorators.txt", "adv_python_generators.txt"],
        2: ["ds_python_pandas.txt", "ds_python_numpy.txt"],
        3: ["web_dev_html.txt", "web_dev_css.txt"],
        4: ["db_design_norm.txt", "db_design_sql.txt"],
        5: ["ml_101_regression.txt", "ml_101_nn.txt"]
    }

    print("Processing and indexing files...")
    # Add files to courses
    for course_idx, filenames in course_files_map.items():
        if course_idx >= len(courses):
            continue
            
        course = courses[course_idx]
        
        for filename in filenames:
            src_path = os.path.join(seed_content_dir, filename)
            if not os.path.exists(src_path):
                print(f"Warning: Source file {src_path} not found.")
                continue

            # Create initial DB record to get an ID
            db_file = CourseMaterialFile(
                course_id=course.id,
                filename="", 
                original_filename=filename,
                file_path="", 
                file_size=0,
                mime_type="text/plain"
            )
            db.add(db_file)
            db.commit() # Commit to generate ID
            db.refresh(db_file)

            # Create an UploadFile-like object or open file for the service
            # process_and_index_file expects an UploadFile object usually, 
            # let's mock it or adapt the calling logic.
            # Assuming we can just pass the open file handle if the service supports it,
            # but usually UploadFile is required by FastAPI services.
            
            with open(src_path, 'rb') as f:
                # Create a mock UploadFile
                upload_file = UploadFile(
                    filename=filename, 
                    file=f, 
                    headers={"content-type": "text/plain"}
                )

                try:
                    # Use the service to process, save, and index
                    print(f"  Indexing {filename} for course {course.id}...")
                    file_path, unique_filename, file_size, chunks = await file_service.process_and_index_file(  
                        file=upload_file,
                        course_id=course.id,
                        file_id=db_file.id
                    )
                    
                    # Update DB record
                    db_file.filename = unique_filename
                    db_file.file_path = file_path
                    db_file.file_size = file_size
                    db.commit()
                except Exception as e:
                    print(f"  Error processing {filename}: {e}")
                    db.delete(db_file)
                    db.commit()

    db.commit()
    print("Database seeded successfully with embeddings!")
    db.close()

def seed_data():
    asyncio.run(seed_data_async())

if __name__ == "__main__":
    init_db()
    seed_data()

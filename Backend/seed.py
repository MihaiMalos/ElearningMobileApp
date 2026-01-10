import os
import shutil
from datetime import datetime
import uuid
from app.database import SessionLocal, init_db
from app.models.user import User, UserRole
from app.models.course import Course
from app.models.enrollment import Enrollment
from app.models.course_material_file import CourseMaterialFile
from app.utils.security import get_password_hash

def seed_data():
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
    # Teacher 1 has 3 courses
    # Teacher 2, 3, 4 have 1 course each
    
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
    # Distribute enrollments randomly or manually
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
    # Only mapping files for the courses we created
    # 0: Intro to Python
    # 1: Advanced Python
    # 2: Data Science with Python
    # 3: Web Dev
    # 4: DB Design
    # 5: ML 101
    
    course_files_map = {
        0: ["intro_python_vars.txt", "intro_python_control.txt"],
        1: ["adv_python_decorators.txt", "adv_python_generators.txt"],
        2: ["ds_python_pandas.txt", "ds_python_numpy.txt"],
        3: ["web_dev_html.txt", "web_dev_css.txt"],
        4: ["db_design_norm.txt", "db_design_sql.txt"],
        5: ["ml_101_regression.txt", "ml_101_nn.txt"]
    }

    # Add files to courses
    for course_idx, filenames in course_files_map.items():
        if course_idx >= len(courses):
            continue
            
        course = courses[course_idx]
        course_upload_dir = os.path.join(uploads_dir, str(course.id))
        
        if not os.path.exists(course_upload_dir):
            os.makedirs(course_upload_dir)
            
        for filename in filenames:
            src_path = os.path.join(seed_content_dir, filename)
            if not os.path.exists(src_path):
                print(f"Warning: Source file {src_path} not found.")
                continue

            file_uuid = str(uuid.uuid4())
            # Copy file to upload dir with uuid name
            dest_path = os.path.join(course_upload_dir, f"{file_uuid}.txt")
            shutil.copy2(src_path, dest_path)
            
            file_size = os.path.getsize(dest_path)
            
            db_file = CourseMaterialFile(
                course_id=course.id,
                filename=filename, # Keep readable filename in DB
                original_filename=filename,
                file_path=dest_path,
                file_size=file_size,
                mime_type="text/plain"
            )
            db.add(db_file)

    db.commit()
    print("Database seeded successfully!")
    db.close()

if __name__ == "__main__":
    init_db()
    seed_data()

import sqlite3
import csv
import os

# Define the database file path
db_file = '15-06-2025-dump.sqlite3'

# Connect to the SQLite database
conn = sqlite3.connect(db_file)
conn.row_factory = sqlite3.Row  # Enables column access by name
cursor = conn.cursor()

# Modified SQL query to match the required CSV format
query = """
SELECT 
    tc.student_count,
    tc.academic_year,
    tc.semester,
    tc.requires_special_scheduling,
    CASE 
        WHEN tc.asssist_teacher_id IS NOT NULL THEN 1
        WHEN tc.asssist_teacher_2_id IS NOT NULL THEN 1
        WHEN tc.asssist_teacher_3_id IS NOT NULL THEN 1
        ELSE 0
    END AS is_assistant,
    t.id AS teacher_id,
    t.staff_code,
    u.first_name,
    u.last_name,
    u.email AS teacher_email,
    cm.id AS course_id,
    cm.course_id AS course_code,
    cm.course_name,
    cm.course_type,
    cm.lecture_hours,
    cm.practical_hours,
    cm.tutorial_hours,
    cm.credits,
    dept_student.dept_name AS course_dept
FROM 
    teacherCourse_teachercourse tc
LEFT JOIN 
    teacher_teacher t ON tc.teacher_id_id = t.id
LEFT JOIN 
    authentication_user u ON t.teacher_id_id = u.email
LEFT JOIN 
    course_course c ON tc.course_id_id = c.id
LEFT JOIN 
    courseMaster_coursemaster cm ON c.course_id_id = cm.id
LEFT JOIN 
    department_department dept_student ON c.for_dept_id_id = dept_student.id
WHERE 
    cm.degree_type NOT IN ('MTECH', 'ME', 'MBA')
"""

cursor.execute(query)
rows = cursor.fetchall()

# CSV output file path
csv_file_path = 'filtered_course_data.csv'

# Define headers
headers = [
    'student_count', 'academic_year', 'semester', 'requires_special_scheduling', 'is_assistant',
    'teacher_id', 'staff_code', 'first_name', 'last_name', 'teacher_email',
    'course_id', 'course_code', 'course_name', 'course_type',
    'lecture_hours', 'practical_hours', 'tutorial_hours', 'credits', 'course_dept'
]

# Write data to CSV
with open(csv_file_path, 'w', newline='', encoding='utf-8') as csv_file:
    writer = csv.writer(csv_file)
    writer.writerow(headers)  # write header row
    
    for row in rows:
        writer.writerow([
            row['student_count'],
            row['academic_year'],
            row['semester'],
            row['requires_special_scheduling'],
            row['is_assistant'],
            row['teacher_id'],
            row['staff_code'],
            row['first_name'],
            row['last_name'],
            row['teacher_email'],
            row['course_id'],
            row['course_code'],
            row['course_name'],
            row['course_type'],
            row['lecture_hours'],
            row['practical_hours'],
            row['tutorial_hours'],
            row['credits'],
            row['course_dept']  # mapped from student_dept
        ])

# Close DB connection
conn.close()

print(f"Data extraction complete. CSV file created at: {os.path.abspath(csv_file_path)}")

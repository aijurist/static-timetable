import pandas as pd
from ortools.sat.python import cp_model
from collections import defaultdict
import matplotlib.pyplot as plt
from matplotlib.table import Table
import os
from datetime import time

# Configuration
DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday']
TIME_SLOTS = [
    ("8:10", "9:00"),     # Period 1
    ("9:00", "9:50"),     # Period 2
    ("9:50", "10:10"),    # Morning break (20 min)
    ("10:10", "11:00"),   # Period 3
    ("11:00", "11:50"),   # Period 4
    ("11:50", "12:40"),   # Lunch break (50 min)
    ("12:40", "13:30"),   # Period 5
    ("13:30", "14:20"),   # Period 6
    ("14:20", "15:10")    # Period 7
]
BREAK_SLOTS = {2, 5}  # Indices of break time slots
MAX_TEACHER_HOURS = 21
CLASS_STRENGTH = 70
LAB_BATCH_SIZE = 35
LAB_DURATION_SLOTS = 2  # Labs require 2 consecutive slots

class DataModel:
    def __init__(self, teachers_courses_csv, rooms_csv):
        self.df = pd.read_csv(teachers_courses_csv)
        self.rooms_df = pd.read_csv(rooms_csv)
        self.teachers = {}
        self.courses = {}
        self.student_groups = []
        self.rooms = []
        self.time_slots = []
        self.lecture_assignments = []
        
        self._load_data()
        self._create_time_slots()
        self._create_lecture_assignments()
    
    def _load_data(self):
        # Load teachers
        for _, row in self.df.iterrows():
            teacher_id = str(row['teacher_id']) if pd.notna(row['teacher_id']) else "Unknown"
            if teacher_id not in self.teachers:
                self.teachers[teacher_id] = {
                    'id': teacher_id,
                    'staff_code': str(row['staff_code']) if pd.notna(row['staff_code']) else "",
                    'name': f"{str(row['first_name']) if pd.notna(row['first_name']) else ''} {str(row['last_name']) if pd.notna(row['last_name']) else ''}".strip(),
                    'email': str(row['teacher_email']) if pd.notna(row['teacher_email']) else "",
                    'max_hours': MAX_TEACHER_HOURS
                }
        
        # Load courses
        for _, row in self.df.iterrows():
            course_id = str(row['course_id'])
            if course_id not in self.courses:
                self.courses[course_id] = {
                    'id': course_id,
                    'code': str(row['course_code']),
                    'name': str(row['course_name']),
                    'type': str(row['course_type']) if pd.notna(row['course_type']) else "",
                    'lecture_hours': int(row['lecture_hours']) if pd.notna(row['lecture_hours']) else 0,
                    'practical_hours': int(row['practical_hours']) if pd.notna(row['practical_hours']) else 0,
                    'tutorial_hours': int(row['tutorial_hours']) if pd.notna(row['tutorial_hours']) else 0,
                    'credits': int(row['credits']) if pd.notna(row['credits']) else 0,
                    'dept': str(row['course_dept']) if pd.notna(row['course_dept']) else ""
                }
        
        # Create 6 student groups (Sections A-F)
        for i in range(6):
            self.student_groups.append({
                'id': i + 1,
                'name': f"Section {chr(65 + i)}",
                'strength': CLASS_STRENGTH
            })
        
        # Load rooms
        for _, row in self.rooms_df.iterrows():
            self.rooms.append({
                'id': int(row['id']),
                'number': str(row['room_number']),
                'block': str(row['block']),
                'is_lab': bool(row['is_lab']),
                'min_cap': int(row['room_min_cap']),
                'max_cap': int(row['room_max_cap'])
            })
    
    def _create_time_slots(self):
        slot_id = 0
        for day in range(len(DAYS)):
            for slot_idx, slot in enumerate(TIME_SLOTS):
                start_time = time(*map(int, slot[0].split(':')))
                end_time = time(*map(int, slot[1].split(':')))
                is_break = slot_idx in BREAK_SLOTS
                
                self.time_slots.append({
                    'id': slot_id,
                    'day': day,
                    'day_name': DAYS[day],
                    'start_time': start_time,
                    'end_time': end_time,
                    'is_break': is_break,
                    'slot_index': slot_idx,
                    'time_str': f"{start_time.strftime('%H:%M')}-{end_time.strftime('%H:%M')}"
                })
                slot_id += 1
    
    def _create_lecture_assignments(self):
        assignment_id = 0
        teacher_dict = self.teachers
        course_teacher_map = defaultdict(list)
        
        # Create mapping of course to available teachers
        for _, row in self.df.iterrows():
            course_id = str(row['course_id'])
            teacher_id = str(row['teacher_id']) if pd.notna(row['teacher_id']) else "Unknown"
            if teacher_id not in course_teacher_map[course_id]:
                course_teacher_map[course_id].append(teacher_id)
        
        for student_group in self.student_groups:
            for course_id, course in self.courses.items():
                teacher_list = course_teacher_map.get(course_id, ["Unknown"])
                
                # Assign teachers in round-robin fashion
                teacher_idx = (student_group['id'] - 1) % len(teacher_list)
                teacher_id = teacher_list[teacher_idx]
                teacher = teacher_dict.get(teacher_id, teacher_dict.get("Unknown"))
                
                # Lectures
                for _ in range(course['lecture_hours']):
                    self.lecture_assignments.append({
                        'id': assignment_id,
                        'course': course,
                        'teacher': teacher,
                        'student_group': student_group,
                        'type': 'lecture',
                        'duration': 1
                    })
                    assignment_id += 1
                
                # Tutorials
                for _ in range(course['tutorial_hours']):
                    self.lecture_assignments.append({
                        'id': assignment_id,
                        'course': course,
                        'teacher': teacher,
                        'student_group': student_group,
                        'type': 'tutorial',
                        'duration': 1
                    })
                    assignment_id += 1
                
                # Labs - create pairs of assignments for each lab session
                lab_sessions_needed = course['practical_hours']
                
                for lab_session in range(lab_sessions_needed):
                    # Create parent ID to link the two lab parts
                    parent_id = f"Lab_{course_id}_{student_group['id']}_{lab_session}"
                    
                    # First part of lab (first 50 minutes)
                    self.lecture_assignments.append({
                        'id': assignment_id,
                        'course': course,
                        'teacher': teacher,
                        'student_group': student_group,
                        'type': 'lab',
                        'duration': 1,
                        'lab_batch': 1,
                        'parent_lab_id': parent_id
                    })
                    assignment_id += 1
                    
                    # Second part of lab (second 50 minutes)
                    self.lecture_assignments.append({
                        'id': assignment_id,
                        'course': course,
                        'teacher': teacher,
                        'student_group': student_group,
                        'type': 'lab',
                        'duration': 1,
                        'lab_batch': 2,
                        'parent_lab_id': parent_id
                    })
                    assignment_id += 1

class TimetableSolver:
    def __init__(self, data_model):
        self.model = cp_model.CpModel()
        self.data = data_model
        self.solver = cp_model.CpSolver()
        self.solver.parameters.max_time_in_seconds = 300  # 5 minutes
        self.solver.parameters.num_search_workers = 8
        
        # Create variables
        self.assignment_vars = {}
        self._create_variables()
        
        # Add constraints
        self._add_constraints()
    
    def _create_variables(self):
        # Create assignment variables: (assignment_id, timeslot_id, room_id) -> BoolVar
        for assignment in self.data.lecture_assignments:
            for timeslot in self.data.time_slots:
                if timeslot['is_break']:
                    continue  # Skip break times
                
                for room in self.data.rooms:
                    # Check room suitability
                    if (assignment['type'] == 'lab' and not room['is_lab']) or \
                       (assignment['type'] != 'lab' and room['is_lab']):
                        continue
                    
                    # Check room capacity
                    required_capacity = LAB_BATCH_SIZE if assignment['type'] == 'lab' else CLASS_STRENGTH
                    if required_capacity > room['max_cap']:
                        continue
                    
                    var_name = f"a{assignment['id']}_t{timeslot['id']}_r{room['id']}"
                    self.assignment_vars[(assignment['id'], timeslot['id'], room['id'])] = \
                        self.model.NewBoolVar(var_name)
    
    def _add_constraints(self):
        # Each assignment must be scheduled exactly once
        for assignment in self.data.lecture_assignments:
            possible_slots = [
                var for (a_id, t_id, r_id), var in self.assignment_vars.items()
                if a_id == assignment['id']
            ]
            self.model.AddExactlyOne(possible_slots)
        
        # No teacher can teach two classes at the same time
        teacher_assignments = defaultdict(list)
        for (a_id, t_id, r_id), var in self.assignment_vars.items():
            assignment = next(a for a in self.data.lecture_assignments if a['id'] == a_id)
            teacher_assignments[(assignment['teacher']['id'], t_id)].append(var)
        
        for (teacher_id, timeslot_id), vars_list in teacher_assignments.items():
            self.model.Add(sum(vars_list) <= 1)
        
        # No room can be used for two classes at the same time
        room_assignments = defaultdict(list)
        for (a_id, t_id, r_id), var in self.assignment_vars.items():
            room_assignments[(r_id, t_id)].append(var)
        
        for (room_id, timeslot_id), vars_list in room_assignments.items():
            self.model.Add(sum(vars_list) <= 1)
        
        # No student group can have two classes at the same time
        group_assignments = defaultdict(list)
        for (a_id, t_id, r_id), var in self.assignment_vars.items():
            assignment = next(a for a in self.data.lecture_assignments if a['id'] == a_id)
            group_assignments[(assignment['student_group']['id'], t_id)].append(var)
        
        for (group_id, timeslot_id), vars_list in group_assignments.items():
            self.model.Add(sum(vars_list) <= 1)
        
        # Teacher max hours constraint
        teacher_hours = defaultdict(list)
        for (a_id, t_id, r_id), var in self.assignment_vars.items():
            assignment = next(a for a in self.data.lecture_assignments if a['id'] == a_id)
            teacher_hours[assignment['teacher']['id']].append(var)
        
        for teacher_id, vars_list in teacher_hours.items():
            self.model.Add(sum(vars_list) <= MAX_TEACHER_HOURS)
        
        # Lab constraints
        lab_assignments = [a for a in self.data.lecture_assignments if a['type'] == 'lab']
        lab_pairs = defaultdict(list)
        
        for assignment in lab_assignments:
            if 'parent_lab_id' in assignment:
                lab_pairs[assignment['parent_lab_id']].append(assignment['id'])
        
        for parent_id, [a1_id, a2_id] in lab_pairs.items():
            # Get all possible assignments for these two lab parts
            a1_vars = [
                (t_id, r_id, var) for (a_id, t_id, r_id), var in self.assignment_vars.items()
                if a_id == a1_id
            ]
            a2_vars = [
                (t_id, r_id, var) for (a_id, t_id, r_id), var in self.assignment_vars.items()
                if a_id == a2_id
            ]
            
            # Ensure they are consecutive and same day
            for (t1_id, r1_id, var1) in a1_vars:
                t1 = next(t for t in self.data.time_slots if t['id'] == t1_id)
                for (t2_id, r2_id, var2) in a2_vars:
                    t2 = next(t for t in self.data.time_slots if t['id'] == t2_id)
                    
                    # Check if consecutive and same day
                    if t1['day'] == t2['day'] and abs(t1['slot_index'] - t2['slot_index']) == 1:
                        # Different rooms for different batches
                        assignment1 = next(a for a in self.data.lecture_assignments if a['id'] == a1_id)
                        assignment2 = next(a for a in self.data.lecture_assignments if a['id'] == a2_id)
                        
                        if assignment1['lab_batch'] == assignment2['lab_batch']:
                            # Same batch must be in same room
                            same_room = self.model.NewBoolVar(f"same_room_{a1_id}_{a2_id}_{r1_id}_{r2_id}")
                            self.model.Add(r1_id == r2_id).OnlyEnforceIf(same_room)
                            self.model.Add(r1_id != r2_id).OnlyEnforceIf(same_room.Not())
                            self.model.AddBoolAnd([var1, var2]).OnlyEnforceIf(same_room)
                        else:
                            # Different batches should be in different rooms
                            self.model.Add(r1_id != r2_id).OnlyEnforceIf(var1).OnlyEnforceIf(var2)
        
        # Objective: Minimize gaps and prefer consecutive classes
        # This is simplified - a real implementation would need more sophisticated objectives
        self.model.Maximize(sum(self.assignment_vars.values()))
    
    def solve(self):
        status = self.solver.Solve(self.model)
        if status in [cp_model.OPTIMAL, cp_model.FEASIBLE]:
            return self._extract_solution()
        else:
            print("No solution found")
            return None
    
    def _extract_solution(self):
        solution = {
            'assignments': [],
            'teacher_hours': defaultdict(int),
            'room_usage': defaultdict(int),
            'group_schedules': defaultdict(list)
        }
        
        for (a_id, t_id, r_id), var in self.assignment_vars.items():
            if self.solver.Value(var):
                assignment = next(a for a in self.data.lecture_assignments if a['id'] == a_id)
                timeslot = next(t for t in self.data.time_slots if t['id'] == t_id)
                room = next(r for r in self.data.rooms if r['id'] == r_id)
                
                solution['assignments'].append({
                    'assignment': assignment,
                    'timeslot': timeslot,
                    'room': room
                })
                
                solution['teacher_hours'][assignment['teacher']['id']] += 1
                solution['room_usage'][r_id] += 1
                solution['group_schedules'][assignment['student_group']['id']].append({
                    'assignment': assignment,
                    'timeslot': timeslot,
                    'room': room
                })
        
        return solution

class TimetableVisualizer:
    @staticmethod
    def print_timetable(solution):
        if not solution:
            print("No solution to display")
            return
        
        print("\n=== FINAL TIMETABLE ===")
        
        # Group by student group
        for group_id, assignments in solution['group_schedules'].items():
            group = next(g for g in data_model.student_groups if g['id'] == group_id)
            print(f"\n=== TIMETABLE FOR {group['name']} ===")
            
            # Sort by day and time
            assignments.sort(key=lambda x: (x['timeslot']['day'], x['timeslot']['start_time']))
            
            current_day = None
            for assignment in assignments:
                if assignment['timeslot']['day'] != current_day:
                    current_day = assignment['timeslot']['day']
                    print(f"\n{DAYS[current_day]}")
                    print("-" * 60)
                
                batch_info = f" (Batch {assignment['assignment']['lab_batch']})" if 'lab_batch' in assignment['assignment'] else ""
                print(f"{assignment['timeslot']['time_str']}: "
                      f"{assignment['assignment']['course']['code']} - {assignment['assignment']['type'].title()}{batch_info}")
                print(f"  Teacher: {assignment['assignment']['teacher']['name']}")
                print(f"  Room: {assignment['room']['block']}-{assignment['room']['number']}")
    
    @staticmethod
    def export_to_csv(solution, filename="timetable_result.csv"):
        if not solution:
            print("No solution to export")
            return
        
        data = []
        for assignment in solution['assignments']:
            data.append({
                'Day': assignment['timeslot']['day_name'],
                'Time': assignment['timeslot']['time_str'],
                'Student Group': assignment['assignment']['student_group']['name'],
                'Course Code': assignment['assignment']['course']['code'],
                'Course Name': assignment['assignment']['course']['name'],
                'Session Type': assignment['assignment']['type'].title(),
                'Lab Batch': assignment['assignment'].get('lab_batch', ''),
                'Teacher': assignment['assignment']['teacher']['name'],
                'Room': f"{assignment['room']['block']}-{assignment['room']['number']}",
                'Duration': assignment['assignment']['duration']
            })
        
        pd.DataFrame(data).to_csv(filename, index=False)
        print(f"Timetable exported to {filename}")
    
    @staticmethod
    def generate_studentgroup_image_timetable(solution, output_dir="timetable_images"):
        os.makedirs(output_dir, exist_ok=True)
        
        # Get all time slots (excluding breaks)
        time_slots = [t for t in data_model.time_slots if not t['is_break']]
        time_strs = sorted({t['time_str'] for t in time_slots})
        
        for group_id, assignments in solution['group_schedules'].items():
            group = next(g for g in data_model.student_groups if g['id'] == group_id)
            
            fig, ax = plt.subplots(figsize=(14, 8))
            ax.axis('off')
            ax.set_title(f'Timetable for {group["name"]}', fontsize=16)
            
            # Prepare data for table
            data = [['' for _ in range(len(DAYS))] for _ in range(len(time_strs))]
            
            for assignment in assignments:
                time_str = assignment['timeslot']['time_str']
                day_idx = assignment['timeslot']['day']
                time_idx = time_strs.index(time_str)
                
                batch_info = f"\n(Batch {assignment['assignment']['lab_batch']})" if 'lab_batch' in assignment['assignment'] else ""
                data[time_idx][day_idx] = (
                    f"{assignment['assignment']['course']['code']} - {assignment['assignment']['type'][:3].upper()}{batch_info}\n"
                    f"{assignment['assignment']['teacher']['name'][:15]}\n"
                    f"{assignment['room']['block']}{assignment['room']['number']}"
                )
            
            # Create table
            table = Table(ax, bbox=[0, 0, 1, 1])
            width, height = 1.0 / len(DAYS), 1.0 / len(time_strs)
            
            # Add headers
            for i, day in enumerate(DAYS):
                table.add_cell(i+1, -1, width, height, text=day, loc='center', facecolor='lightblue')
            for j, t in enumerate(time_strs):
                table.add_cell(-1, j+1, width, height, text=t, loc='center', facecolor='lightblue')
            
            # Add data cells
            for i in range(len(time_strs)):
                for j in range(len(DAYS)):
                    table.add_cell(j+1, i+1, width, height, text=data[i][j], loc='center', 
                                  facecolor='white' if data[i][j] else 'whitesmoke')
            
            ax.add_table(table)
            plt.savefig(f"{output_dir}/{group['name'].replace(' ', '_')}.png", dpi=100)
            plt.close()
        
        print(f"Timetable images saved to {output_dir}")

# Main execution
if __name__ == "__main__":
    # Load data
    data_model = DataModel("cse-1.csv", "techlongue.csv")
    
    # Solve
    solver = TimetableSolver(data_model)
    solution = solver.solve()
    
    # Visualize results
    if solution:
        TimetableVisualizer.print_timetable(solution)
        TimetableVisualizer.export_to_csv(solution)
        TimetableVisualizer.generate_studentgroup_image_timetable(solution)
        
        # Print teacher workload
        print("\n=== TEACHER WORKLOAD ===")
        for teacher_id, hours in solution['teacher_hours'].items():
            teacher = data_model.teachers[teacher_id]
            status = "OVERLOADED" if hours > teacher['max_hours'] else "OK"
            print(f"{teacher['name']}: {hours}/{teacher['max_hours']} hours [{status}]")
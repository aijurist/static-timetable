import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.table import Table
import os
from collections import defaultdict
from datetime import time # Ensure time is imported from datetime
from .config import DAYS # Assuming config is in the same directory or package

# UPDATED HELPER FUNCTION WITH FIX FOR UNSCHEDULED LAB PARTS
def _group_consecutive_lab_assignments(sorted_assignments_for_entity):
    """
    Groups consecutive lab LectureAssignment objects.
    Assumes sorted_assignments_for_entity is already sorted by day and start_time,
    and filtered for relevant assignments (e.g., not breaks, assigned timeslot/room).
    """
    if not sorted_assignments_for_entity:
        return []

    processed_list = []
    i = 0
    n = len(sorted_assignments_for_entity)
    while i < n:
        current_assignment = sorted_assignments_for_entity[i]

        # FIX: Handle unscheduled lab parts before processing
        if not current_assignment.timeslot or not current_assignment.room:
            processed_list.append({
                'is_combined': False,
                'assignment_obj': current_assignment,
                'start_time': None,
                'end_time': None,
                'duration_hours': 0
            })
            i += 1
            continue  # Skip processing for unscheduled labs

        # Only attempt to group if it's a lab with a parent_lab_id and a timeslot
        if (current_assignment.session_type == "lab" and
            current_assignment.parent_lab_id is not None and
            current_assignment.timeslot is not None and # Must have a timeslot
            current_assignment.room is not None):       # And a room

            lab_block_parts = [current_assignment]
            
            for j in range(i + 1, n):
                next_assignment = sorted_assignments_for_entity[j]
                
                # Check conditions for being part of the same lab block
                if (next_assignment.timeslot and # Must have a timeslot
                    next_assignment.room and       # And a room
                    not next_assignment.timeslot.is_break and
                    next_assignment.session_type == "lab" and
                    next_assignment.parent_lab_id == current_assignment.parent_lab_id and
                    next_assignment.student_group == current_assignment.student_group and
                    next_assignment.course == current_assignment.course and
                    next_assignment.teacher == current_assignment.teacher and
                    next_assignment.room == current_assignment.room and 
                    next_assignment.lab_batch == current_assignment.lab_batch and # Handles None cases
                    next_assignment.timeslot.day == current_assignment.timeslot.day and
                    # Check for slot consecutiveness (assuming slot_index exists and is sequential)
                    hasattr(next_assignment.timeslot, 'slot_index') and
                    hasattr(lab_block_parts[-1].timeslot, 'slot_index') and
                    next_assignment.timeslot.slot_index == lab_block_parts[-1].timeslot.slot_index + 1):
                    lab_block_parts.append(next_assignment)
                else:
                    break # Chain broken or conditions not met
            
            if len(lab_block_parts) > 1: # Found a sequence to combine
                first_part = lab_block_parts[0]
                last_part = lab_block_parts[-1]
                
                processed_list.append({
                    'is_combined': True,
                    'assignment_obj': first_part, # Use the first part as the representative object
                    'start_time': first_part.timeslot.start_time,
                    'end_time': last_part.timeslot.end_time,
                    'duration_hours': sum(part.duration_hours() for part in lab_block_parts)
                })
                i += len(lab_block_parts) # Move index past all parts of this combined block
                continue
        
        # Regular assignment or lab not part of a sequence to be combined
        # Ensure it has a timeslot before trying to access timeslot attributes
        start_t = current_assignment.timeslot.start_time if current_assignment.timeslot else None
        end_t = current_assignment.timeslot.end_time if current_assignment.timeslot else None
        duration_h = current_assignment.duration_hours() if current_assignment.timeslot else 0

        processed_list.append({
            'is_combined': False, 
            'assignment_obj': current_assignment,
            'start_time': start_t,
            'end_time': end_t,
            'duration_hours': duration_h
        })
        i += 1
        
    return processed_list

def print_timetable(solution):
    if not solution:
        print("No solution to display")
        return
        
    print(f"\n=== FINAL TIMETABLE (Score: {solution.get_score()}) ===")
    
    # NEW: Add explicit warnings for unscheduled labs
    unscheduled = [a for a in solution.lecture_assignments if not a.timeslot]
    if unscheduled:
        print(f"\n⚠️  WARNING: {len(unscheduled)} UNSCHEDULED SESSIONS!")
        lab_unscheduled = [a for a in unscheduled if a.session_type == "lab"]
        if lab_unscheduled:
            print(f"   - {len(lab_unscheduled)} unscheduled LAB sessions (critical!)")
        print()
    
    by_group = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room and not assignment.timeslot.is_break:
            by_group[assignment.student_group.name].append(assignment)
    
    for group_name, assignments_list in by_group.items():
        print(f"\n=== TIMETABLE FOR {group_name} ===")
        assignments_list.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        
        effective_assignments = _group_consecutive_lab_assignments(assignments_list)

        current_day = None
        for item in effective_assignments:
            assign_obj = item['assignment_obj']
            
            # Skip items with no valid timeslot
            if not assign_obj.timeslot:
                continue
                
            if assign_obj.timeslot.day != current_day: # assign_obj.timeslot should be valid here
                current_day = assign_obj.timeslot.day
                print(f"\n{DAYS[current_day]}")
                print("-" * 60)
            
            if item['start_time'] and item['end_time']:
                start_time_str = item['start_time'].strftime('%H:%M')
                end_time_str = item['end_time'].strftime('%H:%M')
                
                batch_info = f" (Batch {assign_obj.lab_batch})" if assign_obj.lab_batch else ""
                # For unsplit 6 P.H. labs, lab_batch is None, so batch_info is empty, which is correct.
                print(f"{start_time_str}-{end_time_str}: "
                      f"{assign_obj.course.code} - {assign_obj.session_type.title()}{batch_info}")
                print(f"  Teacher: {assign_obj.teacher}")
                print(f"  Room: {assign_obj.room}")

def export_to_csv(solution, filename="timetable_result.csv"):
    if not solution:
        print("No solution to export")
        return
        
    data = []
    all_displayable_assignments = []
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room and not assignment.timeslot.is_break:
            all_displayable_assignments.append(assignment)
    
    # Fix: Ensure parent_lab_id and lab_batch are always strings for sorting
    def safe_parent_lab_id(x):
        return str(x.parent_lab_id) if x.parent_lab_id is not None else ""
    def safe_lab_batch(x):
        return str(x.lab_batch) if x.lab_batch is not None else ""
    
    all_displayable_assignments.sort(key=lambda x: (
        x.student_group.name, 
        x.course.code, 
        safe_parent_lab_id(x),  # Always string
        safe_lab_batch(x),      # Always string
        x.timeslot.day, 
        x.timeslot.start_time
    ))
    
    effective_assignments_for_csv = _group_consecutive_lab_assignments(all_displayable_assignments)

    for item in effective_assignments_for_csv:
        assign_obj = item['assignment_obj']
        
        # Handle unscheduled assignments
        if not item['start_time'] or not item['end_time']:
            continue  # Skip unscheduled for CSV export
            
        data.append({
            "Day": DAYS[assign_obj.timeslot.day],
            "Time": f"{item['start_time'].strftime('%H:%M')}-{item['end_time'].strftime('%H:%M')}",
            "Student Group": assign_obj.student_group.name,
            "Course Code": assign_obj.course.code,
            "Course Name": assign_obj.course.name,
            "Session Type": assign_obj.session_type.title(),
            "Lab Batch": assign_obj.lab_batch if assign_obj.lab_batch else "",
            "Teacher": str(assign_obj.teacher),
            "Room": str(assign_obj.room),
            "Duration (hours)": item['duration_hours']
        })
    
    if data:
        pd.DataFrame(data).to_csv(filename, index=False)
        print(f"Timetable exported to {filename}")
    else:
        print("No data to export to CSV after processing.")

def _generate_overall_timeslot_labels(solution):
    """ Helper to get a consistent list of time slot labels for Excel/Image outputs. """
    overall_effective_time_slots = set() # Store (start_time, end_time) tuples
    grouped_assignments_cache = {} # Cache grouped assignments: (type, entity_key) -> grouped_list

    # Process student group assignments
    by_group_temp = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room and not assignment.timeslot.is_break:
            by_group_temp[assignment.student_group.name].append(assignment)
    
    for group_name, assignments_list in by_group_temp.items():
        assignments_list.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        effective_assignments = _group_consecutive_lab_assignments(assignments_list)
        grouped_assignments_cache[('group', group_name)] = effective_assignments
        for item in effective_assignments:
            if item['start_time'] and item['end_time']: # Ensure times are valid
                overall_effective_time_slots.add((item['start_time'], item['end_time']))

    # Process teacher assignments
    by_teacher_temp = defaultdict(list)
    for assignment in solution.lecture_assignments:
         if assignment.timeslot and assignment.room and not assignment.timeslot.is_break:
            by_teacher_temp[assignment.teacher.full_name].append(assignment) # Use full_name as key

    for teacher_name, assignments_list in by_teacher_temp.items():
        assignments_list.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        effective_assignments = _group_consecutive_lab_assignments(assignments_list)
        grouped_assignments_cache[('teacher', teacher_name)] = effective_assignments
        for item in effective_assignments:
            if item['start_time'] and item['end_time']: # Ensure times are valid
                overall_effective_time_slots.add((item['start_time'], item['end_time']))
            
    sorted_overall_time_slots_tuples = sorted(list(overall_effective_time_slots))
    time_slots_row_labels = [f"{st.strftime('%H:%M')}-{et.strftime('%H:%M')}" 
                             for st, et in sorted_overall_time_slots_tuples]
    return time_slots_row_labels, grouped_assignments_cache

def export_to_excel(solution, filename="timetable.xlsx"):
    if not solution:
        print("No solution to export")
        return

    time_slots_row_labels, grouped_assignments_cache = _generate_overall_timeslot_labels(solution)
    
    # Entities for iteration (student groups and teachers)
    student_group_names = {a.student_group.name for a in solution.lecture_assignments if a.timeslot and a.room}
    teacher_full_names = {a.teacher.full_name for a in solution.lecture_assignments if a.timeslot and a.room}

    with pd.ExcelWriter(filename, engine='openpyxl') as writer:
        # Student Group Timetables
        for group_name in sorted(list(student_group_names)):
            data = {t_label: [''] * len(DAYS) for t_label in time_slots_row_labels}
            
            effective_assignments = grouped_assignments_cache.get(('group', group_name), [])
            for item in effective_assignments:
                assign_obj = item['assignment_obj']
                if not item['start_time'] or not item['end_time']: continue # Skip if times are invalid
                
                time_str_label = f"{item['start_time'].strftime('%H:%M')}-{item['end_time'].strftime('%H:%M')}"
                batch_info = f" (B{assign_obj.lab_batch})" if assign_obj.lab_batch else ""
                
                if time_str_label in data: # Check if the label exists (it should)
                    data[time_str_label][assign_obj.timeslot.day] = (
                        f"{assign_obj.course.code} {assign_obj.session_type[:3].upper()}{batch_info}\n"
                        f"{assign_obj.teacher.full_name[:10]}\n{assign_obj.room.block}{assign_obj.room.room_number}"
                    )
            
            df = pd.DataFrame(data, index=DAYS).T
            if not time_slots_row_labels: # Handle case with no assignable slots
                df = pd.DataFrame(columns=DAYS)
            else:
                 df = df.reindex(time_slots_row_labels, fill_value='')
            df.to_excel(writer, sheet_name=f"{group_name[:25]} Timetable")
        
        # Teacher Workload
        teacher_hours = defaultdict(float)
        teacher_max_hours_map = {}
        for assignment in solution.lecture_assignments:
            if assignment.timeslot:
                teacher_hours[assignment.teacher.full_name] += assignment.duration_hours()
                if assignment.teacher.full_name not in teacher_max_hours_map:
                     teacher_max_hours_map[assignment.teacher.full_name] = assignment.teacher.max_hours
        
        workload_data = []
        for teacher_name, hours in teacher_hours.items():
            max_h = teacher_max_hours_map.get(teacher_name, 0) # Default to 0 if not found
            workload_data.append({
                "Teacher": teacher_name,
                "Total Hours": hours,
                "Max Hours": max_h,
                "Status": "OVERLOADED" if hours > max_h else "OK"
            })
        pd.DataFrame(workload_data).sort_values(by="Total Hours", ascending=False).to_excel(writer, sheet_name="Teacher Workload", index=False)
        
        # Teacher Timetables
        for teacher_name in sorted(list(teacher_full_names)):
            data = {t_label: [''] * len(DAYS) for t_label in time_slots_row_labels}
            
            effective_assignments = grouped_assignments_cache.get(('teacher', teacher_name), [])
            for item in effective_assignments:
                assign_obj = item['assignment_obj']
                if not item['start_time'] or not item['end_time']: continue

                time_str_label = f"{item['start_time'].strftime('%H:%M')}-{item['end_time'].strftime('%H:%M')}"
                batch_info = f" (B{assign_obj.lab_batch})" if assign_obj.lab_batch else ""
                if time_str_label in data:
                     data[time_str_label][assign_obj.timeslot.day] = (
                        f"{assign_obj.course.code} {assign_obj.session_type[:3].upper()}{batch_info}\n"
                        f"{assign_obj.student_group.name}\n{assign_obj.room.block}{assign_obj.room.room_number}"
                    )
            
            df = pd.DataFrame(data, index=DAYS).T
            if not time_slots_row_labels:
                df = pd.DataFrame(columns=DAYS)
            else:
                df = df.reindex(time_slots_row_labels, fill_value='')
            sheet_name = f"{teacher_name[:25]}"
            df.to_excel(writer, sheet_name=sheet_name)
    
    print(f"Excel workbook saved as {filename}")

def print_teacher_workload(solution):
    if not solution:
        print("No solution for teacher workload")
        return
        
    teacher_hours = defaultdict(float)
    teacher_max_hours_map = {} # To store max_hours per teacher
    
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.teacher: # Ensure teacher object exists
            teacher_name = str(assignment.teacher) # Use a consistent key, like string representation
            teacher_hours[teacher_name] += assignment.duration_hours()
            if teacher_name not in teacher_max_hours_map:
                 teacher_max_hours_map[teacher_name] = assignment.teacher.max_hours
    
    print(f"\n=== TEACHER WORKLOAD SUMMARY ===")
    # Sort by hours, then by teacher name for consistent ordering
    sorted_teachers = sorted(teacher_hours.items(), key=lambda x: (-x[1], x[0]))

    for teacher_name, hours in sorted_teachers:
        max_h = teacher_max_hours_map.get(teacher_name, 0) # Default if somehow missed
        status = "OVERLOADED" if hours > max_h else "OK"
        print(f"{teacher_name}: {hours:.2f}/{max_h} hours [{status}]")

def generate_studentgroup_image_timetable(solution, output_dir="timetable_images"):
    os.makedirs(output_dir, exist_ok=True)
    
    time_slots_row_labels, grouped_assignments_cache = _generate_overall_timeslot_labels(solution)
    if not time_slots_row_labels: # No plottable timeslots
        print("No timeslots to generate images for.")
        return

    student_group_names = {a.student_group.name for a in solution.lecture_assignments if a.timeslot and a.room}

    for group_name in sorted(list(student_group_names)):
        fig_height = max(8, len(time_slots_row_labels) * 0.7 + 2) # Dynamic height + space for title
        fig, ax = plt.subplots(figsize=(16, fig_height))
        ax.axis('off') # Turn off axis numbers and ticks
        
        # Create data matrix based on time_slots_row_labels
        table_data = [['' for _ in range(len(DAYS))] for _ in range(len(time_slots_row_labels))]
        
        effective_assignments = grouped_assignments_cache.get(('group', group_name), [])
        for item in effective_assignments:
            assign_obj = item['assignment_obj']
            if not item['start_time'] or not item['end_time']: continue

            time_str_label = f"{item['start_time'].strftime('%H:%M')}-{item['end_time'].strftime('%H:%M')}"
            
            if time_str_label in time_slots_row_labels:
                time_idx = time_slots_row_labels.index(time_str_label)
                day_idx = assign_obj.timeslot.day # This should be valid due to earlier checks
                batch_info = f"\n(B{assign_obj.lab_batch})" if assign_obj.lab_batch else ""
                table_data[time_idx][day_idx] = (
                    f"{assign_obj.course.code} {assign_obj.session_type[:3].upper()}{batch_info}\n"
                    f"{assign_obj.teacher.full_name[:15]}\n{assign_obj.room.block}{assign_obj.room.room_number}"
                )
        
        # Create table - Bbox might need adjustment if title overlaps
        the_table = Table(ax, bbox=[0, 0, 1, 0.95]) # Adjust bbox to leave space for title
        ax.set_title(f'Timetable for {group_name}', fontsize=16, y=0.97) # Position title

        # Define column width and row height based on number of columns/rows + 1 for headers
        col_width = 1.0 / (len(DAYS) + 1)
        row_height = 1.0 / (len(time_slots_row_labels) + 1)

        # Add day headers (top row of the table structure)
        for j, day in enumerate(DAYS):
            the_table.add_cell(0, j + 1, col_width, row_height, text=day, loc='center', facecolor='lightblue')
        
        # Add time slot headers (first column of the table structure)
        for i, t_label in enumerate(time_slots_row_labels):
            the_table.add_cell(i + 1, 0, col_width, row_height, text=t_label, loc='center', facecolor='lightblue')
        
        # Add empty cell for top-left corner
        the_table.add_cell(0, 0, col_width, row_height, text='', loc='center', facecolor='lightblue')

        # Add data cells
        for i in range(len(time_slots_row_labels)):
            for j in range(len(DAYS)):
                cell_text = table_data[i][j]
                # Remove fontsize argument from add_cell (not supported)
                the_table.add_cell(i + 1, j + 1, col_width, row_height, text=cell_text, loc='center', 
                                   facecolor='white' if cell_text else 'whitesmoke')
        
        ax.add_table(the_table)
        
        # Sanitize filename
        safe_group_name = group_name.replace(' ', '_').replace('/', '_').replace('\\', '_')
        plt.savefig(os.path.join(output_dir, f"{safe_group_name}.png"), dpi=120, bbox_inches='tight')
        plt.close(fig) # Close the figure to free memory
    print(f"Timetable images saved to {output_dir}")
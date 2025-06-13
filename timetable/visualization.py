import pandas as pd
import matplotlib
matplotlib.use('Agg')  # Use non-interactive backend
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch
import numpy as np
import os
from collections import defaultdict
from datetime import time, datetime, timedelta
from .config import DAYS, THEORY_TIME_SLOTS, LAB_TIME_SLOTS

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

        # Handle unscheduled lab parts
        if not current_assignment.timeslot or not current_assignment.room:
            processed_list.append({
                'is_combined': False,
                'assignment_obj': current_assignment,
                'start_time': None,
                'end_time': None,
                'duration_hours': 0
            })
            i += 1
            continue

        # Only attempt to group if it's a lab with a parent_lab_id and a timeslot
        if (current_assignment.session_type == "lab" and
            current_assignment.parent_lab_id is not None and
            current_assignment.timeslot is not None and
            current_assignment.room is not None):

            lab_block_parts = [current_assignment]
            
            for j in range(i + 1, n):
                next_assignment = sorted_assignments_for_entity[j]
                
                # Check conditions for being part of the same lab block
                if (next_assignment.timeslot and
                    next_assignment.room and
                    next_assignment.session_type == "lab" and
                    next_assignment.parent_lab_id == current_assignment.parent_lab_id and
                    next_assignment.student_group == current_assignment.student_group and
                    next_assignment.course == current_assignment.course and
                    next_assignment.teacher == current_assignment.teacher and
                    next_assignment.room == current_assignment.room and 
                    next_assignment.lab_batch == current_assignment.lab_batch and
                    next_assignment.timeslot.day == current_assignment.timeslot.day and
                    next_assignment.timeslot.is_lab and  # Must be a lab timeslot
                    current_assignment.timeslot.is_lab):  # Current must also be a lab timeslot
                    lab_block_parts.append(next_assignment)
                else:
                    break
            
            if len(lab_block_parts) > 1:
                first_part = lab_block_parts[0]
                last_part = lab_block_parts[-1]
                
                processed_list.append({
                    'is_combined': True,
                    'assignment_obj': first_part,
                    'start_time': first_part.timeslot.start_time,
                    'end_time': last_part.timeslot.end_time,
                    'duration_hours': sum(part.duration_hours() for part in lab_block_parts)
                })
                i += len(lab_block_parts)
                continue
        
        # Regular assignment or lab not part of a sequence to be combined
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

def _get_time_slots_with_labs():
    """Get all time slots including both theory and lab slots, sorted by time."""
    all_slots = []
    
    # Add theory slots
    for start_str, end_str in THEORY_TIME_SLOTS:
        start_hour, start_min = map(int, start_str.split(':'))
        end_hour, end_min = map(int, end_str.split(':'))
        start_time = time(start_hour, start_min)
        end_time = time(end_hour, end_min)
        all_slots.append((start_time, end_time, 'theory'))
    
    # Add lab slots
    for start_str, end_str in LAB_TIME_SLOTS:
        start_hour, start_min = map(int, start_str.split(':'))
        end_hour, end_min = map(int, end_str.split(':'))
        start_time = time(start_hour, start_min)
        end_time = time(end_hour, end_min)
        all_slots.append((start_time, end_time, 'lab'))
    
    # Sort by start time
    all_slots.sort(key=lambda x: x[0])
    return all_slots

def _time_to_minutes(t):
    """Convert time object to minutes since midnight."""
    return t.hour * 60 + t.minute

def _get_color_for_course(course_code, session_type):
    """Get a consistent color for each course and session type."""
    colors = {
        'theory': ['#E8F4FD', '#D1E9FB', '#B8DDF9', '#9FD2F7', '#86C7F5'],
        'lab': ['#FFF2E8', '#FFE6D1', '#FFD9B8', '#FFCC9F', '#FFBF86'],
        'tutorial': ['#F0F8E8', '#E1F1D1', '#D2EAB8', '#C3E39F', '#B4DC86'],
        'lecture': ['#E8F4FD', '#D1E9FB', '#B8DDF9', '#9FD2F7', '#86C7F5']  # Using same colors as theory
    }
    
    # Generate a hash from course code to get consistent color
    hash_val = hash(course_code) % len(colors[session_type])
    return colors[session_type][hash_val]

def generate_enhanced_timetable_image(solution, output_dir="enhanced_timetables"):
    """Generate enhanced timetable images with better formatting and visual design."""
    if not solution:
        print("No solution to visualize")
        return
        
    os.makedirs(output_dir, exist_ok=True)
    
    # Group assignments by student group
    by_group = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            by_group[assignment.student_group.name].append(assignment)
    
    # Get all time slots
    time_slots = _get_time_slots_with_labs()
    
    # Set up matplotlib parameters for better appearance
    plt.rcParams['font.family'] = 'Arial'
    plt.rcParams['font.size'] = 10
    
    for group_name, assignments in by_group.items():
        # Sort assignments
        assignments.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        effective_assignments = _group_consecutive_lab_assignments(assignments)
        
        # Create figure with better size and DPI
        fig, ax = plt.subplots(figsize=(16, 10))
        ax.set_xlim(0, len(DAYS))
        ax.set_ylim(0, len(time_slots))
        
        # Set background color
        fig.patch.set_facecolor('white')
        ax.set_facecolor('#F8F9FA')
        
        # Create grid
        for i in range(len(DAYS) + 1):
            ax.axvline(x=i, color='#DEE2E6', linewidth=1)
        for i in range(len(time_slots) + 1):
            ax.axhline(y=i, color='#DEE2E6', linewidth=1)
        
        # Add day headers
        for day_idx, day_name in enumerate(DAYS):
            ax.text(day_idx + 0.5, len(time_slots) - 0.3, day_name, 
                   ha='center', va='center', fontweight='bold', fontsize=12,
                   bbox=dict(boxstyle="round,pad=0.3", facecolor='#495057', 
                            edgecolor='none', alpha=0.8),
                   color='white')
        
        # Add time slot labels
        for slot_idx, (start_time, end_time, slot_type) in enumerate(time_slots):
            time_label = f"{start_time.strftime('%H:%M')}\n{end_time.strftime('%H:%M')}"
            slot_color = '#E9ECEF' if slot_type == 'theory' else '#FFF3CD'
            
            ax.text(-0.4, len(time_slots) - slot_idx - 0.5, time_label,
                   ha='center', va='center', fontsize=9,
                   bbox=dict(boxstyle="round,pad=0.2", facecolor=slot_color,
                            edgecolor='#ADB5BD', alpha=0.7))
        
        # Place assignments
        for item in effective_assignments:
            assignment = item['assignment_obj']
            if not item['start_time'] or not item['end_time']:
                continue
            
            day_idx = assignment.timeslot.day
            
            # Find the time slot index
            slot_idx = None
            for i, (start_time, end_time, _) in enumerate(time_slots):
                if (item['start_time'] == start_time and 
                    item['end_time'] == end_time):
                    slot_idx = i
                    break
            
            if slot_idx is not None:
                # Format session type
                session_type = assignment.session_type.title()
                if assignment.session_type == "lab":
                    batch_info = f" (B{assignment.lab_batch})" if assignment.lab_batch else ""
                    session_type = f"Lab{batch_info}"
                
                # Get color for this course
                color = _get_color_for_course(assignment.course.code, assignment.session_type)
                
                # Create fancy box for the assignment
                y_pos = len(time_slots) - slot_idx - 1
                fancy_box = FancyBboxPatch(
                    (day_idx + 0.05, y_pos + 0.05),
                    0.9, 0.9,
                    boxstyle="round,pad=0.02",
                    facecolor=color,
                    edgecolor='#6C757D',
                    linewidth=1.5,
                    alpha=0.9
                )
                ax.add_patch(fancy_box)
                
                # Format text content
                course_text = f"{assignment.course.code}"
                session_text = session_type
                teacher_text = str(assignment.teacher).split()[-1]  # Last name only
                room_text = f"Room: {assignment.room}"
                
                # Add assignment text with better formatting
                ax.text(day_idx + 0.5, y_pos + 0.7, course_text,
                       ha='center', va='center', fontweight='bold', fontsize=11,
                       color='#212529')
                ax.text(day_idx + 0.5, y_pos + 0.5, session_text,
                       ha='center', va='center', fontsize=9,
                       color='#495057', style='italic')
                ax.text(day_idx + 0.5, y_pos + 0.3, teacher_text,
                       ha='center', va='center', fontsize=8,
                       color='#6C757D')
                ax.text(day_idx + 0.5, y_pos + 0.1, room_text,
                       ha='center', va='center', fontsize=8,
                       color='#6C757D')
        
        # Remove axes and ticks
        ax.set_xticks([])
        ax.set_yticks([])
        ax.spines['top'].set_visible(False)
        ax.spines['right'].set_visible(False)
        ax.spines['bottom'].set_visible(False)
        ax.spines['left'].set_visible(False)
        
        # Add title with better styling
        plt.suptitle(f"Timetable for {group_name}", 
                    fontsize=16, fontweight='bold', y=0.95,
                    bbox=dict(boxstyle="round,pad=0.5", facecolor='#F8F9FA',
                             edgecolor='#DEE2E6', linewidth=2))
        
        # Add legend
        legend_elements = []
        session_types = set()
        for assignment in assignments:
            session_types.add(assignment.session_type)
        
        legend_y = 0.02
        for i, session_type in enumerate(sorted(session_types)):
            color = _get_color_for_course("SAMPLE", session_type)
            legend_patch = patches.Rectangle((0, 0), 1, 1, facecolor=color, 
                                           edgecolor='#6C757D', linewidth=1)
            legend_elements.append((legend_patch, session_type.title()))
        
        if legend_elements:
            patches_list, labels_list = zip(*legend_elements)
            ax.legend(patches_list, labels_list, loc='upper left', 
                     bbox_to_anchor=(0, 1), framealpha=0.9)
        
        # Adjust layout and save
        plt.tight_layout()
        plt.subplots_adjust(top=0.92, bottom=0.05, left=0.08, right=0.98)
        
        # Save with high quality
        filename = os.path.join(output_dir, f"{group_name}_enhanced_timetable.png")
        plt.savefig(filename, dpi=300, bbox_inches='tight', 
                   facecolor='white', edgecolor='none')
        plt.close()
    
    print(f"Enhanced timetable images generated in {output_dir}")

def generate_teacher_timetable_images(solution, output_dir="teacher_timetables"):
    """Generate enhanced timetable images for teachers."""
    if not solution:
        print("No solution to visualize")
        return
        
    os.makedirs(output_dir, exist_ok=True)
    
    # Group assignments by teacher
    by_teacher = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            by_teacher[assignment.teacher.full_name].append(assignment)
    
    # Get all time slots
    time_slots = _get_time_slots_with_labs()
    
    # Set up matplotlib parameters
    plt.rcParams['font.family'] = 'Arial'
    plt.rcParams['font.size'] = 10
    
    for teacher_name, assignments in by_teacher.items():
        # Sort assignments
        assignments.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        effective_assignments = _group_consecutive_lab_assignments(assignments)
        
        # Create figure
        fig, ax = plt.subplots(figsize=(16, 10))
        ax.set_xlim(0, len(DAYS))
        ax.set_ylim(0, len(time_slots))
        
        # Set background
        fig.patch.set_facecolor('white')
        ax.set_facecolor('#F8F9FA')
        
        # Create grid
        for i in range(len(DAYS) + 1):
            ax.axvline(x=i, color='#DEE2E6', linewidth=1)
        for i in range(len(time_slots) + 1):
            ax.axhline(y=i, color='#DEE2E6', linewidth=1)
        
        # Add day headers
        for day_idx, day_name in enumerate(DAYS):
            ax.text(day_idx + 0.5, len(time_slots) - 0.3, day_name, 
                   ha='center', va='center', fontweight='bold', fontsize=12,
                   bbox=dict(boxstyle="round,pad=0.3", facecolor='#28A745', 
                            edgecolor='none', alpha=0.8),
                   color='white')
        
        # Add time slot labels
        for slot_idx, (start_time, end_time, slot_type) in enumerate(time_slots):
            time_label = f"{start_time.strftime('%H:%M')}\n{end_time.strftime('%H:%M')}"
            slot_color = '#E9ECEF' if slot_type == 'theory' else '#FFF3CD'
            
            ax.text(-0.4, len(time_slots) - slot_idx - 0.5, time_label,
                   ha='center', va='center', fontsize=9,
                   bbox=dict(boxstyle="round,pad=0.2", facecolor=slot_color,
                            edgecolor='#ADB5BD', alpha=0.7))
        
        # Place assignments
        for item in effective_assignments:
            assignment = item['assignment_obj']
            if not item['start_time'] or not item['end_time']:
                continue
            
            day_idx = assignment.timeslot.day
            
            # Find the time slot index
            slot_idx = None
            for i, (start_time, end_time, _) in enumerate(time_slots):
                if (item['start_time'] == start_time and 
                    item['end_time'] == end_time):
                    slot_idx = i
                    break
            
            if slot_idx is not None:
                # Format session type
                session_type = assignment.session_type.title()
                if assignment.session_type == "lab":
                    batch_info = f" (B{assignment.lab_batch})" if assignment.lab_batch else ""
                    session_type = f"Lab{batch_info}"
                
                # Get color
                color = _get_color_for_course(assignment.course.code, assignment.session_type)
                
                # Create fancy box
                y_pos = len(time_slots) - slot_idx - 1
                fancy_box = FancyBboxPatch(
                    (day_idx + 0.05, y_pos + 0.05),
                    0.9, 0.9,
                    boxstyle="round,pad=0.02",
                    facecolor=color,
                    edgecolor='#6C757D',
                    linewidth=1.5,
                    alpha=0.9
                )
                ax.add_patch(fancy_box)
                
                # Format text content
                course_text = f"{assignment.course.code}"
                session_text = session_type
                group_text = assignment.student_group.name
                room_text = f"Room: {assignment.room}"
                
                # Add text
                ax.text(day_idx + 0.5, y_pos + 0.7, course_text,
                       ha='center', va='center', fontweight='bold', fontsize=11,
                       color='#212529')
                ax.text(day_idx + 0.5, y_pos + 0.5, session_text,
                       ha='center', va='center', fontsize=9,
                       color='#495057', style='italic')
                ax.text(day_idx + 0.5, y_pos + 0.3, group_text,
                       ha='center', va='center', fontsize=8,
                       color='#6C757D')
                ax.text(day_idx + 0.5, y_pos + 0.1, room_text,
                       ha='center', va='center', fontsize=8,
                       color='#6C757D')
        
        # Remove axes
        ax.set_xticks([])
        ax.set_yticks([])
        for spine in ax.spines.values():
            spine.set_visible(False)
        
        # Add title
        plt.suptitle(f"Timetable for {teacher_name}", 
                    fontsize=16, fontweight='bold', y=0.95,
                    bbox=dict(boxstyle="round,pad=0.5", facecolor='#E8F5E8',
                             edgecolor='#28A745', linewidth=2))
        
        # Calculate total hours for subtitle
        total_hours = sum(item['duration_hours'] for item in effective_assignments)
        plt.figtext(0.5, 0.02, f"Total Teaching Hours: {total_hours}", 
                   ha='center', fontsize=12, style='italic')
        
        # Adjust layout and save
        plt.tight_layout()
        plt.subplots_adjust(top=0.92, bottom=0.08, left=0.08, right=0.98)
        
        # Clean filename
        safe_filename = "".join(c for c in teacher_name if c.isalnum() or c in (' ', '-', '_')).rstrip()
        filename = os.path.join(output_dir, f"{safe_filename}_timetable.png")
        plt.savefig(filename, dpi=300, bbox_inches='tight', 
                   facecolor='white', edgecolor='none')
        plt.close()
    
    print(f"Teacher timetable images generated in {output_dir}")

def generate_summary_dashboard(solution, output_dir="timetable_summary"):
    """Generate a summary dashboard showing key statistics."""
    if not solution:
        print("No solution to visualize")
        return
        
    os.makedirs(output_dir, exist_ok=True)
    
    # Collect statistics
    total_assignments = len([a for a in solution.lecture_assignments if a.timeslot and a.room])
    unscheduled = len([a for a in solution.lecture_assignments if not a.timeslot])
    
    # Group by type
    by_type = defaultdict(int)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            by_type[assignment.session_type] += 1
    
    # Teacher workload
    teacher_hours = defaultdict(float)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            teacher_hours[assignment.teacher.full_name] += assignment.duration_hours()
    
    # Room utilization
    room_usage = defaultdict(int)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            room_usage[str(assignment.room)] += 1
    
    # Create dashboard
    fig = plt.figure(figsize=(16, 10))
    fig.patch.set_facecolor('white')
    
    # Main title
    fig.suptitle('Timetable Summary Dashboard', fontsize=20, fontweight='bold', y=0.95)
    
    # Create subplots
    gs = fig.add_gridspec(3, 3, hspace=0.3, wspace=0.3)
    
    # Overall statistics
    ax1 = fig.add_subplot(gs[0, :])
    ax1.axis('off')
    score = solution.get_score()
    stats_text = f"""
    📊 OVERALL STATISTICS
    
    Total Scheduled Sessions: {total_assignments}
    Unscheduled Sessions: {unscheduled}
    Overall Score: Hard={score.hardScore}, Soft={score.softScore}
    
    Session Types: Theory ({by_type.get('theory', 0)}), Lab ({by_type.get('lab', 0)}), Tutorial ({by_type.get('tutorial', 0)})
    """
    ax1.text(0.1, 0.5, stats_text, fontsize=14, verticalalignment='center',
            bbox=dict(boxstyle="round,pad=0.5", facecolor='#E8F4FD', alpha=0.7))
    
    # Teacher workload chart
    ax2 = fig.add_subplot(gs[1, 0])
    if teacher_hours:
        teachers = list(teacher_hours.keys())[:10]  # Top 10 teachers
        hours = [teacher_hours[t] for t in teachers]
        teacher_names = [t.split()[-1] for t in teachers]  # Last names only
        
        bars = ax2.barh(teacher_names, hours, color='#28A745', alpha=0.7)
        ax2.set_title('Teacher Workload (Hours)', fontweight='bold')
        ax2.set_xlabel('Teaching Hours')
        
        # Add value labels on bars
        for bar, hour in zip(bars, hours):
            ax2.text(bar.get_width() + 0.1, bar.get_y() + bar.get_height()/2, 
                    f'{hour:.1f}', va='center', fontsize=9)
    
    # Room utilization
    ax3 = fig.add_subplot(gs[1, 1])
    if room_usage:
        rooms = list(room_usage.keys())[:10]  # Top 10 rooms
        usage = [room_usage[r] for r in rooms]
        
        bars = ax3.bar(rooms, usage, color='#FFC107', alpha=0.7)
        ax3.set_title('Room Utilization', fontweight='bold')
        ax3.set_ylabel('Number of Sessions')
        ax3.tick_params(axis='x', rotation=45)
        
        # Add value labels
        for bar, use in zip(bars, usage):
            ax3.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.1, 
                    str(use), ha='center', fontsize=9)
    
    # Session type distribution
    ax4 = fig.add_subplot(gs[1, 2])
    if by_type:
        colors = ['#007BFF', '#FFC107', '#28A745']
        wedges, texts, autotexts = ax4.pie(by_type.values(), labels=by_type.keys(), 
                                          autopct='%1.1f%%', colors=colors, 
                                          startangle=90)
        ax4.set_title('Session Type Distribution', fontweight='bold')
    
    # Daily distribution
    ax5 = fig.add_subplot(gs[2, :2])
    daily_count = [0] * 5
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            daily_count[assignment.timeslot.day] += 1
    
    bars = ax5.bar(DAYS, daily_count, color=['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7'])
    ax5.set_title('Daily Session Distribution', fontweight='bold')
    ax5.set_ylabel('Number of Sessions')
    
    # Add value labels
    for bar, count in zip(bars, daily_count):
        ax5.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.1, 
                str(count), ha='center', fontsize=10, fontweight='bold')
    
    # Issues summary
    ax6 = fig.add_subplot(gs[2, 2])
    ax6.axis('off')
    issues_text = f"""
    ⚠️ ISSUES SUMMARY
    
    Unscheduled: {unscheduled}
    
    """
    if unscheduled > 0:
        issues_text += "❌ Critical: Unscheduled sessions found!"
    else:
        issues_text += "✅ All sessions scheduled successfully!"
    
    box_color = '#FFE6E6' if unscheduled > 0 else '#E8F5E8'
    ax6.text(0.1, 0.5, issues_text, fontsize=12, verticalalignment='center',
            bbox=dict(boxstyle="round,pad=0.3", facecolor=box_color, alpha=0.8))
    
    # Save dashboard
    plt.savefig(os.path.join(output_dir, 'timetable_dashboard.png'), 
               dpi=300, bbox_inches='tight', facecolor='white')
    plt.close()
    
    print(f"Summary dashboard generated in {output_dir}")

# Keep existing functions but update the main image generation function
def generate_studentgroup_image_timetable(solution, output_dir="timetable_images"):
    """Updated wrapper that calls the enhanced version."""
    return generate_enhanced_timetable_image(solution, output_dir)

# Export functions for existing modules
def print_timetable(solution):
    """Print timetable to console - keeping original functionality."""
    if not solution:
        print("No solution to display")
        return
        
    print(f"\n=== FINAL TIMETABLE (Score: {solution.get_score()}) ===")
    
    unscheduled = [a for a in solution.lecture_assignments if not a.timeslot]
    if unscheduled:
        print(f"\n⚠️  WARNING: {len(unscheduled)} UNSCHEDULED SESSIONS!")
        lab_unscheduled = [a for a in unscheduled if a.session_type == "lab"]
        if lab_unscheduled:
            print(f"   - {len(lab_unscheduled)} unscheduled LAB sessions (critical!)")
        print()
    
    by_group = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            by_group[assignment.student_group.name].append(assignment)
    
    for group_name, assignments_list in by_group.items():
        print(f"\n=== TIMETABLE FOR {group_name} ===")
        assignments_list.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        
        effective_assignments = _group_consecutive_lab_assignments(assignments_list)

        current_day = None
        for item in effective_assignments:
            assign_obj = item['assignment_obj']
            
            if not assign_obj.timeslot:
                continue
                
            if assign_obj.timeslot.day != current_day:
                current_day = assign_obj.timeslot.day
                print(f"\n{DAYS[current_day]}")
                print("-" * 60)
            
            if item['start_time'] and item['end_time']:
                start_time_str = item['start_time'].strftime('%H:%M')
                end_time_str = item['end_time'].strftime('%H:%M')
                
                session_type = assign_obj.session_type.title()
                if assign_obj.session_type == "lab":
                    batch_info = f" (Batch {assign_obj.lab_batch})" if assign_obj.lab_batch else ""
                    session_type = f"Lab{batch_info}"
                
                print(f"{start_time_str}-{end_time_str}: "
                      f"{assign_obj.course.code} - {session_type}")
                print(f"  Teacher: {assign_obj.teacher}")
                print(f"  Room: {assign_obj.room}")

def export_to_csv(solution, filename="timetable_result.csv"):
    """Export timetable to CSV - keeping original functionality."""
    if not solution:
        print("No solution to export")
        return
        
    data = []
    all_displayable_assignments = []
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            all_displayable_assignments.append(assignment)
    
    def safe_parent_lab_id(x):
        return str(x.parent_lab_id) if x.parent_lab_id is not None else ""
    def safe_lab_batch(x):
        return str(x.lab_batch) if x.lab_batch is not None else ""
    
    all_displayable_assignments.sort(key=lambda x: (
        x.student_group.name, 
        x.course.code, 
        safe_parent_lab_id(x),
        safe_lab_batch(x),
        x.timeslot.day, 
        x.timeslot.start_time
    ))
    
    effective_assignments_for_csv = _group_consecutive_lab_assignments(all_displayable_assignments)

    for item in effective_assignments_for_csv:
        assign_obj = item['assignment_obj']
        
        if not item['start_time'] or not item['end_time']:
            continue
            
        session_type = assign_obj.session_type.title()
        if assign_obj.session_type == "lab":
            batch_info = f" (Batch {assign_obj.lab_batch})" if assign_obj.lab_batch else ""
            session_type = f"Lab{batch_info}"
            
        data.append({
            "Day": DAYS[assign_obj.timeslot.day],
            "Time": f"{item['start_time'].strftime('%H:%M')}-{item['end_time'].strftime('%H:%M')}",
            "Student Group": assign_obj.student_group.name,
            "Course Code": assign_obj.course.code,
            "Course Name": assign_obj.course.name,
            "Session Type": session_type,
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
    """Helper to get a consistent list of time slot labels for Excel/Image outputs."""
    overall_effective_time_slots = set()
    grouped_assignments_cache = {}

    # Process student group assignments
    by_group_temp = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            by_group_temp[assignment.student_group.name].append(assignment)
    
    for group_name, assignments_list in by_group_temp.items():
        assignments_list.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        effective_assignments = _group_consecutive_lab_assignments(assignments_list)
        grouped_assignments_cache[('group', group_name)] = effective_assignments
        for item in effective_assignments:
            if item['start_time'] and item['end_time']:
                overall_effective_time_slots.add((item['start_time'], item['end_time']))

    # Process teacher assignments
    by_teacher_temp = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            by_teacher_temp[assignment.teacher.full_name].append(assignment)

    for teacher_name, assignments_list in by_teacher_temp.items():
        assignments_list.sort(key=lambda x: (x.timeslot.day, x.timeslot.start_time))
        effective_assignments = _group_consecutive_lab_assignments(assignments_list)
        grouped_assignments_cache[('teacher', teacher_name)] = effective_assignments
        for item in effective_assignments:
            if item['start_time'] and item['end_time']:
                overall_effective_time_slots.add((item['start_time'], item['end_time']))
            
    sorted_overall_time_slots_tuples = sorted(list(overall_effective_time_slots))
    time_slots_row_labels = [f"{st.strftime('%H:%M')}-{et.strftime('%H:%M')}" 
                             for st, et in sorted_overall_time_slots_tuples]
    return time_slots_row_labels, grouped_assignments_cache

def export_to_excel(solution, filename="timetable.xlsx"):
    """Export timetable to Excel - keeping original functionality."""
    if not solution:
        print("No solution to export")
        return

    time_slots_row_labels, grouped_assignments_cache = _generate_overall_timeslot_labels(solution)
    
    # Create Excel writer
    with pd.ExcelWriter(filename, engine='openpyxl') as writer:
        # Export student group timetables
        student_group_names = {a.student_group.name for a in solution.lecture_assignments if a.timeslot and a.room}
        for group_name in sorted(student_group_names):
            data = []
            effective_assignments = grouped_assignments_cache[('group', group_name)]
            
            for item in effective_assignments:
                assign_obj = item['assignment_obj']
                if not item['start_time'] or not item['end_time']:
                    continue
                    
                session_type = assign_obj.session_type.title()
                if assign_obj.session_type == "lab":
                    batch_info = f" (Batch {assign_obj.lab_batch})" if assign_obj.lab_batch else ""
                    session_type = f"Lab{batch_info}"
                
                data.append({
                    "Day": DAYS[assign_obj.timeslot.day],
                    "Time": f"{item['start_time'].strftime('%H:%M')}-{item['end_time'].strftime('%H:%M')}",
                    "Course": f"{assign_obj.course.code} - {session_type}",
                    "Teacher": str(assign_obj.teacher),
                    "Room": str(assign_obj.room)
                })
            
            if data:
                df = pd.DataFrame(data)
                df.to_excel(writer, sheet_name=f"Group_{group_name}", index=False)
        
        # Export teacher timetables
        teacher_names = {a.teacher.full_name for a in solution.lecture_assignments if a.timeslot and a.room}
        for teacher_name in sorted(teacher_names):
            data = []
            effective_assignments = grouped_assignments_cache[('teacher', teacher_name)]
            
            for item in effective_assignments:
                assign_obj = item['assignment_obj']
                if not item['start_time'] or not item['end_time']:
                    continue
                    
                session_type = assign_obj.session_type.title()
                if assign_obj.session_type == "lab":
                    batch_info = f" (Batch {assign_obj.lab_batch})" if assign_obj.lab_batch else ""
                    session_type = f"Lab{batch_info}"
                
                data.append({
                    "Day": DAYS[assign_obj.timeslot.day],
                    "Time": f"{item['start_time'].strftime('%H:%M')}-{item['end_time'].strftime('%H:%M')}",
                    "Course": f"{assign_obj.course.code} - {session_type}",
                    "Group": assign_obj.student_group.name,
                    "Room": str(assign_obj.room)
                })
            
            if data:
                df = pd.DataFrame(data)
                df.to_excel(writer, sheet_name=f"Teacher_{teacher_name}", index=False)
        
        # After all sheets are written, ensure at least one sheet exists:
        if not writer.book.sheetnames:
            # Create a dummy sheet to avoid openpyxl error
            writer.book.create_sheet("Sheet1")
    
    print(f"Timetable exported to {filename}")

def print_teacher_workload(solution):
    """Print teacher workload analysis - keeping original functionality."""
    if not solution:
        print("No solution to analyze")
        return
        
    print("\n=== TEACHER WORKLOAD ANALYSIS ===")
    
    by_teacher = defaultdict(list)
    for assignment in solution.lecture_assignments:
        if assignment.timeslot and assignment.room:
            by_teacher[assignment.teacher.full_name].append(assignment)
    
    for teacher_name, assignments in sorted(by_teacher.items()):
        total_hours = sum(a.duration_hours() for a in assignments)
        print(f"\n{teacher_name}:")
        print(f"Total hours: {total_hours}")
        
        by_day = defaultdict(list)
        for assignment in assignments:
            by_day[assignment.timeslot.day].append(assignment)
        
        for day in range(5):
            day_assignments = by_day[day]
            if day_assignments:
                print(f"\n{DAYS[day]}:")
                day_assignments.sort(key=lambda x: x.timeslot.start_time)
                effective_assignments = _group_consecutive_lab_assignments(day_assignments)
                
                for item in effective_assignments:
                    if not item['start_time'] or not item['end_time']:
                        continue
                        
                    session_type = item['assignment_obj'].session_type.title()
                    if item['assignment_obj'].session_type == "lab":
                        batch_info = f" (Batch {item['assignment_obj'].lab_batch})" if item['assignment_obj'].lab_batch else ""
                        session_type = f"Lab{batch_info}"
                    
                    print(f"  {item['start_time'].strftime('%H:%M')}-{item['end_time'].strftime('%H:%M')}: "
                          f"{item['assignment_obj'].course.code} - {session_type} "
                          f"({item['assignment_obj'].student_group.name})")

# Additional utility function for generating all visualizations at once
def generate_all_visualizations(solution, base_output_dir="timetable_outputs"):
    """Generate all types of visualizations and exports."""
    if not solution:
        print("No solution to visualize")
        return
    
    print("Generating comprehensive timetable visualizations...")
    
    # Create base directory
    os.makedirs(base_output_dir, exist_ok=True)
    
    # Generate student group timetables
    print("1. Generating enhanced student group timetables...")
    generate_enhanced_timetable_image(solution, 
                                    os.path.join(base_output_dir, "student_timetables"))
    
    # Generate teacher timetables
    print("2. Generating teacher timetables...")
    generate_teacher_timetable_images(solution, 
                                    os.path.join(base_output_dir, "teacher_timetables"))
    
    # Generate summary dashboard
    print("3. Generating summary dashboard...")
    generate_summary_dashboard(solution, 
                             os.path.join(base_output_dir, "dashboard"))
    
    # Export to various formats
    print("4. Exporting to CSV...")
    export_to_csv(solution, os.path.join(base_output_dir, "timetable_complete.csv"))
    
    print("5. Exporting to Excel...")
    export_to_excel(solution, os.path.join(base_output_dir, "timetable_complete.xlsx"))
    
    print(f"\n✅ All visualizations and exports completed!")
    print(f"📁 Check the '{base_output_dir}' directory for all outputs:")
    print(f"   - student_timetables/: Individual student group timetables")
    print(f"   - teacher_timetables/: Individual teacher timetables") 
    print(f"   - dashboard/: Summary dashboard with statistics")
    print(f"   - timetable_complete.csv: Complete CSV export")
    print(f"   - timetable_complete.xlsx: Complete Excel export")
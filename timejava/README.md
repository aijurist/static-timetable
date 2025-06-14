# Java Timetable Scheduling System

A comprehensive timetable scheduling system built in Java that generates optimized schedules for students and teachers across multiple departments. This project migrated from a Python implementation using OR-Tools to a standalone Java solution.

## Features

- **Multi-Department Support**: Handles 5 departments (AIML, CSBS, CSD, CSE, CSE-CS)
- **Multiple Class Types**: Theory, Lab, and Tutorial classes
- **Flexible Scheduling**: Supports multiple years (2, 3, 4) and sections (A, B, C)
- **Teacher Assignment**: Automatic teacher allocation with workload balancing
- **HTML Output**: Generates beautiful HTML timetables for easy viewing
- **GUI Viewers**: Multiple graphical interfaces for browsing timetables
- **Analysis Tools**: Comprehensive analysis of timetable coverage and gaps

## Project Structure

```
timejava/
├── src/main/java/org/timetable/
│   ├── domain/           # Core domain classes
│   ├── generator/        # Timetable generation logic
│   ├── gui/             # GUI applications
│   ├── analysis/        # Analysis tools
│   └── util/            # Utility classes
├── output/
│   ├── student_timetables/  # Generated student HTML timetables
│   └── teacher_timetables/  # Generated teacher HTML timetables
├── lib/                 # External libraries
└── target/classes/      # Compiled Java classes
```

## Quick Start

### Prerequisites

- Java 8 or higher
- Bash shell (for running scripts)

### Running the System

1. **Generate Timetables**:
   ```bash
   ./run-generator.sh
   ```

2. **View Timetables with GUI**:
   ```bash
   # Basic file tree viewer
   ./run-gui-viewer.sh
   
   # Enhanced viewer with tabs and filtering
   ./run-enhanced-gui.sh
   ```

3. **Analyze Timetables**:
   ```bash
   # Basic analysis
   ./run-analyzer.sh
   
   # Gap analysis
   ./run-gap-analyzer.sh
   ```

## Components

### Core Classes

- **Department**: Enum defining all supported departments
- **TimeSlot**: Represents time periods for classes
- **StudentGroup**: Represents student groups by department, year, and section
- **Teacher**: Represents teaching staff
- **Course**: Represents courses with different types (theory, lab, tutorial)

### Timetable Generation

- **StandaloneTimetableGenerator**: Main generator that creates optimized schedules
- **TimetableExporter**: Exports timetables to HTML format
- **TimetableConstants**: Defines time slots and scheduling constraints

### GUI Applications

1. **TimetableViewer**: Basic file tree viewer for browsing timetables
2. **EnhancedTimetableViewer**: Advanced viewer with:
   - Tabbed interface for students and teachers
   - Department and year filtering
   - Search functionality
   - Integrated HTML display

### Analysis Tools

1. **TimetableAnalyzer**: Comprehensive analysis including:
   - Department coverage statistics
   - Class type distribution
   - Teacher workload analysis
   - Cross-reference validation

2. **TimetableGapAnalyzer**: Gap analysis including:
   - Empty slot detection
   - Day-wise distribution analysis
   - Teacher utilization metrics

## Generated Output

### Student Timetables
- Format: `timetable_{DEPT}_Y{YEAR}_{SECTION}.html`
- Example: `timetable_CSE_Y3_A.html`

### Teacher Timetables
- Format: `timetable_teacher_{DEPT}_Teacher_{NUMBER}.html`
- Example: `timetable_teacher_CSE_Teacher_1.html`

## Analysis Results

The system provides comprehensive analysis of the generated timetables:

### Department Coverage
- **5 Departments**: AIML, CSBS, CSD, CSE, CSE-CS
- **Complete Coverage**: All departments have both student and teacher timetables
- **Years Covered**: 2nd, 3rd, and 4th year for all departments
- **Sections**: Variable by department (A, B, C for larger departments)

### Class Distribution
- **Theory Classes**: Primary instruction sessions
- **Lab Classes**: Practical/hands-on sessions
- **Tutorial Classes**: Small group discussion sessions

### Teacher Workload
- **5 Teachers per Department**: Balanced across all departments
- **Average Load**: ~18.68 classes per teacher
- **Load Range**: 10-31 classes per teacher
- **Fair Distribution**: Workload is reasonably balanced

## Scheduling Constraints

The system respects several constraints:

1. **Time Conflicts**: No teacher or student group can have overlapping classes
2. **Room Capacity**: Classes are assigned to appropriate room types
3. **Class Duration**: Different durations for theory (1 hour), lab (2 hours), and tutorial (1 hour)
4. **Weekly Distribution**: Classes are spread across Monday-Friday
5. **Teacher Availability**: Teachers are not over-scheduled

## Customization

### Adding New Departments
1. Update the `Department` enum
2. Add corresponding data in the generator
3. Update analysis patterns if needed

### Modifying Time Slots
1. Edit `TimetableConstants.TIME_SLOTS`
2. Adjust generation logic if needed

### Changing Class Types
1. Update course definitions in the generator
2. Modify HTML styling for new class types

## Troubleshooting

### Common Issues

1. **Compilation Errors**: Ensure Java 8+ is installed
2. **Missing Output**: Run the generator first before viewing
3. **GUI Not Starting**: Check if display is available (for headless systems)

### Logs and Debugging

The system provides console output during generation and analysis. Check the terminal output for any warnings or errors.

## Future Enhancements

- **Database Integration**: Store timetables in a database
- **Web Interface**: Browser-based timetable management
- **Conflict Resolution**: Advanced constraint solving
- **Export Formats**: PDF, Excel, and other formats
- **Real-time Updates**: Dynamic timetable modifications

## License

This project is open source and available under the MIT License.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Support

For issues or questions, please check the analysis output first, as it provides comprehensive information about timetable coverage and potential problems. 
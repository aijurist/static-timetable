# Department Timetable Viewer Features

## Overview
The new `DepartmentTimetableViewer` provides an enhanced GUI that organizes timetables by department in a hierarchical, sorted structure.

## Key Features

### 1. Department Organization
- **Hierarchical Structure**: Timetables are organized by department at the top level
- **Sorted Departments**: All departments are displayed in alphabetical order
- **Department Statistics**: Shows count of student and teacher timetables for each department

### 2. Student Timetables Organization
```
Department (X students, Y teachers)
├── Student Timetables
│   ├── Year 2 (N sections)
│   │   ├── Section A
│   │   ├── Section B
│   │   └── ...
│   ├── Year 3 (N sections)
│   └── Year 4 (N sections)
└── Teacher Timetables
    ├── Teacher 1
    ├── Teacher 2
    └── ...
```

### 3. Sorting Features
- **Departments**: Alphabetically sorted (AERO, AIDS, AIML, etc.)
- **Years**: Numerically sorted (Year 2, Year 3, Year 4)
- **Sections**: Alphabetically sorted (A, B, C, etc.)
- **Teachers**: Numerically sorted (Teacher 1, Teacher 2, etc.)

### 4. Enhanced User Interface
- **Tree Navigation**: Easy browsing through hierarchical structure
- **Status Bar**: Shows current selection and loading status
- **Responsive Layout**: Adjustable split pane between tree and content
- **Auto-Expansion**: Department nodes are automatically expanded for easy access

### 5. Department Coverage
The GUI displays all 19 departments:

**Large Departments (>10 sections):**
- CSE: 21 sections
- ECE: 14 sections  
- IT: 12 sections

**Medium Departments (5-10 sections):**
- AIML: 10 sections
- AIDS: 9 sections
- BT: 9 sections
- CSBS, EEE, BME, MECH: 6 sections each

**Small Departments (1-4 sections):**
- CSE-CS, CSD, AERO, AUTO, MCT, R&A, FT, CIVIL, CHEM: 3 sections each

## Usage

### Running the GUI
```bash
./run-department-gui.sh
```

### Navigation
1. **Select Department**: Click on any department to see its structure
2. **Browse Years**: Expand student timetables to see years
3. **View Sections**: Click on individual sections to view timetables
4. **Check Teachers**: Browse teacher timetables for each department

### Features in Action
- **Quick Overview**: See at a glance how many timetables each department has
- **Organized Browsing**: Navigate through years and sections systematically
- **Status Feedback**: Status bar shows what you're currently viewing
- **Efficient Loading**: Only loads HTML content when a timetable is selected

## Comparison with Previous GUI

| Feature | Old GUI | New Department GUI |
|---------|---------|-------------------|
| Organization | Flat file list | Hierarchical by department |
| Sorting | Basic file order | Multi-level sorting |
| Navigation | Manual file browsing | Structured tree navigation |
| Department View | Mixed together | Clearly segregated |
| Statistics | None | Department counts shown |
| User Experience | Basic | Enhanced with status and organization |

## Benefits

1. **Better Organization**: Easy to find timetables for specific departments
2. **Improved Navigation**: Hierarchical structure makes browsing intuitive
3. **Clear Overview**: Immediately see which departments have timetables
4. **Sorted Display**: Everything is in logical, sorted order
5. **Enhanced UX**: Status feedback and responsive design
6. **Scalable**: Works well with large numbers of timetables across many departments

This new GUI makes it much easier to manage and view the comprehensive timetable system covering all 19 departments with 126 student groups and 65 teachers. 
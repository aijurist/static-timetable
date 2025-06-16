
# ==========================================
# ADVANCED TIMETABLE APPLICATION LAUNCHER (PowerShell)
# ==========================================
# Enhanced argument parser for department selection
# ==========================================

param(
    [string]$DepartmentType = "both",
    [int]$Minutes = 20,
    [string[]]$Files = @(),
    [switch]$Help
)

# Function to show usage
function Show-Help {
    Write-Host @"
TIMETABLE SOLVER - Department Selection Tool (PowerShell)

USAGE:
    .\run-app.ps1 [OPTIONS] [DEPARTMENT_TYPE]

DEPARTMENT TYPES:
    core        Run only core departments (ECE, EEE, MECH, BME, AUTO, AERO, MCT, R&A, BT, CHEM, CIVIL, FT)
    cse         Run only computer science departments (CSE, IT, AIDS, CSBS, CSD, AIML)
    both        Run both core and computer science departments (default)
    custom      Use custom CSV files (specify with -Files option)

OPTIONS:
    -Minutes MINUTES        Set solver time limit in minutes (default: 20)
    -Files FILE1,FILE2      Comma-separated list of custom CSV files
    -Help                   Show this help message

EXAMPLES:
    .\run-app.ps1 core                              # Run only core departments
    .\run-app.ps1 cse                               # Run only CSE departments  
    .\run-app.ps1 both                              # Run both (default)
    .\run-app.ps1 -Minutes 30 core                  # Run core departments for 30 minutes
    .\run-app.ps1 custom -Files "data/courses/my_custom.csv"
    .\run-app.ps1 custom -Files "file1.csv","file2.csv"

ENVIRONMENT VARIABLES:
    `$env:SOLVER_MINUTES    Override default solver time

"@
}

# Show help if requested
if ($Help) {
    Show-Help
    exit 0
}

# Handle positional arguments
if ($args.Count -gt 0) {
    $DepartmentType = $args[0]
}

# Override minutes from environment variable if set
if ($env:SOLVER_MINUTES) {
    $Minutes = [int]$env:SOLVER_MINUTES
}

# Validate department type
$validDepartments = @("core", "cse", "both", "custom")
if ($DepartmentType -notin $validDepartments) {
    Write-Host "[ERROR] Invalid department type: $DepartmentType" -ForegroundColor Red
    Write-Host "Valid options: $($validDepartments -join ', ')" -ForegroundColor Yellow
    Write-Host "Use -Help for usage information" -ForegroundColor Yellow
    exit 1
}

# Determine course files based on department type
$CourseFiles = @()
switch ($DepartmentType) {
    "core" {
        $CourseFiles = @("data/courses/core_dept_red.csv")
    }
    "cse" {
        $CourseFiles = @("data/courses/cse_dept_red.csv")
    }
    "both" {
        $CourseFiles = @("data/courses/cse_dept_red.csv", "data/courses/core_dept_red.csv")
    }
    "custom" {
        if ($Files.Count -eq 0) {
            Write-Host "[ERROR] Custom department type requires at least one file specified with -Files option" -ForegroundColor Red
            Write-Host "Use -Help for usage information" -ForegroundColor Yellow
            exit 1
        }
        $CourseFiles = $Files
    }
}

# Validate that course files exist
foreach ($file in $CourseFiles) {
    if (-not (Test-Path $file)) {
        Write-Host "[ERROR] Course file not found: $file" -ForegroundColor Red
        exit 1
    }
}

# Join course files with comma for Java app
$CourseFilesStr = $CourseFiles -join ","

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "OPTIMIZED TIMETABLE SOLVER" -ForegroundColor Cyan
Write-Host "Department type: $DepartmentType" -ForegroundColor Green
Write-Host "Course files   : $CourseFilesStr" -ForegroundColor Green
Write-Host "Solver minutes : $Minutes" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan

Write-Host "[INFO] Checking Maven..." -ForegroundColor Yellow
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Maven is not installed. Please install Maven first." -ForegroundColor Red
    exit 1
}

if (-not (Test-Path "lib") -or (Get-ChildItem "lib" -ErrorAction SilentlyContinue).Count -eq 0) {
    Write-Host "[INFO] Downloading libraries..." -ForegroundColor Yellow
    & .\download-libs.sh
}

Write-Host "[INFO] Building project (may take a moment)..." -ForegroundColor Yellow
& mvn -q clean package

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Maven build failed" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] Starting timetable solver..." -ForegroundColor Yellow
& java "-Dsolver.minutes=$Minutes" -jar "target/timejava-1.0-SNAPSHOT.jar" $CourseFilesStr "data" 
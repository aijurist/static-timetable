from timetable.config import DEPARTMENT_DATA
sum_st = 0
for dept, data in DEPARTMENT_DATA.items():
    print(f"Department: {dept}")
    for year, strength in data.items():
        sum_st = sum_st + strength

print(sum_st)
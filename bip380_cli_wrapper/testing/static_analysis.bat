@echo off
echo Running Static Analysis Tests
echo ============================

echo.
echo Running PMD analysis... (if you have PMD installed)
if exist "C:\path\to\pmd\bin\pmd.bat" (
    C:\path\to\pmd\bin\pmd.bat -d src -R rulesets/java/quickstart.xml -f text
) else (
    echo PMD not found, skipping...
)

echo.
echo Running SpotBugs analysis... (if you have SpotBugs installed)
if exist "C:\path\to\spotbugs\bin\spotbugs.bat" (
    C:\path\to\spotbugs\bin\spotbugs.bat -textui -effort:max -low -output spotbugs_report.txt -sourcepath src bin
) else (
    echo SpotBugs not found, skipping...
)

echo.
echo Analysis complete!
pause
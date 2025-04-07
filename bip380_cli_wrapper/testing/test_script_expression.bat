@echo off
echo Testing script-expression command...

echo.
echo Basic test:
java -cp "bin;lib/*" bip380_cli_wrapper.Wrapper script-expression "raw(deadbeef)"

pause
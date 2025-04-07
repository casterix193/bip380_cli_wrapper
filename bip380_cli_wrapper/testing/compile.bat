@echo off
echo Compiling BIP380 CLI wrapper...

REM Create bin directory if it doesn't exist
mkdir bin 2>nul

REM Compile each file individually
javac -d bin -cp "lib/*;src" src/bip380_cli_wrapper/Wrapper.java src/bip380_cli_wrapper/Command.java src/bip380_cli_wrapper/ArgParser.java src/bip380_cli_wrapper/DeriveKeyCommand.java src/bip380_cli_wrapper/KeyExpressionCommand.java src/bip380_cli_wrapper/ScriptExpressionCommand.java src/bip380_cli_wrapper/ErrorHandler.java

if %errorlevel% equ 0 (
    echo Compilation successful!
) else (
    echo Compilation failed with error code %errorlevel%
)

pause
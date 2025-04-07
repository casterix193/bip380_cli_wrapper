@echo off
echo === BIP380 CLI WRAPPER TESTING ===
echo.

echo === Testing derive-key command ===
java -cp "bin;lib/*" bip380_cli_wrapper.Wrapper derive-key 000102030405060708090a0b0c0d0e0f
echo.

echo === Testing key-expression command ===
java -cp "bin;lib/*" bip380_cli_wrapper.Wrapper key-expression 0260b2003c386519fc9eadf2b5cf124dd8eea4c4e68d5e154050a9346ea98ce600
echo.

echo === Testing script-expression basic parsing ===
java -cp "bin;lib/*" bip380_cli_wrapper.Wrapper script-expression "raw(deadbeef)"
echo.

echo === Testing script-expression checksum computation ===
java -cp "bin;lib/*" bip380_cli_wrapper.Wrapper script-expression --compute-checksum "raw(deadbeef)"
echo.

echo === Testing script-expression checksum verification ===
java -cp "bin;lib/*" bip380_cli_wrapper.Wrapper script-expression --verify-checksum "raw(deadbeef)#GK9Stf9"
echo.

echo === Testing error handling ===
java -cp "bin;lib/*" bip380_cli_wrapper.Wrapper script-expression "invalid_format()"
echo.

pause
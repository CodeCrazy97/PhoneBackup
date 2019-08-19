@echo off

REM Below will maximize the screen.
if not "%1" == "max" start /MAX cmd /c %0 max & exit/b

cd dist
java -jar DataBackupJava.jar
pause
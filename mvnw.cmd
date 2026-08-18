@echo off
setlocal
if exist "%~dp0.tools\maven\apache-maven-3.9.6\bin\mvn.cmd" (
    "%~dp0.tools\maven\apache-maven-3.9.6\bin\mvn.cmd" %*
) else (
    mvn %*
)

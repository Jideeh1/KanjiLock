@echo off
setlocal
cd /d "%~dp0"
set LOG=%~dp0claude-build.log
echo === kanjilock release build started %DATE% %TIME% === > "%LOG%"
call gradlew.bat --console=plain --stacktrace assembleGithubRelease >> "%LOG%" 2>&1
set RC=%ERRORLEVEL%
echo. >> "%LOG%"
echo CLAUDE_EXIT=%RC% >> "%LOG%"
echo CLAUDE_DONE >> "%LOG%"
endlocal

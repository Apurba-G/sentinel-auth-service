@echo off
TITLE Ecommerce Auth Service - Automated Git Push
echo ==============================================================================
echo                 AUTOMATED GIT COMMIT & PUSH SCRIPT
echo ==============================================================================
echo.

SET COMMIT_MSG=%~1
IF "%COMMIT_MSG%"=="" (
    SET COMMIT_MSG=Auto-commit and push updates: %DATE% %TIME%
)

echo Adding modified files to Git staging...
git add .

echo.
echo Committing changes with message: "%COMMIT_MSG%"
git commit -m "%COMMIT_MSG%"

echo.
echo Pushing changes to remote repository...
git push

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo ==============================================================================
    echo [ERROR] GIT PUSH FAILED! Check remote repository credentials or connection.
    echo ==============================================================================
    exit /b %ERRORLEVEL%
) ELSE (
    echo.
    echo ==============================================================================
    echo [SUCCESS] CODE SUCCESSFULLY COMMITTED AND PUSHED TO GITHUB!
    echo ==============================================================================
)

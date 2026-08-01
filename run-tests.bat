@echo off
TITLE Ecommerce Auth Service - Automated Test Execution
echo ==============================================================================
echo                 AUTOMATED TEST SUITE RUNNER
echo ==============================================================================
echo Starting Maven Test Suite for ecommerce-auth-service...
echo.

call mvn clean test

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo ==============================================================================
    echo [ERROR] TEST SUITE FAILED! Please check error logs above.
    echo ==============================================================================
    exit /b %ERRORLEVEL%
) ELSE (
    echo.
    echo ==============================================================================
    echo [SUCCESS] ALL TESTS PASSED SUCCESSFULLY!
    echo ==============================================================================
)

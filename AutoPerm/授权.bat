@echo off
color 0B

:: Script Information
set PACKAGE_NAME=com.flowercat.rfmouse
set DEVICE_ADMIN_RECEIVER=com.flowercat.rfmouse.receiver.MyDeviceAdminReceiver
set ACCESSIBILITY_SERVICE=com.flowercat.rfmouse/com.flowercat.rfmouse.service.FlowerMouseService
set ADB_PATH=%~dp0adb.exe

echo.
echo ====================================================
echo             Flowercat Authorization Script v1.4
echo ====================================================
echo.
echo  This script will automatically grant the following permissions:
echo  - Read/Write Storage
echo  - System Alert Window
echo  - Write Secure Settings
echo  - Accessibility Service
echo.
echo  Before you start, please ensure your phone is:
echo  1. Connected to the computer
echo  2. USB Debugging is enabled
echo.
echo  If a USB debugging prompt appears on your phone, please tap "Allow".
echo.
pause

:: Check for adb.exe
if not exist "%ADB_PATH%" (
    echo.
    echo ----------------------------------------------------
    echo   Error: adb.exe not found! Please place this script
    echo   in the same directory as adb.exe.
    echo ----------------------------------------------------
    pause
    exit
)

echo.
echo ----------------------------------------------------
echo   Checking for device connection...
echo ----------------------------------------------------
"%ADB_PATH%" start-server > nul
"%ADB_PATH%" get-state > nul
if errorlevel 1 (
    echo.
    echo   Device not connected. Please check:
    echo   - Is your phone connected to the PC?
    - Is USB debugging enabled?
    - Did you tap "Allow" on the USB debugging prompt?
    echo.
    echo   Please fix the issue and run the script again.
    echo.
    pause
    exit
) else (
    echo.
    echo   Device connected. Starting authorization...
)

echo.
echo ====================================================
echo   Granting basic permissions...
echo ====================================================

:: Grant Storage permissions
echo    Granting Read/Write Storage permissions...
"%ADB_PATH%" shell pm grant %PACKAGE_NAME% android.permission.READ_EXTERNAL_STORAGE > nul
"%ADB_PATH%" shell pm grant %PACKAGE_NAME% android.permission.WRITE_EXTERNAL_STORAGE > nul

:: Grant System Alert Window permission
echo    Granting System Alert Window permission...
"%ADB_PATH%" shell pm grant %PACKAGE_NAME% android.permission.SYSTEM_ALERT_WINDOW > nul

:: Grant Write Secure Settings permission
echo    Granting Write Secure Settings permission...
"%ADB_PATH%" shell pm grant %PACKAGE_NAME% android.permission.WRITE_SECURE_SETTINGS > nul

echo.
echo ----------------------------------------------------
echo   All basic permissions have been granted.
echo ----------------------------------------------------
pause

echo.
echo ====================================================
echo   Device Admin and Accessibility Service Authorization...
echo ====================================================

:: Attempt to activate Device Admin directly.
:: If the command fails, it's likely due to the Android version,
:: and the script will simply continue.
echo    Attempting to activate Device Admin...
echo   Please tap "Activate" or "Agree" on your phone if prompted.
"%ADB_PATH%" shell dpm set-active-admin %PACKAGE_NAME%/%DEVICE_ADMIN_RECEIVER% > nul 2>&1

echo.
echo ----------------------------------------------------
echo   Activating Accessibility Service...
echo ----------------------------------------------------

:: Get current enabled services
set "CURRENT_SERVICES="
for /f "delims=" %%s in ('"%ADB_PATH%" shell settings get secure enabled_accessibility_services') do (
    set "CURRENT_SERVICES=%%s"
)

:: Check if our service is already enabled
echo "%CURRENT_SERVICES%" | findstr /i "%ACCESSIBILITY_SERVICE%" > nul
if not errorlevel 1 (
    echo    Accessibility Service is already active. No action needed.
) else (
    :: Update the services list
    echo    Adding accessibility service...
    "%ADB_PATH%" shell settings put secure enabled_accessibility_services "%ACCESSIBILITY_SERVICE%" > nul 2>&1
    
    :: Enable accessibility
    echo    Enabling accessibility...
    "%ADB_PATH%" shell settings put secure accessibility_enabled 1 > nul 2>&1

    echo    Accessibility Service activated successfully.
)

echo.
echo ====================================================
echo   Authorization complete!
echo ====================================================
echo.
echo   If the app is still not working, please check your phone's settings
echo   to make sure **"Device Admin"** and **"Accessibility Service"** are enabled.
echo.
pause
exit
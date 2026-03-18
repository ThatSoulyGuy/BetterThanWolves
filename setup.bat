@echo off
setlocal enabledelayedexpansion

echo === Better Than Wolves Build Setup ===
echo.

set "PROJECT_DIR=%~dp0"
set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

if "%~1"=="" (
    echo No MCP workspace provided.
    echo.
    echo To set up the build environment, you need MCP-decompiled vanilla
    echo Minecraft 1.5.2 source code. There are two ways to get this:
    echo.
    echo   Option A: Provide an existing MCP workspace
    echo     setup.bat C:\path\to\mcp
    echo.
    echo   Option B: Use RetroMCP ^(recommended^)
    echo     1. Install Python 3
    echo     2. pip install retromcp
    echo     3. mkdir mcp ^&^& cd mcp
    echo     4. retromcp setup 1.5.2
    echo     5. retromcp decompile
    echo     6. cd .. ^&^& setup.bat mcp
    echo.
    echo   Option C: Use classic MCP 7.51
    echo     1. Download MCP 7.51 from the MCP archive
    echo     2. Place minecraft.jar ^(1.5.2^) in mcp\jars\bin\
    echo     3. Place minecraft_server.jar ^(1.5.2^) in mcp\jars\
    echo     4. Run: cd mcp ^&^& python runtime\decompile.py
    echo     5. cd .. ^&^& setup.bat mcp
    echo.
    exit /b 1
)

set "MCP_DIR=%~1"
echo Using MCP workspace: %MCP_DIR%

rem ---------------------------------------------------------------------------
rem Locate decompiled source
rem ---------------------------------------------------------------------------
set "CLIENT_SRC="
set "SERVER_SRC="

if exist "%MCP_DIR%\src\minecraft\net\minecraft\src" (
    set "CLIENT_SRC=%MCP_DIR%\src\minecraft"
)

if exist "%MCP_DIR%\src\minecraft_server\net\minecraft\src" (
    set "SERVER_SRC=%MCP_DIR%\src\minecraft_server"
)

if "%CLIENT_SRC%"=="" (
    echo ERROR: Could not find decompiled client source in MCP workspace.
    echo Expected: %MCP_DIR%\src\minecraft\net\minecraft\src\
    exit /b 1
)

echo Found client source: %CLIENT_SRC%
if not "%SERVER_SRC%"=="" echo Found server source: %SERVER_SRC%

rem ---------------------------------------------------------------------------
rem Copy vanilla source
rem ---------------------------------------------------------------------------
echo.
echo Copying vanilla source to vanilla\ ...

if exist "%PROJECT_DIR%\vanilla\client" rmdir /s /q "%PROJECT_DIR%\vanilla\client"
if exist "%PROJECT_DIR%\vanilla\server" rmdir /s /q "%PROJECT_DIR%\vanilla\server"
mkdir "%PROJECT_DIR%\vanilla\client"
mkdir "%PROJECT_DIR%\vanilla\server"

xcopy /s /e /q /y "%CLIENT_SRC%\*" "%PROJECT_DIR%\vanilla\client\" >nul
echo   Copied client source

if not "%SERVER_SRC%"=="" (
    xcopy /s /e /q /y "%SERVER_SRC%\*" "%PROJECT_DIR%\vanilla\server\" >nul
    echo   Copied server source
) else (
    xcopy /s /e /q /y "%CLIENT_SRC%\*" "%PROJECT_DIR%\vanilla\server\" >nul
    echo   Copied client source as server base
)

rem ---------------------------------------------------------------------------
rem Apply BTW patches
rem ---------------------------------------------------------------------------
echo.
echo Applying BTW patches ...

if exist "%PROJECT_DIR%\Src\patch.txt" (
    echo   Applying shared patches ...
    pushd "%PROJECT_DIR%\vanilla\client"
    patch -R -p0 --binary -i "%PROJECT_DIR%\Src\patch.txt"
    popd
)

if exist "%PROJECT_DIR%\SrcClient\patch.txt" (
    echo   Applying client-only patches ...
    pushd "%PROJECT_DIR%\vanilla\client"
    patch -R -p0 --binary -i "%PROJECT_DIR%\SrcClient\patch.txt"
    popd
)

if exist "%PROJECT_DIR%\SrcServer\patch.txt" (
    echo   Applying server patches ...
    pushd "%PROJECT_DIR%\vanilla\server"
    patch -R -p0 --binary -i "%PROJECT_DIR%\SrcServer\patch.txt"
    popd
)

rem ---------------------------------------------------------------------------
rem Done
rem ---------------------------------------------------------------------------
echo.
echo === Setup complete! ===
echo.
echo You can now build with:
echo   gradlew.bat jar          - Build client JAR
echo   gradlew.bat serverJar    - Build server JAR
echo   gradlew.bat buildAll     - Build both
echo.
echo The compiled JARs will be in build\libs\

endlocal

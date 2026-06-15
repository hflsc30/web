@echo off
setlocal
set GPG=E:\Git\Git\usr\bin\gpg.exe
echo %* | findstr /c:"--version" > nul
if %errorlevel% equ 0 (
    "%GPG%" %* 2>&1 | powershell -Command "$input | ForEach-Object { $_ -replace '(gpg \(GnuPG\) [\d.]+)-unknown', '$1' }"
) else (
    "%GPG%" %*
)
exit /b %errorlevel%

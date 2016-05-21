@echo off
title FrostBite 3 Tools Console
echo DO NOT CLOSE THIS WINDOW, ITS REQUIRED!
echo JavaFX does only works on Java 8+
echo.
echo Please type in any additional arguments. [ENTER]
set /p commandLineArgs=
java -Xmx2048m -cp FrostBite3Editor-0.4.0-SNAPSHOT.jar -Djava.library.path=lib\native\windows tk.greydynamics.Game.Core %commandLineArgs%
echo Press any key to EXIT!
pause >nul
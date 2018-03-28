@echo off

rem
rem # ==================================================================== ###
rem                                                                          #
rem  The File Exchange Interface (FEI) File Addition Client Utility          #
rem                                                                          #
rem  Function:                                                               #
rem  Displays contents of files matching criteria to standard out.           #
rem  Useful for piping contents to filter utilities.                         #
rem                                                                          #
rem  Copyright (c) 2006 by the California Institute of Technology.           #
rem  ALL RIGHTS RESERVED.  United States Government Sponsorship              #
rem  acknowledged. Any commercial use must be negotiated with the            #
rem  Office of Technology at the California Institute of Technology.         #
rem                                                                          #
rem  The technical data in this document (or file) is controlled for         #
rem  export under the Export Administration Regulations (EAR), 15 CFR,       #
rem  Parts 730-774. Violations of these laws are subject to fines and        #
rem  penalties under the Export Administration Act.                          #
rem                                                                          #
rem                                                                          #
rem  Created:                                                                #
rem  Nov. 02, 2004 by Nicholas Toole {Nicholas.Toole@jpl.nasa.gov}           #
rem                                                                          #
rem  Modifications:                                                          # 
rem  Apr 27, 2006 by ntt: Fixed spaces in dirnames issue                     #
rem                                                                          #
rem # ==================================================================== ###
rem 
rem $Id: fei5display.bat,v 1.2 2005/02/03 22:49:38 ntt Exp $
rem 


REM =============================================

REM Set Java class to run with JRE
set LJAVACLASS=jpl.mipl.mdms.FileService.komodo.client.UtilClient

REM =============================================

REM Read all command line arguments
set LMYNAME=%0%
set LARGS=
:loop
if [%1] == [] goto endloop
	set LARGS=%LARGS% %1
	shift
	goto loop
:endloop


REM =============================================

REM Check FEI5 env

if "%FEI5%" == "" GOTO NO_FEI5
goto :FEI5_DEFD
:NO_FEI5
echo FEI5 environment variable not set.
echo Please set FEI5 environment variable.
goto ERROR
:FEI5_DEFD


REM =============================================

REM Add specific entries to classpath, if necessary
set LPRECLASSPATH=
if NOT "%CLASSPATH%" == "" set LPRECLASSPATH=%CLASSPATH%

REM Call mdms config batch to initialize common variables
if "%LPRECLASSPATH%" == "" goto NO_ARGS
CALL "%FEI5%"\mdmsconfig.bat "%LPRECLASSPATH%"
goto CONFIGD
:NO_ARGS
CALL "%FEI5%"\mdmsconfig.bat
goto CONFIGD


REM Return value if successful should be JVM args (including java cmd)
:CONFIGD
if "%KOMODO_SCRIPT_EXIT_STATUS%" == "1" goto ERROR
set LJVMARGS=%KOMODO_SCRIPT_RETURN_VALUE%

REM =============================================

REM Add custom JVM arguments to end of LJVM_ARGS, if necessary
set LJVMARGS=%LJVMARGS% -Dmdms.user.operation=komodo.util.display
set LJVMARGS=%LJVMARGS% -Dmdms.user.application=%LMYNAME%

REM =============================================

REM Create invocation command using vm args, main class, and prog args
set LINVOKE_CMD=%LJVMARGS% %LJAVACLASS% %LARGS%

REM Invoke command
%LINVOKE_CMD%
set KOMODO_SCRIPT_EXIT_STATUS=0
goto END

REM =============================================

REM In case error occurs
:ERROR
echo %0%: Error occurred.  Exiting...
set KOMODO_SCRIPT_EXIT_STATUS=1
GOTO END


REM =============================================

REM Reset local variables
:END
set LPRECLASSPATH=
set LJVMARGS=
set LJAVACLASS=
set LARGS=
set LINVOKE_CMD=
set LMYNAME=
set KOMODO_SCRIPT_RETURN_VALUE=

REM =============================================

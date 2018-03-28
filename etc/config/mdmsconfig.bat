rem @echo off

REM 
REM # ==================================================================== ###
REM                                                                          #
REM  The File Exchange Interface (FEI) MDMS Config Utility                   #
REM                                                                          #
REM  Function:                                                               #
REM  Provides configuration base for MDMS Batch commands, accepting an       #
REM  initial classpath and building the Java invocation string including     #
REM  the necessary JARS and options                                          #
REM                                                                          #
REM  Parameters:                                                             #
REM    Pre-initialized classpath, surrounded by 1 pair of quotes (Optional)  #
REM                                                                          #
REM  Variables set by script:                                                #
REM    KOMODO_SCRIPT_RETURN_VALUE - JVM_ARGUMENTS value.                     #
REM    KOMODO_SCRIPT_EXIT_STATUS  - 0 if successful, 1 otherwise             #
REM                                                                          #
REM  Copyright (c) 2006 by the California Institute of Technology.           #
REM  ALL RIGHTS RESERVED.  United States Government Sponsorship              #
REM  acknowledged. Any commercial use must be negotiated with the            #
REM  Office of Technology at the California Institute of Technology.         #
REM                                                                          #
REM  The technical data in this document (or file) is controlled for         #
REM  export under the Export Administration Regulations (EAR), 15 CFR,       #
REM  Parts 730-774. Violations of these laws are subject to fines and        #
REM  penalties under the Export Administration Act.                          #
REM                                                                          #
REM                                                                          #
REM  Created:                                                                #
REM  Oct. 21, 2004 by Nicholas Toole {Nicholas.Toole@jpl.nasa.gov}           #
REM                                                                          #
REM Modified:                                                                #
REM  Apr. 27, 2006 by Nicholas Toole {Nicholas.Toole@jpl.nasa.gov}           #
REM  If FEI was installed in a directory with spaces, launchers would either #
REM  fail or arguments would be corrupted.  Fixed by selectively wrapping    #
REM  values in quotes (i.e. FEI5)                                            #
REM                                                                          #
REM # ==================================================================== ###
REM 
REM $Id: mdmsconfig.bat,v 1.10 2005/02/03 22:24:31 ntt Exp $
REM 


REM ==========================================================================
REM == Entry point. Sets LPRE_CLASSPATH to argument if necessary then       ==
REM == calls main 'subroutine' to begin processing.                         ==
REM ==========================================================================

REM Declare local variables
set LPRE_CLASSPATH=
set LRESTART_DIR=
set LRUNJARS=
set LJVM_ARGS=
set LJVM_CLASSPATH=
set LJAVA=

REM set local classpath argument if parameter specified
REM assume argument was pre-quoted before calling this batch
if NOT Z%1 == "Z" set LPRE_CLASSPATH="%1"


goto MAIN


REM ==========================================================================
REM == Main 'routine'                                                       ==
REM == Calls 'subroutines' to set variables in order to build final         ==
REM == JVM argument list.                                                   ==
REM ==========================================================================

:MAIN


REM Checks that FEI5 variable was set prior to script entry
goto CKFEI5_A
:CKFEI5_B

REM Sets LJAVA executable
goto CKJVM_A
:CKJVM_B

REM Sets LJVM_CLASSPATH variable
goto CP_A
:CP_B

REM Set LRESTART_DIR variable
goto RSDIR_A
:RSDIR_B

REM Build JVM argument string, sets LJVM_ARGS
goto VMARGS_A
:VMARGS_B


goto DONE


REM ==========================================================================
REM == Check FEI5 environment variable                                      ==
REM ==========================================================================


:CKFEI5_A

REM If FEI5 not set, use default
if "%FEI5%" == "" GOTO NO_FEI5
goto CHECKDIR

REM FEI5 not defined
:NO_FEI5
echo FEI5 environment variable not set.
echo Please set FEI5 environment variable.
goto CKFEI5_E

REM Check that FEI5 value exists
:CHECKDIR
if exist "%FEI5%" goto CKFEI5_D
echo FEI5 directory "%FEI5%" does not exist.
echo Please check FEI5 environment variable.
GOTO CKFEI5_E


REM set return values for success
:CKFEI5_D
GOTO CKFEI5_Z


REM Error - set return data accordingly
:CKFEI5_E
goto ERROR

REM Unset local environment variables
:CKFEI5_Z
goto CKFEI5_B


REM ==========================================================================
REM == Check JVM                                                            ==
REM == Sets LJAVA variable to the correct java executable                   ==
REM ==========================================================================


:CKJVM_A
set JRE_HOME=

REM Check if JAVA_HOME is defined
if "Z%JAVA_HOME%" == "Z" GOTO NO_JHOME
set JRE_HOME="%JAVA_HOME%"
if NOT EXIST %JRE_HOME% GOTO JHOME_ER
set LJAVA=%JRE_HOME%\bin\java

GOTO CKJVM_Z

:NO_JHOME
REM echo Unable to locate Java executables.
REM echo Please set environemnt JAVA_HOME to the Java installation directory.
REM GOTO CKJVM_E
set LJAVA=java
GOTO CKJVM_Z

:JHOME_ER
echo Current value for JAVA_HOME could not be found: "%JAVA_HOME%"
echo Please set environemnt JAVA_HOME to the Java installation directory.
GOTO CKJVM_E

REM Set return values for success
:CKJVM_D
set LJAVA=%JRE_HOME%\bin\java
GOTO CKJVM_Z

:CKJVM_E
set JRE_HOME=
GOTO ERROR

:CKJVM_Z
set JRE_HOME=
goto CKJVM_B


REM ==========================================================================
REM == Prepare classpath                                                    ==
REM == Sets the LJVM_CLASSPATH variable to include shared and passed        ==
REM == in segments of classpath                                             ==
REM ==========================================================================


:CP_A
REM Get the JAR directory
set JARS_DIR="%FEI5%"\..\lib

REM Set RUNJARS to empty string
set RUNJARS=

REM Add Jars to RUNJARS list
set KOMODO_SCRIPT_RETURN_VALUE=
PUSHD "%JARS_DIR%"
for %%i in ("*.jar") do call "%FEI5%"\appKSRV.bat %JARS_DIR%\%%i
POPD
set RUNJARS=%KOMODO_SCRIPT_RETURN_VALUE%
set KOMODO_SCRIPT_RETURN_VALUE=

REM Combine pre classpath with runjars if necessary
REM then set variable LJVM_CLASSPATH
if "%LPRE_CLASSPATH%" == "" goto NO_PRECP
if "%V2HTML%" == "" goto NO_PRECP
set LJVM_CLASSPATH=%LPRE_CLASSPATH%;%RUNJARS%;"%FEI5%"
goto JVMCP_SET

:NO_PRECP
set LJVM_CLASSPATH=%RUNJARS%
goto JVMCP_SET

:JVMCP_SET
goto CP_Z


REM Error - set return data accordingly
:CP_E
set JARS_DIR=
set RUNJARS=
set KOMODO_SCRIPT_RETURN_VALUE=
goto ERROR


REM Unset local environment variables
:CP_Z
set JARS_DIR=
set RUNJARS=
goto CP_B

REM ==========================================================================
REM == Sets the restart directory as defined by FEI5CCDIR if set, or        ==
REM == to the C:\ root otherwise.                                           ==
REM == Sets the LRESTART_DIR variable.                                      ==
REM ==========================================================================

:RSDIR_A
REM Set Restart dir variable to empty string
set REST_DIR=

REM Check of FEI5CCDIR is defined, if so use it, else goto USE_ROOT
if "%FEI5CCDIR%" == "" GOTO USE_ROOT
set REST_DIR="%FEI5CCDIR%"
GOTO RSDIR_D

REM Just use C:\ as the restart directory
:USE_ROOT
set REST_DIR=C:\
GOTO RSDIR_D

REM set return values for success
:RSDIR_D
set LRESTART_DIR=%REST_DIR%
GOTO RSDIR_Z

REM Error - set return data accordingly
:RSDIR_E
set REST_DIR=
goto ERROR

REM Unset local environment variables
:RSDIR_Z
set REST_DIR=
goto RSDIR_B

REM ==========================================================================
REM == Sets shared JVM Arguments                                            ==
REM == Sets the LJVM_ARGS variable.                                         ==
REM ==========================================================================

:VMARGS_A

REM Add arguments to JVM string
set JVM_ARGS=-classpath %LJVM_CLASSPATH%
set JVM_ARGS=%JVM_ARGS% -Djavax.net.ssl.trustStore="%FEI5%"\mdms-fei.keystore
set JVM_ARGS=%JVM_ARGS% -Dkomodo.restart.dir=%LRESTART_DIR%
set JVM_ARGS=%JVM_ARGS% -Dkomodo.config.dir="%FEI5%"
set JVM_ARGS=%JVM_ARGS% -Dkomodo.domain.file="%FEI5%"\domain.fei
set JVM_ARGS=%JVM_ARGS% -Dmdms.logging.config="%FEI5%"\mdms.lcf
set JVM_ARGS=%JVM_ARGS% -Dkomodo.public.key="%FEI5%"\public.der
set JVM_ARGS=%JVM_ARGS% -Dkomodo.query.interval=1
set JVM_ARGS=%JVM_ARGS% -Dkomodo.client.pulse=300
REM set JVM_ARGS=%JVM_ARGS% -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl
REM set JVM_ARGS=%JVM_ARGS% -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl
REM set JVM_ARGS=%JVM_ARGS% -Dorg.xml.sax.driver=org.apache.xerces.parsers.SAXParser
set JVM_ARGS=%JVM_ARGS% -Djava.net.preferIPv4Stack=true
set JVM_ARGS=%JVM_ARGS% -Dkomodo.filehandling.enable=true
set JVM_ARGS=%JVM_ARGS% -Dkomodo.net.ciphers=TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA
set JVM_ARGS=%JVM_ARGS% -Dkomodo.net.protocol=TLSv1.2
goto VMARGS_D


REM Done
:VMARGS_D
set LJVM_ARGS=%JVM_ARGS%
goto VMARGS_Z

REM Error - set return data accordingly
:VMARGS_E
set JVM_ARGS=
goto ERROR

REM Unset local environment variables
:VMARGS_Z
set JVM_ARGS=
goto VMARGS_B


REM ==========================================================================
REM == Exit point                                                           ==
REM == Sets return value to LJAVA LJVM_ARGS if successful                   ==
REM ==========================================================================


:ERROR
set KOMODO_SCRIPT_EXIT_STATUS=1
set KOMODO_SCRIPT_RETURN_VALUE=
goto END

:DONE
set KOMODO_SCRIPT_EXIT_STATUS=0
set KOMODO_SCRIPT_RETURN_VALUE=%LJAVA% %LJVM_ARGS%
goto END

:END
REM Unset local vars
set LPRE_CLASSPATH=
set LRESTART_DIR=
set LRUNJARS=
set LJVM_ARGS=
set LJVM_CLASSPATH=
set LJAVA=

REM ==========================================================================
REM ==========================================================================
REM ==========================================================================

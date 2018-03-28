@echo off

rem ====================================================================== ###
rem                                                                          #
rem  The File Exchange Interface (FEI) Environment Setup Script              #
rem                                                                          #
rem  Function:                                                               #
rem  Simple MS-DOS script to add FEI5 launchers to clients path.             #
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
rem  Oct. 07, 2003 by Rich Pavlovsky {rich.pavlovsky@jpl.nasa.gov}           #
rem                                                                          #
rem  Modifications:                                                          #
rem  Oct. 08, 2003 by Rich Pavlovsky {rich.pavlovsky@jpl.nasa.gov}           #
rem  Changed etc directory to config so it'll match users guide.             #
rem                                                                          #
rem  Dec. 15, 2003 by Rich Pavlovsky {rich.pavlovsky@jpl.nasa.gov}           #
rem  Fixed the current working directory problem and disabled echo output    #
rem                                                                          #
rem  Nov 4, 2004 by N Toole (nicholas.toole@jpl.nasa.gov}                    #
rem  Commented out setting of JAVA_HOME env.                                 # 
rem ====================================================================== ###
rem
rem $Id: use_FEI5.bat,v 1.5 2004/11/05 00:38:04 ntt Exp $

rem Get the current working directory
set CWD=%CD%

rem Set to the location of Java distribution (should be set prior)
rem Go to http://www.java.com to download Sun's Java
rem set JAVA_HOME=C:\progra~1\java_1.4.2

rem Set the FEI5 variable 
set FEI5=%CWD%\config

rem Add FEI5 launchers to the path
set PATH=%FEI5%\..\bin;%PATH%

rem Specify the name of the host running the Java Naming Service
set JAVA_NAMING_HOST=mdms.jpl.nasa.gov

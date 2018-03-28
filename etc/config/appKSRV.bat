rem ---------------------------------------------------------------------------
rem Append to KOMODO_SCRIPT_RETURN_VALUE. Useful for bulding paths with      #
rem Komodo/MDMS scripting world.                                             #
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
rem $Id: $                                                                   #
rem ---------------------------------------------------------------------------

rem Process the first argument
if ""%1"" == """" goto end
set KOMODO_SCRIPT_RETURN_VALUE=%KOMODO_SCRIPT_RETURN_VALUE%;%1
shift

rem Process the remaining arguments
:setArgs
if ""%1"" == """" goto doneSetArgs
set KOMODO_SCRIPT_RETURN_VALUE=%KOMODO_SCRIPT_RETURN_VALUE%;%1
shift
goto setArgs
:doneSetArgs
:end
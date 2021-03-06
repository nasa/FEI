#!/bin/sh 
#
### ==================================================================== ###
#                                                                          #
#  The File Exchange Interface (FEI) GUI Client Bootstrap Script           #
#                                                                          #
#  Function:                                                               # 
#  Launches the FEI GUI client application.  NOTE: The $FEI5 environment   #
#  variable must point to the directory containing the domain.fei and SSL  #
#  keystore files.                                                         # 
#                                                                          #
#  Copyright (c) 2006 by the California Institute of Technology.           #
#  ALL RIGHTS RESERVED.  United States Government Sponsorship              #
#  acknowledged. Any commercial use must be negotiated with the            #
#  Office of Technology at the California Institute of Technology.         #
#                                                                          #
#  The technical data in this document (or file) is controlled for         #
#  export under the Export Administration Regulations (EAR), 15 CFR,       #
#  Parts 730-774. Violations of these laws are subject to fines and        #
#  penalties under the Export Administration Act.                          #
#                                                                          #
#                                                                          #
#  Created:                                                                #
#  Oct. 28, 2004 by Nicholas Toole {nicholas.toole@jpl.nasa.gov}           #
#                                                                          #
#  Modifications:                                                          #
#  Dec. 10, 2004 by ntt: Appended GUI log config for mdms.logging.config   #
#                                                                          #
### ==================================================================== ###
#
# $Id: fei5gui.sh,v 1.10 2008/07/31 22:58:53 ntt Exp $
#

MYNAME=`basename $0`
WRITE="echo "
DEBUG=0
#umask 077


# Set variables
MAIN_CLASS=jpl.mipl.mdms.FileService.komodo.ui.savannah.Savannah

# Check for FEI5 environment variable
if [ "${FEI5}Z" = "Z" ]
  then
     ${WRITE} ${MYNAME}: [ERROR] FEI5 environment variable is not set. 1>&2
     exit 1
fi


# include function 'module'
if [ ! -f ${FEI5}/mdmsconfig.sh ]
  then
     ${WRITE} ${MYNAME}: [ERROR] Could not locate mdmsconfig.sh in FEI5 directory. 1>&2
     exit 1
fi
. ${FEI5}/mdmsconfig.sh


# get Java command
JAVACMD=`getCmd`
if [ $? -ne 0 ]; then exit 1; fi

if [ "${CLASSPATH}Z" = "Z" ]
  then 
     LCLASSPATH="."
  else
     LCLASSPATH=${CLASSPATH}
fi

# get JVM argument string
GUIFLAG=1
JVMARGS=`getJVMArgs ${DEBUG} ${LCLASSPATH} ${GUIFLAG}`
if [ $? -ne 0 ]; then exit 1; fi


# append specialized JVM arguments
JVMARGS="${JVMARGS} -Dmdms.user.operation=komodo.util.savannah"
JVMARGS="${JVMARGS} -Dmdms.logging.config=${FEI5}/mdmsgui.lcf"

if [ ${DEBUG} -eq 1 ]
  then
    echo ${JAVACMD} ${JVMARGS} ${MAIN_CLASS} "$@"
fi


# invoke Java command
${JAVACMD} ${JVMARGS} ${MAIN_CLASS} "$@"
EXIT_STAT=$?

if [ ${EXIT_STAT} -ne 0 ]
  then
   # exit with JAVA return status
   if [ ${DEBUG} -eq 1 ]
     then
       ${WRITE} ${MYNAME}: [ERROR] Program invocation exited with error status: ${EXIT_STAT} 1>&2
   fi
   exit ${EXIT_STAT}
fi

exit 0
   

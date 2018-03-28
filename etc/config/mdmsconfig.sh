
#
### ==================================================================== ###
#                                                                          #
#  The File Exchange Interface (FEI) Configuration Functions               #
#                                                                          #
#  Function:                                                               #
#  A series of funtions providing core services to FEI5 command            #
#  launchers under Bourne (sh) or Bash (bash) shells.  This file           #
#  is referenced by caller scripts that utilize functions.  Callers        #
#  source this file, and perform function.  Therefore, caller scripts      #
#  must run in the Bourne(Again) Shell.                                    #
#                                                                          #
#  To reference this file: Add ". mdmsconfig.sh" in caller, then call      #
#  functions by name.                                                      #
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
#  Created:                                                                #
#  Oct. 28, 2004 by Nicholas Toole {nicholas.toole@jpl.nasa.gov}           #
#                                                                          #
#                                                                          #
### ==================================================================== ###
#
# $Id: mdmsconfig.sh,v 1.19 2016/09/19 22:53:13 awt Exp $
#

# setting umask to provide owner all access, none to everyone else
umask 077

# Variable for minimum JRE version
MDMSCONFIG_DEFAULT_JRE_VERSION="1.5"

# Variable for 'echo' (Linux has been known to be a problem)
MDMSCONFIG_WRITE="echo"


##--------------------------------------------------------------------------


## 
# Subroutine to compare two float version values of form "[0-9]+.[0-9]+"
# @param 1 - version which is being checked
# @param 2 - minimum allowable version
# @returns 0 if param1 >= param2, 1 otherwise.
##

checkVersions()
{
  CV_NAME="checkVersions()"
  CV_SUCCESS=0
  CV_FAILURE=1

  if [ $# -ne 2 ]
    then
    ${MDMSCONFIG_WRITE} "${CV_NAME}: [ERROR] Missing required arguments" 1>&2
    exit 1
  fi
    
  ARG1=$1
  ARG2=$2

  I1=`echo $ARG1 | sed -n 's/^\([0-9][0-9]*\)\..*$/\1/p'`
  I2=`echo $ARG2 | sed -n 's/^\([0-9][0-9]*\)\..*$/\1/p'`

  if [ $I1 -eq $I2 ]
   then
    D1=`echo $ARG1 | sed -n 's/^[0-9][0-9]*\.\([0-9][0-9]*\)[^0-9]*.*$/\1/p'`
    D2=`echo $ARG2 | sed -n 's/^[0-9][0-9]*\.\([0-9][0-9]*\)[^0-9]*.*$/\1/p'`
    if [ $D1 -lt $D2 ]
     then echo $CV_FAILURE
     else echo $CV_SUCCESS
    fi
   elif [ $I1 -lt $I2 ]
    then echo $CV_FAILURE
   else echo $CV_SUCCESS
  fi
}


##--------------------------------------------------------------------------


## 
# Subroutine to check the user JVM version and return the correct
# Java executable.
# @param 1 - required version of JRE
# @returns Path of JRE java executable.
##

getCmdReqVersion() {
    
    GCRV_NAME="getCmdReqVersion()"
    
    # Assume Java executable is on the path and is the desired instance
    # We will check later for env vars that point
    GCRV_JAVACMD="java"
    
    if [ $# -ne 1 ]
    then
    ${MDMSCONFIG_WRITE} "${GCRV_NAME}: [ERROR] Missing required argument" 1>&2
       exit 1
    fi
    
    GCRV_REQVERSION=$1
     
    # set local V2JDK variable
#    if [ "Z${V2JDK}" = "Z" ]
#    then
#        GCRV_V2JDK="/usr/java"
#    else
#        GCRV_V2JDK="${V2JDK}"
#    fi
    

    # set local JavaHome variable
#    if [ "Z${JAVA_HOME}" = "Z" ]
#    then
#        GCRV_JAVA_CMD="${GCRV_V2JDK}"
#    else
#        GCRV_JAVA_CMD="${JAVA_HOME}/bin/java"
#    fi   

    # Cannot do following because that is another JVM call which can be expensive
    #java -version > /dev/null 2>&1
    #sh-2.05b$ if [ $? -ne 0 ]; then echo "NOT FOUND"; else echo "FOUND"; fi


    # Let's start by assuming that java is somewhere on the PATH and set our 
    # command to 'java'.  Next, check if JAVA_HOME is set.  If so use it!
    # If not, check if V2JDK is set and use it if it is.  Otherwise, stick
    # with our initial assumption    
    GCRV_JAVACMD="java"
    # set local JavaHome variable
    if [ "Z${JAVA_HOME}" != "Z" ]
    then 
        GCRV_JAVACMD="${JAVA_HOME}/bin/java"
    else
        if [ "Z${V2JDK}" != "Z" ]
        then
            GCRV_JAVACMD="${V2JDK}/bin/java"        
        fi  
    fi        
    
    # get the version number
    GCRV_VERSIONSTR=`${GCRV_JAVACMD} -version 2>&1`
    GCRV_VERSION=`echo ${GCRV_VERSIONSTR} | sed -n 's/^.*\([0-9][0-9]*\.[0-9][0-9]*\).*$/\1/p'`
    
    if [ "${GCRV_VERSION}Z" = "Z" ]
    then 
        ${MDMSCONFIG_WRITE} "${GCRV_NAME}: [ERROR] Could not determine JRE version." 1>&2
        ${MDMSCONFIG_WRITE} "${GCRV_NAME}: [ERROR] Current JRE command: ${GCRV_JAVACMD}." 1>&2
        exit 1
    fi
    
    # compare version to required version
    if [ `checkVersions ${GCRV_VERSION} ${GCRV_REQVERSION}` -eq 1 ]
    then
        ${MDMSCONFIG_WRITE} "${GCRV_NAME}: [ERROR] This software requires JRE version" 1>&2
        ${MDMSCONFIG_WRITE} "${GCRV_NAME}: ${GCRV_REQVERSION} or above." 1>&2
        ${MDMSCONFIG_WRITE} "${GCRV_NAME}: [ERROR] Current JRE command: ${GCRV_JAVACMD}." 1>&2
        exit 1
    fi
    
    # return the java invocation command
    echo ${GCRV_JAVACMD}
}


##--------------------------------------------------------------------------


## 
# Subroutine to check the user JVM version and return the correct
# Java executable using the default required JRE version
# @returns Path of JRE java executable.
##

getCmd() {
   RVAL=`getCmdReqVersion ${MDMSCONFIG_DEFAULT_JRE_VERSION}`
   if [ $? -ne 0 ]; then exit 1; fi
   echo $RVAL
}


##--------------------------------------------------------------------------

## 
# Subroutine to set and return the FEI5 environment variable
# @param 1 - default directory to which FEI5 can be set  
# @returns FEI5 environment value
##

getFEI5() {   
    SF_NAME="setFEI5()"
    
    if [ $# -ne 1 ]
    then
       ${MDMSCONFIG_WRITE} "${SF_NAME}: [ERROR] Missing required argument" 1>&2
       exit 1
    fi
    
    SF_DEF_VAL=$1
    
    # check if FEI5 is defined
    if [ "Z${FEI5}" = "Z" ]
      then
        ${MDMSCONFIG_WRITE} "${SF_NAME}: [WARNING] FEI5 environment variable is not set." 1>&2
        ${MDMSCONFIG_WRITE} "${SF_NAME}: Using default location ${SF_DEF_VAL}" 1>&2
        SF_FEI5=${SF_DEF_VAL}
      else
        SF_FEI5=${FEI5}
    fi
     
    # return FEI5 value
    echo ${SF_FEI5}      
}



##--------------------------------------------------------------------------


## 
# Subroutine to set and return application classpath
# @param 1 - user nput classpath to be prepended to the application
#            classpath. NOTE: HACK WARNING *** if environment var "V2HTML" 
#            is not defined then this parameter will be ignored.
#            NOTE: If $V2HTML/jars exists, those jars will be added to the
#            classpath.  Otherwise $FEI5/../lib jars will be added.
# @returns Application classpath
##

getClasspath() {
    
    GCP_NAME="getClasspath()"
    
    # get classpath parameter if available
    GCP_CP=""  #default empty
    if [ $# -eq 1 ]
     then
     if [ "Z${V2HTML}" != "Z" ] 
      then
       GCP_CP=$1
     fi
    fi
    
    # build default value for FEI5 variable
    DEF_FEI5="${PWD}/../config"
    GCP_FEI5=`getFEI5 ${DEF_FEI5}`
    if [ $? -ne 0 ]; then exit 1; fi
    
    
    # build Jar files using FEI5 XOR V2HTML (if defined)
    MDMSJARPATH="${GCP_FEI5}/../lib" #default value
    if [ "Z${V2HTML}" != "Z" ]
     then
     if [ -d "${V2HTML}/jars" ] 
      then
        MDMSJARPATH="${V2HTML}/jars"
     fi
    fi
     
    
    # test to see if Jars exist
    # echo `ls ${MDMSJARPATH}/*.jar`
    GCP_TEMP1=`echo ${MDMSJARPATH}/*.jar | sed -n 's/^\([^*]*\)$/\1/p'`
    
  
    # check if Jars were found in the directory
    if [ "${GCP_TEMP1}Z" = "Z" ]
      then
        ${MDMSCONFIG_WRITE} "${GCP_NAME}: [WARNING] No jars found in ${MDMSJARPATH}. Returning initial classpath" 1>&2
      else   
        for MDMS_JAR in `echo ${MDMSJARPATH}/*.jar`
        do
            if [ "${GCP_CP}Z" != "Z" ]
              then 
                 GCP_CP="${GCP_CP}:"   
            fi
            GCP_CP="${GCP_CP}${MDMS_JAR}"
        done
    fi
    
    # this is used by the server
    # jgroups requires that the location of the keystore used by the encrypt protocol
    # be stored in the classpath
    GCP_CP="${GCP_CP}:${GCP_FEI5}"
    
    # return classpath
    echo $GCP_CP
}


##--------------------------------------------------------------------------

## 
# Subroutine to return restart directory.  Checks if FEI5CCDIR env is
# defined, if so, value is returned.  Otherwise, default return value
# is user home directory.
# @returns Restart directory
##

getRestartDir() {
     
    GRD_NAME="getRestartDir()"
    
    # check if FEI5CCDIR defined
    if [ "Z${FEI5CCDIR}" = "Z" ]
     then
        GRD_DIR=${HOME} 
     else
        GRD_DIR=${FEI5CCDIR}  
    fi
   
    # return restart dir 
    echo ${GRD_DIR}
}

##--------------------------------------------------------------------------

## 
# Subroutine to build initial Java argument string including the
# application classpath, and shared JRE options.
# @param 1 - debug flag (true if value is "1", false otherwise)
# @param 2 - user input classpath to be prepended to the application
#            classpath (optional, but required if using param 3)
#
# @returns Initial JVM argument string
##

getJVMArgsForServer() {

    
    GJA_NAME="getJVMArgs()"
    
    # get debug flag
    if [ $# -ge 1 ]
     then 
        GJA_DEBUG=$1
     else
        GJA_DEBUG=$0
    fi
    
    # get classpath parameter if available
    if [ $# -ge 2 ]
     then
       GJA_CP=$2
     else
       GJA_CP=""
    fi
     
    
    # get FEI5 value
    DEF_FEI5="${PWD}/../config"
    GJA_FEI5=`getFEI5 ${DEF_FEI5}`
    if [ $? -ne 0 ]; then exit 1; fi

    # get application classpath
    GJA_CP=`getClasspath ${GJA_CP}`
    if [ $? -ne 0 ]; then exit 1; fi
    
    if [ "${GJA_CP}Z" = "Z" ]
     then
        GJA_CP="."
    fi
    
    # get restart directory
    GJA_RESDIR=`getRestartDir`
    if [ $? -ne 0 ]; then exit 1; fi
    
    GJA_USERAPP=`basename ${0}`
    
    
    # build the JVM arguments string
    GJA_ARGS=" -classpath ${GJA_CP}"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.config.dir=${GJA_FEI5}"
    #GJA_ARGS="${GJA_ARGS} -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl"
    #GJA_ARGS="${GJA_ARGS} -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl"
    #GJA_ARGS="${GJA_ARGS} -Dorg.xml.sax.driver=org.apache.xerces.parsers.SAXParser"
    GJA_ARGS="${GJA_ARGS} -Dmdms.user.application=${GJA_USERAPP}"
    GJA_ARGS="${GJA_ARGS} -Djava.net.preferIPv4Stack=true"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.net.ciphers=TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.net.protocol=TLSv1.2"
    
    
    # check for MacOSX (darwin) options 
    if [ "Z${OSTYPE}" = "Zdarwin" ]
    then
        GJA_ARGS="${GJA_ARGS} -Dcom.apple.backgroundOnly=true"
        GJA_ARGS="${GJA_ARGS} -Djava.awt.headless=true"        
    fi
    
    # check debug flag
    if [ ${GJA_DEBUG} -eq 1 ]
     then
          GJA_ARGS="${GJA_ARGS} -Dmdms.enable.debug"   
    fi
    
    # return JVM arguments
    echo ${GJA_ARGS}
}
##--------------------------------------------------------------------------

## 
# Subroutine to build initial Java argument string including the
# application classpath, and shared JRE options.
# @param 1 - debug flag (true if value is "1", false otherwise)
# @param 2 - user input classpath to be prepended to the application
#            classpath (optional, but required if using param 3)
# @param 3 - GUI flag, true if GUI application (true if value is "1",
#            false otherwise)  (optional)
# @returns Initial JVM argument string
##

getJVMArgs() {
    
    GJA_NAME="getJVMArgs()"
    
    # get debug flag
    if [ $# -ge 1 ]
     then 
        GJA_DEBUG=$1
     else
        GJA_DEBUG=$0
    fi
    
    # get classpath parameter if available
    if [ $# -ge 2 ]
     then
       GJA_CP=$2
     else
       GJA_CP=""
    fi
 
    # get GUI flag parameter if available
    if [ $# -ge 3 ]
     then
       GJA_GUIFLAG=$3
     else
       GJA_GUIFLAG="0"
    fi
    
    
    # get FEI5 value
    DEF_FEI5="${PWD}/../config"
    GJA_FEI5=`getFEI5 ${DEF_FEI5}`
    if [ $? -ne 0 ]; then exit 1; fi

    # get application classpath
    GJA_CP=`getClasspath ${GJA_CP}`
    if [ $? -ne 0 ]; then exit 1; fi
    
    if [ "${GJA_CP}Z" = "Z" ]
     then
        GJA_CP="."
    fi
    
    # get restart directory
    GJA_RESDIR=`getRestartDir`
    if [ $? -ne 0 ]; then exit 1; fi
    
    GJA_USERAPP=`basename ${0}`
    
    
    # build the JVM arguments string
    GJA_ARGS=" -classpath ${GJA_CP}"
    GJA_ARGS="${GJA_ARGS} -Djavax.net.ssl.trustStore=${GJA_FEI5}/mdms-fei.keystore"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.restart.dir=${GJA_RESDIR}"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.config.dir=${GJA_FEI5}"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.domain.file=${GJA_FEI5}/domain.fei"
    GJA_ARGS="${GJA_ARGS} -Dmdms.logging.config=${GJA_FEI5}/mdms.lcf"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.public.key=${GJA_FEI5}/public.der"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.query.interval=1"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.client.pulse=300"
    #GJA_ARGS="${GJA_ARGS} -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl"
    #GJA_ARGS="${GJA_ARGS} -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl"
    #GJA_ARGS="${GJA_ARGS} -Dorg.xml.sax.driver=org.apache.xerces.parsers.SAXParser"
    GJA_ARGS="${GJA_ARGS} -Dmdms.user.application=${GJA_USERAPP}"
    GJA_ARGS="${GJA_ARGS} -Djava.net.preferIPv4Stack=true"
    GJA_ARGS="${GJA_ARGS} -Dkomodo.filehandling.enable=true"   
    GJA_ARGS="${GJA_ARGS} -Dkomodo.net.ciphers=TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA" 
    GJA_ARGS="${GJA_ARGS} -Dkomodo.net.protocol=TLSv1.2"
 
    # check for MacOSX (darwin) options 
    if [ "Z${OSTYPE}" = "Zdarwin" ]
    then if [ ${GJA_GUIFLAG} != "1" ]
         then
            GJA_ARGS="${GJA_ARGS} -Dcom.apple.backgroundOnly=true"
            GJA_ARGS="${GJA_ARGS} -Djava.awt.headless=true"
         fi
    fi
    
    # check debug flag
    if [ ${GJA_DEBUG} -eq 1 ]
     then
          GJA_ARGS="${GJA_ARGS} -Dmdms.enable.debug"   
    fi
    
    # return JVM arguments
    echo ${GJA_ARGS}
}



##--------------------------------------------------------------------------
##--------------------------------------------------------------------------




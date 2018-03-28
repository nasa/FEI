#! /bin/sh 
##-xv

### ==================================================================== ###
#                                                                          # 
# FEI file filter driver.  Accepts a filename and criteria                 #
# script name and evaluates the file using that script.                    #
# Can be invoked using list files for filenames and criteria               #
# See usage for more information.                                          #
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
# Author: Nicholas Toole (Nicholas.T.Toole@jpl.nasa.gov)                  #
# Date: February 1, 2006                                                  #
# Version: 1.0                                                            #
# Dependencies: Criteria scripts with names passed in as parameter        #
# $Id: fei5guardian.sh,v 1.5 2006/10/03 02:18:18 ntt Exp $                #
#                                                                          #
### ==================================================================== ###

MY_NAME=`basename $0`

## constants
QFUNCTION_AND="AND"
QFUNCTION_OR="OR"
QTRUE="0"
QFALSE="1"

## =======================================================

## Prints script usage
printUsage()
{
    cat <<EOM 1>&2
Usage: ${MY_NAME} [ -f filename | -l fileList ] [ -c criteriaName | 
       -q queryList ] [ -o | -a ] [ -s ] [ -h ] [ feiCommandWithArgs ]

    -f filename      - Name of file to test
    -c criteriaName  - Name of criterion script to invoke 
    -l fileList      - File containing list of files to test
    -q queryList     - File containing list of criteria to use
    -a               - And flag. File is successful if it passed 
                       all criteria. (default)
    -o               - Or flag. File is successful if it passed 
                       at least one criterion. 
    -s               - Prints status of each file and criteria 
    -h               - Prints this message and exits
    feiCommand...    - Any FEI command that accepts filenames
                       as last arguments.  This will only
                       be invoked if files pass criteria.
                       
    Option constaints:
    Either one or more -f options XOR one -l option.
    Either one of more -c options XOR one -q option.
    
    Returns: If no feiCommand is supplied, then return value of 
    script is 0 is all files passed all criteria, 1 otherwise. If
    feiCommand is supplied and all files passed, exit value of fei
    command; 1 otherwise.
    

EOM
}

## =======================================================

## Prints script purpose 
printPurpose()
{
    cat <<EOM 1>&2
Purpose: Invokes criteria tests on files to determine if FEI
         operation should be invoked.  Return status 0 if all
         files satisfy all criteria; >0 for errors.

EOM
}

## =======================================================

## evaluates crieria for the file
evalCriteria()
{    
    if [ $# -ne 2 ]
    then    
        return 1
    fi    
    
    CRIT_NAME=${1}
    F_NAME=${2}   
    RESULT=${QFALSE}
    
    # check that the criteria file exists?
    if [ ! -f ${CRIT_NAME} ] 
    then
        echo "Criteria ${CRIT_NAME} cannot be found" 1>&2
        return ${RESULT}
    fi
    
    # execute criteria
    ${CRIT_NAME} ${F_NAME}
    if [ $? -eq 0 ]
    then
        RESULT=${QTRUE}
    fi
    
    # check if we shoul dprint status
    if [ ${LOG_STATUS} = ${QTRUE} ]
    then
        if [ ${RESULT} = ${QTRUE} ]
        then
            echo "Evaluating ${F_NAME} for criteria ${CRIT_NAME}...OK"
        else
            echo "Evaluating ${F_NAME} for criteria ${CRIT_NAME}...ERROR"
        fi        
    fi
        
    return ${RESULT}
}

## =======================================================
## =======================================================

# arg1 = first operand
# arg2 = second operand
# arg3 = function 
# return QTRUE or QFALSE

evalBooleanFnc()
{
    NUMARGS=$#
    
    if [ ${NUMARGS} -ne 3 ]
    then    
        return ${QFALSE}
    fi               
    
    OPERAND_1=${1}
    OPERAND_2=${2}
    FUNCTION=${3}
    RESULT=${QFALSE}
    
    if [ ${FUNCTION} = ${QFUNCTION_OR} ]
    then
        if [ ${OPERAND_1} = ${QTRUE} -o ${OPERAND_2} = ${QTRUE} ]
        then
            RESULT=${QTRUE}
        fi
    else
        if [ ${OPERAND_1} = ${QTRUE} -a ${OPERAND_2} = ${QTRUE} ]
        then
            RESULT=${QTRUE}
        fi    
    fi
    
    return $RESULT    
}

## =======================================================
## =======================================================

## handle echo for Linux...sigh...
if [ `uname -s | tr '[a-z]' '[A-Z]'` = LINUX ]
then 
    WRITE="echo -e "      
else
    WRITE="echo "
fi


## =======================================================
## =======================================================

## Arg count check
if [ $# -lt 1 ]
then 
    ${WRITE} "${MY_NAME}: *** Error - Incorrect number of arguments"
    printUsage
    exit 1
fi


## =======================================================
## =======================================================

## Declare command line params. assign defaults 
FILE_LIST_NAME=NONE
CRITERIA_LIST_NAME=NONE
FEI_ARGS=NONE
FILE_LIST=""
CRITERIA_LIST=""
CUMMULATIVE_FCN=${QFUNCTION_AND}
LOG_STATUS=${QFALSE}


## =======================================================
## =======================================================

## parse arguments

while :
do
    case "$1" in
    -f) shift; 
        if [ "${FILE_LIST_NAME}" != "NONE" ]
        then
            ${WRITE} "*** Error - Cannot use -l and -f options together.  Exiting..." 1>&2
            printUsage
            exit 1
        fi

        if [ $# -gt 0 ]
        then 
            if [ "${FILE_LIST}Z" != "Z" ]
            then
                FILE_LIST="${FILE_LIST} ${1}"
            else
                FILE_LIST="$1"
            fi
        else
            ${WRITE} "*** Error - Missing filename.  Exiting..." 1>&2
            printUsage 1>&2
            exit 1
        fi ;;

    -c) shift; 
        if [ "${CRITERIA_LIST_NAME}" != "NONE" ]
        then
            ${WRITE} "*** Error - Cannot use -q and -c options together.  Exiting..." 1>&2
            printUsage
            exit 1
        fi

        if [ $# -gt 0 ]
        then 
            if [ "${CRITERIA_LIST}Z" != "Z" ]
            then
                CRITERIA_LIST="${CRITERIA_LIST} ${1}"
            else
                CRITERIA_LIST="$1"
            fi
        else
            ${WRITE} "*** Error - Missing criteria name.  Exiting..." 1>&2
            printUsage
            exit 1
        fi ;;

     -l) shift; 
        if [ "${FILE_LIST}Z" != "Z" ]
        then
            ${WRITE} "*** Error - Cannot use -l and -f options together.  Exiting..." 1>&2
            printUsage
            exit 1
        fi

        if [ $# -gt 0 ]
        then 
            FILE_LIST_NAME="$1"
        else
            ${WRITE} "*** Error - Missing list filename.  Exiting..." 1>&2
            printUsage
            exit 1
        fi ;;

    -q) shift; 
        if [ "${CRITERIA_LIST}Z" != "Z" ]
        then
            ${WRITE} "*** Error - Cannot use -q and -c options together.  Exiting..." 1>&2
            printUsage
            exit 1
        fi

        if [ $# -gt 0 ]
        then 
            CRITERIA_LIST_NAME="$1"
        else
            ${WRITE} "*** Error - Missing criteria list.  Exiting..." 1>&2
            printUsage
            exit 1
        fi ;;

    -h) printPurpose; printUsage; exit 0 ;;
    -o) CUMMULATIVE_FCN=${QFUNCTION_OR} ;;  # enable OR function
    -s) LOG_STATUS=${QTRUE} ;;  # enable OR function
    "") break ;;
    *) ## Program arguments
       FEI_ARGS="$*"
       break ;;       
    esac
    shift
done


## =======================================================
## =======================================================

## Test to ensure minimal options were provided
if [ "${FILE_LIST}Z" = "Z"  -a  ${FILE_LIST_NAME} = "NONE" ]
then
    ${WRITE} "Error: Either -f or -l option must be specified." 1>&2
    printUsage
    exit 1  
fi

if [ "${CRITERIA_LIST}Z" = "Z"  -a  ${CRITERIA_LIST_NAME} = "NONE" ]
then
    ${WRITE} "Error: Either -c or -q option must be specified." 1>&2
    printUsage
    exit 1
fi

## =======================================================
## =======================================================

## Test filenames to ensure they exist

if [ ${FILE_LIST_NAME} != "NONE" ]
then
    if [ ! -f ${FILE_LIST_NAME} ]
    then 
        ${WRITE} "Error: File \"${FILE_LIST_NAME}\" does not exist" 1>&2
        exit 1
    fi
fi

if [ ${CRITERIA_LIST_NAME} != "NONE" ]
then
    if [ ! -f ${CRITERIA_LIST_NAME} ]
    then 
        ${WRITE} "Error: File \"${CRITERIA_LIST_NAME}\" does not exist" 1>&2
        exit 1
    fi
fi

## =======================================================
## =======================================================


## Compile list of files
if [ ${FILE_LIST_NAME} != "NONE" ]
then
    for LINE in `cat ${FILE_LIST_NAME}`
    do
        FILE_LIST="${FILE_LIST} ${LINE}"
    done
fi


## Compile list of criteria
if [ ${CRITERIA_LIST_NAME} != "NONE" ]
then
    for LINE in `cat ${CRITERIA_LIST_NAME}`
    do
        CRITERIA_LIST="${CRITERIA_LIST} ${LINE}"
    done
fi


## =======================================================
## =======================================================

## Perform criteria check for each file and for each criterion

ALL_FILES_OK=${QTRUE}
FILE_PASSED_CRIT=${QTRUE}
FILE_OK=${QTRUE}

## Initial OK - identify value based on function - AND: true, OR: false
INITIAL_OK=${QTRUE}
if [ "${CUMMULATIVE_FCN}" = "${QFUNCTION_OR}" ]
then
    INITIAL_OK=${QFALSE}
fi

#echo "FILELIST     = ${FILE_LIST}"
#echo "CRITERIALIST = ${CRITERIA_LIST}"

for FILENAME in $FILE_LIST
do
    ## init file ok to false
    FILE_OK=${INITIAL_OK}
    
    for CRITERIA in $CRITERIA_LIST
    do          
        ## evaulate the criteria for current filename
        evalCriteria ${CRITERIA} ${FILENAME}
        FILE_PASSED_CRIT=$?        
        
        ## is file still ok?  ask function
        evalBooleanFnc ${FILE_OK} ${FILE_PASSED_CRIT} ${CUMMULATIVE_FCN}
        FILE_OK=$?
        
    done
 
    ## check if we should print status
    if [ ${LOG_STATUS} = ${QTRUE} ]
    then
        if [ ${FILE_OK} = ${QTRUE} ]
        then
            echo "File ${FILENAME}: PASSED"
        else
            echo "File ${FILENAME}: FAILED"
        fi        
        echo ""
    fi
    
    
    ## are all files so far ok?  Update flag 
    evalBooleanFnc ${ALL_FILES_OK} ${FILE_OK} ${QFUNCTION_AND}
    ALL_FILES_OK=$?
    
done

## check if we should print status
if [ ${LOG_STATUS} = ${QTRUE} ]
then
    if [ ${ALL_FILES_OK} = ${QTRUE} ]
    then
        echo "All files: PASSED"
    else
        echo "Some file(s): FAILED"
    fi        
fi


## =======================================================
## =======================================================

## If no args, then return success, 0, or failure, >0.
## If args and success so far, attempt to invoke FEI command
## passing it the files associated with FILE_LIST.

## we won't being invoking anything, so return our status
if [ "${FEI_ARGS}" = "NONE" -o  ${ALL_FILES_OK} = ${QFALSE} ]
then
    exit ${ALL_FILES_OK}
fi

## Invocation begin!!!
if [ ${LOG_STATUS} = ${QTRUE} ]
then
    echo "Invoking: ${FEI_ARGS} ${FILE_LIST}"                
fi

${FEI_ARGS} ${FILE_LIST}
exit $?

    

## =======================================================
## =======================================================

# exit


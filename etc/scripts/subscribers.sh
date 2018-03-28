#! /bin/sh
# xv

#
### ==================================================================== ###
#                                                                          #
#  The File Exchange Interface (FEI) Configuration Functions               #
#                                                                          #
#  Function:                                                               #
#  Launchers series of subscription sessions for use with testing          #
#  and benchmarking server.                                                # 
#                                                                          #
#  Usage: subscribers.sh -n <num> -o <root> -a '<args>' [ -l ]             #
#         <num> : number of subscription sessions to run                   #
#         <root>: output root.  Each session will have its own directory   #
#                 <root>/session_<id>, where id is the unique value from   #
#                 1 to <num> assigned to session.                          #
#         <args>: subscription arguments.  Will be supplied to each        # 
#                 session.  Must be enclosed in single quotes.             #
#         -l    : Enables log redirection per session (client.log)         #  
#                                                                          #
#  Example: $ subscribers.sh -n 3 -o /pkg/fei/test/ -a 'dev:type replace'  #
#           Will spawn three subscription sessions using the arguments     #    
#           appended with 'output /pkg/fei/test/session_<id>' where        # 
#           <id>: {1,2,3}.                                                 #
#                                                                          #
#  Copyright 2005, California Institute of Technology                      #
#  ALL RIGHTS RESERVED                                                     #
#  U.S. Government Sponsorship acknowledged 08/02/2005                     #
#  Mission Data Management Service (MDMS)                                  #
#                                                                          #
#  Created:                                                                #
#  Aug. 02, 2005 by Nicholas Toole {nicholas.toole@jpl.nasa.gov}           #
#                                                                          #
#                                                                          #
### ==================================================================== ###
#
# $Id: subscribers.sh,v 1.1 2005/08/19 23:52:37 ntt Exp $
#

# Output variables
WRITE="echo "
MY_NAME=`basename $0`

# Maintains a list of child process ids
PROC_LIST=""

# Number of clients to launch
NUM_CLIENTS=1

# Root for output paths
OUT_ROOT="."

# Contains subscription arguments
SUB_ARGS=""

#log flags
LOG_FLAG="0"
LOG_FILE="client.log"


##--------------------------------------------------------------------------

# Test which version of echo to use based on OS name

if [ `uname -s | tr '[a-z]' '[A-Z]'` = LINUX ]
    then 
        WRITE="echo -e "      
    else
        WRITE="echo "
fi

##--------------------------------------------------------------------------

# Subroutine that creates new directory and launches session
## ARG1 Unique id associated with subscription session
## Returns child process id, -1 if error

runSubscribe()
{
    CPID="-1"

    # arg check
    if [ $# -ne 1 ]
      then
      ${WRITE} "${MY_NAME}: [ERROR]: Subroutine expects one argument" 1>&2 
      echo ${CPID}
      exit 1
    fi    
    ARG1=$1
   
    #create session directory
    OUT_DIR=${OUT_ROOT}/session_${ARG1}
    if [ ! -d $OUT_DIR ]
      then
        mkdir ${OUT_DIR}
    fi
    
    #spawn new process to run subscription
    if [ "${LOG_FLAG}" = "1" ]
      then
      fei5subscribe ${SUB_ARGS} output ${OUT_DIR} > ${OUT_DIR}/${LOG_FILE} 2>&1 &
      else
        fei5subscribe ${SUB_ARGS} output ${OUT_DIR} 1>&2 &
    fi
    
    #get the process id
    CPID=$!
    
    #return that bad boy to caller
    echo $CPID    
}


##--------------------------------------------------------------------------

# Function: getChildPids(ppid)
# Purpose: Returns a list of processes with parent id's equal to that 
# of the argument
# ARG1 - Parent process id
# Returns: possibly empty list of children pids

getChildPids()
{
    CLIST=""
    
    # arg check
    if [ $# -ne 1 ]
      then
      ${WRITE} "${MY_NAME}: [ERROR]: Subroutine expects one argument" 1>&2 
      echo ${CLIST}
      exit 1
    fi    
    ARG1=$1
    
    PARENT_ID=${ARG1}
    
    CLIST=`ps -o uid,pid,ppid | while read U_ID P_ID PP_ID REST; do
        if [ "${PARENT_ID}Z" = "${PP_ID}Z" ]
          then
            echo $P_ID
        fi    
    done`
     
    echo "${CLIST}"    
}


##--------------------------------------------------------------------------

# Function: cleanup()
# Purpose: Kills all spawned processes and their children.

cleanup()
{
    # get script name if not already set
    ${WRITE} ""
    if [ "${MY_NAME}Z" = "Z" ] 
      then 
        MY_NAME=`basename $0`;
    fi

    # print message
    ${WRITE} "${MY_NAME}: Received termination signal. Cleaning up..." 1>&2 ;


    # kill processes from list
    if [ "${PROC_LIST}Z" != "Z" ]
      then             
        for P_PID in ${PROC_LIST}; do
          CPIDS=`getChildPids ${P_PID}`
          if [ "${CPIDS}Z" != "Z" ]
            then
              for TOKILL in ${CPIDS}; do
                 kill $TOKILL > /dev/null 2>&1             
              done
          fi      
        done
        kill ${PROC_LIST} > /dev/null 2>&1   
    fi 
    
    ${WRITE} "${MY_NAME}: Done" 1>&2 ;
    
    exit 1  
        
}

##--------------------------------------------------------------------------

# Function: printUsage()
# Purpose: Prints usage to stderr 

printUsage() 
{
    $WRITE "" 1>&2
    $WRITE "Usage: ${MY_NAME} -n <num_clients> -o <out_root> -a '<args>' [ -l | -h ]" 1>&2
    $WRITE "" 1>&2
    $WRITE "       -n <num_clients>  - Number of clients to launch" 1>&2
    $WRITE "       -o <out_root>     - Output root." 1>&2
    $WRITE "       -a <args>         - Subscription session arguments (enclosed in quotes)" 1>&2
    $WRITE "       -l                - Enables log redirection for each session" 1>&2
    $WRITE "       -h                - Prints this message and exits" 1>&2
    $WRITE "" 1>&2    
}

##--------------------------------------------------------------------------

# Function: printPurpose()
# Purpose: Prints purpose to stderr.

printPurpose()
{
    $WRITE ""  1>&2 
    $WRITE "Purpose: Script launcher which invokes a set number of FEI5"  1>&2 
    $WRITE "         subscription sessions."  1>&2
}

##--------------------------------------------------------------------------
##--------------------------------------------------------------------------

# Set trap which performs cleanup when script is terminated
trap ' cleanup ' 1 2 3 15

##--------------------------------------------------------------------------

# Check for FEI5 environment variable

if [ "${FEI5}Z" = "Z" ]
  then
     ${WRITE} ${MY_NAME}: [ERROR] FEI5 environment variable is not set. 1>&2
     exit 1
fi

##--------------------------------------------------------------------------

# Process input

## Arg count check
if [ $# -lt 1 ]
  then 
    ${WRITE} "${MY_NAME}: Incorrect number of arguments"
    printUsage
    exit 1
fi


while :
do
    case "$1" in
    -n) shift; 
        if [ $# -gt 0 ]        
          then 
            NUM_CLIENTS="$1"
          else
            ${WRITE} "Warning - Missing client count, assuming 1." 1>&2
            NUM_CLIENT="1"
        fi ;;
    -o) shift; 
        if [ $# -gt 0 ]
          then 
            OUT_ROOT="$1"
          else
            ${WRITE} "Warning - Missing output root.  Assuming current dir" 1>&2
            OUT_ROOT="."
        fi ;;
    -a) shift; 
        if [ $# -gt 0 ]
          then 
            SUB_ARGS="$1"
          else
            ${WRITE} "${MY_NAME}: [Error] - Missing subscription arguments." 1>&2
            printUsage
            exit 1 
        fi ;;
    -h) printPurpose; printUsage; exit 0 ;;
    -l) LOG_FLAG="1"; ;;
    "") break;;
    *)  ${WRITE} "${MY_NAME}: [Error] - Unrecognized argument: " ${1} 1>&2
        printUsage
        exit 1 ;;
    esac
    shift
done



#echo "Num clients to run = " $NUM_CLIENTS
#echo "Output directory root = " $OUT_ROOT
#echo "Command arguments = " $SUB_ARGS

##--------------------------------------------------------------------------

# Spawn $NUM_CLIENTS subscription sessions

INDEX=1
while [ ${INDEX} -le ${NUM_CLIENTS} ]; do

    # run new subscription session, grab the child proc id
    CPID=`runSubscribe ${INDEX}`
    if [ ${CPID} -ne -1 ]
      then
        ${WRITE} "${MY_NAME}: Started session with id = ${INDEX} (pid=${CPID})..."  
        PROC_LIST="${PROC_LIST} ${CPID}"
      else
        ${WRITE} "${MY_NAME}: [ERROR] Could not start session ${INDEX}." 
    fi
    INDEX=`expr $INDEX + 1`
done



##--------------------------------------------------------------------------

# Busy wait so that cleanup can be called by trapping signals

while [ 1 -eq 1 ]
do
    sleep 2
done

##--------------------------------------------------------------------------

# Should never get here, but you know...stuff happens
exit 1

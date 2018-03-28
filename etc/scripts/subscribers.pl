#!/usr/bin/perl -w
# -*-Perl-*-

#
### ==================================================================== ###
#                                                                          #
#  The File Exchange Interface (FEI) Testing Functions                     #
#                                                                          #
#  Function:                                                               #
#  Launchers series of subscription sessions for use with testing          #
#  and benchmarking server.                                                # 
#                                                                          #
#  Usage: subscribers -n <num> -o <root> -a '<args>' [ -l ]                #
#         <num> : number of subscription sessions to run                   #
#         <root>: output root.  Each session will have its own directory   #
#                 <root>/session_<id>, where id is the unique value from   #
#                 1 to <num> assigned to session.                          #
#         <args>: subscription arguments.  Will be supplied to each        # 
#                 session.  Must be enclosed in single quotes.             #
#         -l    : Enables log redirection per session (client.log)         #  
#                                                                          #
#  Example: $ subscribers.pl -n 3 -o /pkg/fei/test/ -a 'dev:type replace'  #
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
# $Id: subscribers.pl,v 1.1 2005/08/19 23:52:37 ntt Exp $
#

use strict;
use English;
use File::Basename;
use Getopt::Std;
use File::Spec;

# the global default JRE version
use constant DEFAULT_NUM_CLEINTS => 1;
use constant DEFAULT_OUTPUT_ROOT => ".";


# Output variables
my $my_name = basename($PROGRAM_NAME);

# Maintains a list of child process ids
my @process_list = ();

# Number of clients to launch
my $num_clients = 1;

# Root for output paths
my $output_root = ".";

# Contains subscription arguments
my $subscription_args = "";

#log flags
my $log_flag = 0;
my $log_file = "client.log";


##--------------------------------------------------------------------------

## Function void writeMsg(args);
## Subprocedure that prints arguments to standard error.

sub writeMsg {    
    
    #if no arguments, then just print a newline
    if ($#_ == -1)
    {
        print STDERR "\n";   
    } 
    #else print each index 
    else 
    {
        my $i;
        for ($i = 0; $i <= $#_; $i++) {
            print STDERR "$_[$i]";
        }
    }
}

##--------------------------------------------------------------------------

## Function: int runSubscribe(int session_id)
## Subroutine that creates new directory and launches session
## ARG1 Unique id associated with subscription session
## Returns child process id, -1 if error

sub runSubscribe {
    
    my $cpid = -1;
    my ($session_index) = @_;

    my $session_dir = "session_" . ${session_index};
    $session_dir = File::Spec->catdir(${output_root}, ${session_dir});
    unless (-d ${session_dir}) {
        mkdir(${session_dir}, 0755) || die "Could not create ${session_dir}."   
    }
    
    my $subCmd = "fei5subscribe ${subscription_args} output ${session_dir} ";
    if ( ${log_flag} == 1) {
        my $logFilePath = File::Spec->catdir(${session_dir}, ${log_file});
        $subCmd = $subCmd . " > ${logFilePath}";
    } else {
        $subCmd = $subCmd . " 1>&2";
    }
    
    my $cPid = fork;
    if ($cPid) {
        # i am the parent
    } else {
        # i am the child, let me redefine what I do
        exec(" ${subCmd} ");  
        exit(0);
    }
    
    #return process id
    return $cPid;
}


##--------------------------------------------------------------------------

## Function: int[] getChildPids(int ppid)
## Purpose: Returns a list of processes with parent id's equal to that 
## of the argument
## ARG1 - Parent process id
## Returns: possibly empty list of children pids

sub getChildPids {
    my ($parent_id) = @_;
    my @child_pids = ();
    my $line; my $blank; my $uid; my $pid; my $ppid; my $rest;
    foreach $line (`ps -o uid,pid,ppid`) {
        ($blank, $uid, $pid, $ppid, $rest) = split(/\s+/, $line);
        if ( $ppid = $parent_id ) {
            push(@child_pids, $pid);   
        }
    }
    return @child_pids;
}

##--------------------------------------------------------------------------

## Function: void killFamily(int ppid)
## Purpose: Kills the family of processes using the first argument
## as the root of the process tree.
## ARG1 - Parent process id

sub killFamily {
    my ($proc_id) = @_;
    
    # collect descendant pids
    my @familyOfIds;    
    @{familyOfIds} = &getDescendantPids(${proc_id});
    
    # add cur id to end of list, then reverse it
    push(@{familyOfIds}, ${proc_id});
    @{familyOfIds} = reverse(@familyOfIds);
    
    #call kill starting from leaves upward
    #writeMsg("Im gonna kill the following: @familyOfIds \n");
    system("kill @{familyOfIds} 2>&1");
}

##--------------------------------------------------------------------------

## Function: int[] getDescendantPids(int ppid)
## Purpose: Recursively collections all child and lower
## pids for a given process id.  Base case if a process
## with no children.
## ARG1 - Parent process id
## Returns: possibly empty list of family pids

sub getDescendantPids {
    my ($parent_id) = @_;    
    my @child_pids = ();
    my @family_ids = ();
    my $line; my $blank; my $uid; my $pid; my $ppid; my $rest;
    
    # collect child with ppids matching argument
    foreach $line (`ps -o uid,pid,ppid`) {
        ($blank, $uid, $pid, $ppid, $rest) = split(/\s+/, $line);
        if ( $ppid ne "PPID" && $ppid == $parent_id ) {
            push(@child_pids, $pid);   
        }
    }
    
    # add children to list
    push(@{family_ids}, @{child_pids});
    
    # now recursively call on each child to collect its family
    my @tempList = ();
    foreach $pid (@child_pids) {
        @tempList = &getDescendantPids($pid);
        push(@family_ids, @tempList);
    }
    
    return @family_ids;
}


##--------------------------------------------------------------------------

## Function: cleanup()
## Purpose: Kills all spawned processes and their descendants.

sub cleanup {
    
    &writeMsg("\n");
    
    # get script name if not already set
    if (! defined($my_name)) {
        $my_name = basename($PROGRAM_NAME);
    }   

    # print message
    &writeMsg("${my_name}: Received termination signal. Cleaning up...\n");
    
    
    my $proc_id;
    foreach ${proc_id} (@process_list) {
        &killFamily(${proc_id});
    }   
    
                
    &writeMsg("${my_name}: Done\n");    
    exit(1);          
}


##--------------------------------------------------------------------------

# Function: printUsage()
# Purpose: Prints usage to stderr 

sub printUsage {
    my @usage = ("Usage: ${my_name} -n <num_clients> -o <out_root> -a '<args>' [ -l | -h ]\n",
                 "       -n <num_clients>  - Number of clients to launch\n",
                 "       -o <out_root>     - Output root.\n",
                 "       -a <args>         - Subscription session arguments (enclosed in quotes)\n",
                 "       -l                - Enables log redirection for each session\n",
                 "       -h                - Prints this message and exits\n",
                 "\n");
    &writeMsg(@usage); 
    return(0);
}

##--------------------------------------------------------------------------

# Function: printPurpose()
# Purpose: Prints purpose to stderr.

sub printPurpose {
    my @purpose = ("Purpose: Script launcher which invokes a set number of FEI5\n",
                   "         subscription sessions.\n");
    &writeMsg(@purpose); 
    return(0);
}

##--------------------------------------------------------------------------
##--------------------------------------------------------------------------

# Set trap which performs cleanup when script is terminated
$SIG{QUIT} = \&cleanup;
$SIG{INT}  = \&cleanup;
$SIG{KILL} = \&cleanup;
$SIG{TERM} = \&cleanup;

##--------------------------------------------------------------------------

# Check for FEI5 environment variable
my $fei5 = $ENV{FEI5} ? $ENV{FEI5} : "";
if ( $fei5 eq "" ) {
    die("FEI5 environment variable is not set.\n");   
}

##--------------------------------------------------------------------------

# Process input

## Arg count check
if (($#ARGV + 1) < 2) {
    print "${my_name}: Incorrect number of arguments.";
    &printUsage;
    exit(1);
}

## Parse command line options
my %options = ();
getopts("n:o:a:hl", \%options);
if ($options{n}) { $num_clients = $options{n}; } else { $num_clients = DEFAULT_NUM_CLEINTS; }
if ($options{o}) { $output_root = $options{o}; } else { $output_root = DEFAULT_OUTPUT_ROOT; }
if ($options{a}) { 
    $subscription_args = $options{a}; 
} else { 
    $subscription_args = 1;
    &writeMsg("${my_name}: [Error] - Missing subscription arguments.");
    &printUsage;
    exit(1);
}
if ($options{l}) { $log_flag = 1; } else { $log_flag = 0; }
if ($options{h}) { 
    &printPurpose;
    &printUsage;
    exit(0);
}


#&writeMsg("Num clients to run = ${num_clients} \n");
#&writeMsg("Output directory root = ${output_root} \n");
#&writeMsg("Command arguments = ${subscription_args} \n");

##--------------------------------------------------------------------------

# Print this scripts PID
&writeMsg();
&writeMsg("${my_name}: Spawning ${num_clients} session(s).  (to exit: kill ${PROCESS_ID})\n");

# Spawn $NUM_CLIENTS subscription sessions

my $cpid;
for (my $i = 1; $i <= ${num_clients}; $i++) {
    ${cpid} = &runSubscribe(${i});
    if (${cpid} > 0) {
        &writeMsg("${my_name}: Started session with id = ${i} (pid = ${cpid})\n"); 
        push(@process_list, $cpid);
    } else {
        &writeMsg("${my_name}: [ERROR] Could not start sessuib ${i}\n");
    }    
}



##--------------------------------------------------------------------------

# Busy wait so that cleanup can be called by trapping signals

while ( 1 == 1 )
{
    sleep 1;
}

##--------------------------------------------------------------------------

# Should never get here, but you know...stuff happens


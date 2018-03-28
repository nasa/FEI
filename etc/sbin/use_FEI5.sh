#!/bin/sh
#
### ==================================================================== ###
#                                                                          #
#  The File Exchange Interface (FEI) Environment Setup Script              #
#                                                                          #
#  Function:                                                               #
#  Simple shell script to add FEI5 launchers to clients path.              # 
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
#  Sept. 24, 2004 by Rich Pavlovsky {rich.pavlovsky@jpl.nasa.gov}          #
#                                                                          #
#  Modifications:                                                          #
#                                                                          #
### ==================================================================== ###
#
# $Id: use_FEI5.sh,v 1.4 2006/10/03 02:18:19 ntt Exp $
#

export FEI5=${PWD}/config
export PATH=${FEI5}/../bin:${PATH}
export JAVA_NAMING_HOST=mdms.jpl.nasa.gov

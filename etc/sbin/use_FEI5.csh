#!/bin/csh -f
#
### ==================================================================== ###
#                                                                          #
#  The File Exchange Interface (FEI) Environment Setup Script              #
#                                                                          #
#  Function:                                                               #
#  Simple c-shell script to add FEI5 launchers to clients path.            #
#                                                                          #
#  Copyright (c) 2006 by the California Institute of Technology.           #
#  ALL RIGHTS RESERVED.  United States Government Sponsorship              #
#  acknowledged. Any commercial use must be negotiated with the            #
#  Office of Technology at the California Institute of Technology.         #
#                                                                          #
#  Installation under terms of the software license.  The Department       #
#  of Commerce has classified the FEI Client 5 Software as EAR 99,         #
#  which means that the software may be distributed to any country         #
#  except the Terrorist 6.  The Terrorist 6 Countries include North        #
#  Korea, Cuba, Iran, Syria, Sudan and Libya.                              #  
#                                                                          #
#                                                                          #
#  Created:                                                                #
#  Oct. 07, 2003 by Rich Pavlovsky {rich.pavlovsky@jpl.nasa.gov}           #
#                                                                          #
#  Modifications:                                                          #
#  Oct. 08, 2003 by Rich Pavlovsky {rich.pavlovsky@jpl.nasa.gov}           #
#  Changed etc directory to config so it'll match users guide.             #
### ==================================================================== ###
#
# $Id: use_FEI5.csh,v 1.5 2006/07/28 01:33:22 ntt Exp $
#

setenv FEI5 ${cwd}/config
#setenv KRB5_CONFIG ${FEI5}/krb5.conf
#setenv PWDSERVER ${FEI5}
setenv PATH ${FEI5}/../bin:${PATH}
setenv JAVA_NAMING_HOST mdms.jpl.nasa.gov

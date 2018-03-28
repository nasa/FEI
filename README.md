# fei5_ant_client
Client Only Repository

### Keeping only the minimal set of jars for client. 
1. mdms.jar
2. mdms-komodo-client.jar
3. mdms-komodo-lib.jar
4. mdms-fei5.zip
5. mdms-fei5.tar.gz

#### Due to shared libraries between server and client in current setup, some classes are omitted from the libraries. 

**mdms.jar**
1. `jpl/mipl/mdms/utils/TFAAuthUser.class`

**mdms-komodo-client.jar**
*none*

**mdms-komodo-lib.jar**
1. `jpl/mipl/mdms/FileService/komodo/services/*`
2. `jpl/mipl/mdms/FileService/komodo/util/Pair.class`
3. `jpl/mipl/mdms/FileService/sigevents/*`

*NOTE:* the comparison are made with `MIPL`-built libraries, not with the `ANT`-built libraries. 


### Issues & TODO
1. `javadoc` generation has multiple warnings & errors. 
2. `checkstyle` task has several warnings
3. `javancss` task has multiple errors
4. `emma`'s `test-run` task is failed. 
5. To remove unused variables in `build.xml` and associated files
6. The source code is calling several `deprecated` methods. 



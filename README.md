# FEI5 Client Repository
Visit FEI5 at [FEI Overview](https://www-mipl.jpl.nasa.gov/mdms/Fei/feiOverview.html)
## About
The File Exchange Interface (FEI5) service offers secure file transaction, store, transport, and management services. FEI5 is the science data product management and distribution service used by most major space missions. The service offers a transaction-oriented approach in file management. That is, all concurrent updates to the same data product are prohibited. All uncommitted file transactions are automatically rolled back. The latest distribution, FEI5 software code name Komodo, is a complete redesign from its predecessors, which adopts the latest computing technologies and standards.
## License
Copyright © 2002-2018 United States Government as represented by the Administrator of the National Aeronautics and Space Administration. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
## Installations
### Prerequisites 
* Java-1.7 or later
* [Apache Ant](https://ant.apache.org/)
## Instructions
1. Clone this repository to a new directory. 
2. build FEI5 Client by running `ant clean package`
   * alternatively, `ant clean all` can be used to generate `javadoc`, and `checkstyle` tasks. 
   * `java` class files are saved in `build/classes/`
  
3. 3 jar files and 2 distribution packages are created in `dist/`
   * mdms.jar
   * mdms-komodo-client.jar
   * mdms-komodo-lib.jar
   * mdms-fei5.zip
   * mdms-fei5.tar.gz
4. Use distribution packages to create an FEI5-Client instance. The instruction is included inside the distribution packages. 





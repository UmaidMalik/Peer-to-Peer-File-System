How to run:
REQUIRES: JDK 15 or later

Method 1:

If you have IntelliJ IDE installed, you can open the project
from the directory 'P2PFS' and then run it.

 

Method 2:
You need search for your for where your jdk is located.
It should be in program files -> find Java directory -> find 'jdk-XX.0.1' Note use version 15 or later 
Copy for example, 'jdk-15.0.1'; in the 'P2PFS' project directory

The current Server.bat and Client.bat are currently configured for 'jdk-15.0.1'
so if you have a later JDK, just edit the batch files for version.
For example, if you use JDK 17, then "..\P2PFS\jdk-15.0.1\bin\java.exe" should
look like this "..\P2PFS\jdk-17.0.1\java.exe"




Configure batch file with:
@echo off
..\P2PFS\jdk-15.0.1\bin\java.exe -jar Example.jar

So like this....

for 'Client.bat'
@echo Running Client.jar... 
@echo off
..\P2PFS\jdk-15.0.1\bin\java.exe -jar Client.jar 
or
@echo Running Client.jar... 
@echo off
..\P2PFS\jdk-15.0.1\bin\java.exe -jar Client.jar <name> <ip> <udp> <tcp>

for 'Server.bat'
@echo Running Server.jar... 
@echo off
..\P2PFS\jdk-15.0.1\bin\java.exe -jar Server.jar



Method 3:
To run .jar files with JDK 15 or later already installed on you PC,
Edit batch (.bat) file to:
java -jar Example.jar

like this...

for 'Client.bat'
java -jar Client.jar
or
java -jar Client.jar <name> <ip> <udp> <tcp>

for 'Server.bat'
java -jar Server.jar

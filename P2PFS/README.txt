How to run:

Method 1:

If you have JDK 15 or later and IntelliJ installed, you can open the project
from the directory 'P2PFS' and then run it.

 

Method 2:
You need JDK 15 (jdk-15.0.1) in the 'P2PFS' directory

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




Note: To run .jar files with JDK 15 or later already installed on you PC,
Edit batch (.bat) file to:
java -jar Example.jar

like this...

for 'Client.bat'
java -jar Client.jar
or
java -jar Client.jar <name> <ip> <udp> <tcp>

for 'Server.bat'
java -jar Server.jar

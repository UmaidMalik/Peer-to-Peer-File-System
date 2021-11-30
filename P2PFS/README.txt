How to run:

Method 1:

If you have JDK 15 or later and IntelliJ installed, you can open the project
from the directory "P2PFS" and then run it.

 

Method 2:

Configure batch file with:
@echo off
fullpath\jdk-15.0.1\bin\java.exe -jar Example.jar

@echo Running Client.jar... 
@echo off
C:\Users\u_mal\Desktop\P2PFS\jdk-15.0.1\bin\java.exe -jar Client.jar 

@echo Running Client.jar... 
@echo off
C:\Users\u_mal\Desktop\P2PFS\jdk-15.0.1\bin\java.exe -jar Client.jar <name> <ip> <udp> <tcp>

@echo Running Server.jar... 
@echo off
C:\Users\u_mal\Desktop\P2PFS\jdk-15.0.1\bin\java.exe -jar Server.jar




Note: To run .jar files with JDK 15 already installed,
Edit batch (.bat) file to:
java -jar Example.jar
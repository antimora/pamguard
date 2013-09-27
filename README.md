About
=====

This github repository is a fork of the official codebase hosted on SourgeForge. The URL to the snapshotted version is [here](http://svn.code.sf.net/p/pamguard/svn/PamguardJava/trunk/beta/?r=1557). 


The [PAMGUARD](http://www.pamguard.org/) project develops software to help detect, locate and classify marine mammals using Passive Acoustic Monitoring.


Installation
============
The installation procedure that you need to follow depends on the distribution you have downloaded. However, you will definitely need: A Java Virtual Machine (JVM). For the stable and Beta versions of PAMGUARD you will need to be using version 1.6.0 or higher of Java, downloadable at http://www.oracle.com/technetwork/java/javase/downloads/index.html.

In order to run the binaries, you need to install only the Java Runtime Environment (JRE). If you are going to compile source files as well, you may prefer to install the Java Development Kit (JDK). Installing any version of JDK also installs the corresponding version of JRE. N.B. For this 1.6 release of Pamguard anyone using a 64 bit version of Windows will still only get full functionality if there is a 32 bit version of  Java installed on that 64 bit machine. The *.exe launcher files will check when starting PAMGUARD as to whether a suitable 32 bit JVM is installed and if not then direct the user to the Java website so one can be downloaded.


Starting PAMGUARD
=================
On Windows, use one of the installers and run the executables via the shortcuts in the Windows Start menu.

N.B. Under Vista you may want to set the preferences for the launchers (and/or the shortcuts to them) to state that they should always be run under Administrator privileges. These are required if you want PAMGUARD to be able to perform actions such as using the GPS time to set the system clock. If using other operating systems such as Linux or Mac OS X you should be able to launch PAMGUARD via an appropriate command line.

For the stable version of PAMGUARD the command line to invoke the jarfile would want to be something like:

    java -Xms384m -Xmx1024m -Djava.library.path=lib -jar PamguardCore_1_6_00.jar

The `-Xms384m -Xmx1024m` specify the initial and maximum heap size for the JVM being used to run Pamguard i.e. how much memory it gets to use. The default max size usually being too low.

The `-Djava.library.path=lib` tells the JVM that it should look in the folder called `lib` for the required shared libraries.

On Windows the `Pamguard32.exe` pretty much just executes the above command line.

For the beta version of PAMGUARD the command line would be along these lines:

    java -Xms384m -Xmx1024m -Djava.library.path=lib -jar PamguardBeta_1_9_00.jar

Again on Windows the main function of `PamguardBeta32.exe` is to execute the above command line for you.

For "Mixed" and "Viewer" modes just add a `-m` or `-v` to the list of java arguments. On Windows just run the appropriately named Launch4j launcher (i.e. `PamguardBeta32_MixedMode.exe` or `PamguardBeta32_ViewerMode.exe`).

We may occasionally only release a new jar file. In this case if you're on Windows you should:

* place a copy the new jar file in your `Program Files/Pamguard` or `Program Files/PamguardBeta` folder.

* if it has a different name to the original jar file use a text editor to change the `l4j.ini` files within that same folder so that the `-jar` lines use the new jarfile's name.

* the `Launch4j *.exe` launchers should then launch the new version



License 
=======
PAMGUARD is free software, and you are welcome to redistribute and modify it under the terms of the GNU General Public License (either version 2 or any later version).

UPDATE: The build is broken because of JVM version upgrade. See issue [#5](https://github.com/antimora/pamguard/issues/5).


PAMGUARD
========

[![Build Status](https://travis-ci.org/antimora/pamguard.png?branch=develop)](https://travis-ci.org/antimora/pamguard)

About
-----

This github repository is a fork of the official codebase hosted on SourgeForge. The URL to the snapshotted version is [here](http://svn.code.sf.net/p/pamguard/svn/PamguardJava/trunk/beta/?r=1557).

The [PAMGUARD](http://www.pamguard.org/) project develops software to help detect, locate and classify marine mammals using Passive Acoustic Monitoring.

Getting Started
---------------

This project uses [Maven](http://maven.apache.org/) build system. If you are not familiar with Maven, there is a [5 minute tutorial](http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) about this tool.

Most modern Java IDEs have an excellent support for Maven. [Netbeans](http://wiki.netbeans.org/MavenBestPractices) and [IntelliJ IDEA](http://www.jetbrains.com/idea/webhelp/maven-2.html) already come with built-in facilities. And Ecplise there is a [maven plugin M2E](http://www.eclipse.org/m2e/).

Build Project
-------------

From the directory where the source code, run the following:

```
mvn clean install
```

License
-------
PAMGUARD is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Hotel Management System
=======================
Application to make life easier for people running hotels... or maybe my bachelor thesis... I don't know which one but certainly one of those.

Installation
=============

1. Get my variation of Ledge framework and install it
2. Get this project and install it

In console:
 1. `git clone git://github.com/mklew/ledge.git`
 2. `cd ledge/ledge-common`
 3. Make sure that you are on `mklew-dev` branch. Look at `git branch`
 4. `mvn clean install`
 5. `cd ~`
 6. clone this repository
 7. install it via maven `mvn clean install`
 8. go to `hotel-inhouse-web`
 9. run `mvn jetty:run`

 In case ObjectLedge tests fail, skip them. Sometimes there's something wrong with timeouts or something else completely not related to my work so just skip them.

Developers
----------
Marek Lewandowski
# Java ONVIF (Open Network Video Interface Forum)

ONVIF is a community to standardize communication between IP-based security products (like cameras).

This project aims to improve [https://github.com/milg0/onvif-java-lib](https://github.com/milg0/onvif-java-lib).  

I've tried to convice its author to use to my code but it seems we have different objectives: my goal is to create a project that focus on the funny part of the development of an ONVIF application, **keeping the interaction with the WS as simple as possible** and delege that annoying part to Apache CXF in order to not waste the developer time in writing (and MAINTAINING) code that interacts with ONVIF web services.  

My wish is to help other developers willing to contribute to an enterprise-level Java library for ONVIF devices.

# Apported improvements

- Project **mavenization** and **modularization** (separation between Java stubs and application) and 
- WS tests generation using Apache CXF maven plugin (declaring the specific Onvif specification of each wsdl)
- maintainability and extendability of the overall code
- Separation of Test/examples from other code

# Rebuilding WS stubs

If you need to change the list of managed WSDLs (in onvif/onvif-ws-tests/src/main/resources/wsdl) and thus you need to regenerate the WS Java stubs using the [Apache CXF codegen maven plugin](http://cxf.apache.org/docs/maven-cxf-codegen-plugin-wsdl-to-java.html), you need to go through the following steps:

1. **Download Onvif WSDLs** to onvif/onvif-ws-tests/src/main/resources/wsdl appending the version before the .wsdl suffix.
 For example, from main dir (onvif) use you can run the following shell commmand:<br>

`wget http://www.onvif.org/onvif/ver10/device/wsdl/devicemgmt.wsdl onvif-ws-tests/src/main/resources/wsdl/devicemgmt_2.5.wsdl` 

1. **Update WSDLLocations constants (if needed)** within class  *de.onvif.utils.WSDLLocations* (module onvif-java)
2. **Add required url-rewriting rules (if needed)** to onvif/onvif-ws-tests/src/main/resources/wsdl/jax-ws-catalog.xml
3. Delete old Java classes in onvif/onvif-ws-tests/src/main/java
4. **Run the class generation command**: decomment goal and phase of cxf-codegen-plugin in onvif-ws-tests pom.xml and run ```mvn clean install```
5. To see how to properly add a new ONVIF service to OnvifDevice look into OnvifDevice.init()

# TODO

My next goals are:

1. Create an active community of enthusiastic developers (the crazier you are, the better)
2. Write a more comprehensive examples (e.g. subscribe to an event notification, use I/O ports, etc...)
3. Create consistent Onvif specifications tags (at least for onvif-ws-tests). For example: 2.4, 2.5, etc...
4. Fix WS-Discovery example (with my camera it doesn't work at all)
5. Write a simple UI to test the device functionalities
6. Fix offline mode (xml files in *local* folder)
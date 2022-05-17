 * Copyright (C)2006 - 2020 Enterprise Architecture Solutions Ltd.
 * This file is part of Essential Architecture Manager, 
 * the Essential Architecture Meta Model and The Essential Project.
 *
 * Essential Architecture Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Essential Architecture Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Essential Architecture Manager, in the file 'COPYING'.  
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 
Essential Architecture Manager - part of the Essential Project
==============================================================
www.enterprise-architecture.org

Essential Widgets
~~~~~~~~~~~~~~~~~
The Essential Widgets contain 2 Protege widgets in this plug-in.

1. Essential Viewer Tab tab widget, previously known as Essential Architecture Reporting Tab 
2. AutoText slot widget

To use these widgets, copy the com.enterprise_architecture.essential.widgets folder (or unzip
the essential-widgets.zip file) to <Your_Protege_Install>/plugins.
Basic help for the use of these widgets is included in the Help->Plugins... menu in Protege.
For more extensive help, please see the documentation at: 
http://www.enterprise-architecture.org


RELEASE NOTES
===============================================================

VERSION 4.2.1
=============
Released 09.01.2020
Updated all the libraries used by this component to their latest versions


VERSION 4.2
~~~~~~~~~~~
Released 15.03.2017
Improved publishing to Essential Viewers that are running on Tomcat 8, which changes the response to POST messages. 
This resolves publishing issues experienced when publishing to Tomcat 8 with form-based security enabled.
A new version of the Essential XML component is included which extends the content of the XML snapshot to include
class definitions.
Revised branding for the Essential Project and EAS.

VERSION 3.0
~~~~~~~~~~~
Released 01.08.2013
This release re-works the user interface, including a new name and icon for the tab. The new Essential
Viewer Tab provides an additional control allowing the user to choose whether or not to send the images
taken from all graphical models defined using the Graph Widget, e.g. Business Process Flow, Application 
Dependency Model, Technology Product Build Architecture.

In addition, the underlying engine now support both HTTP BASIC Authentication and form-based authentication.
The use of form-based login is detected automatically, provided specific comments are added to the login forms

Finally, the generated XML now includes the new <superclass> tag to each instance to enable Viewer queries
to operate on the inheritance hierarchy. 

VERSION 2.5
~~~~~~~~~~~
Released 03.11.2010
This release fixes a bug with the Essential Architecture Reporting Tab and introduces a timestamp tag
to the report XML snapshot that is sent to Essential Viewer. Version 2.5 is backwards compatible with
earlier releases of Essential Viewer (version 2.2).

Fixes:
- The empty architecture image bug, which prevents a successful sending of the repository XML to Essential
Viewer when any architecture in the repository has an empty Graph Widget (i.e. no elements on the canvas)
reported 27.10.2010. 
Any architecture (e.g. business process, architecture etc.) that has no graphical elements defined is now
rendered as an empty 1x1 image.

New Features:
- A <timestamp> tag is introduced into the reportXML.xml repository snapshot XML document. This timestamp
captures the time at which the repository was sent to Essential Viewer, providing reports / Views with
information about when the XML was generated (e.g. for a 'Last Updated' value). The reportXML.xml now refers
to an extended XML schema: http://www.enterprise-architecture.org/xml/essentialreportxml.xsd

- A new property to control the format and rendering of the <timestamp> tag value is available in the 
reporttab.properties file. The reporttab.xml.datetimeformat property can define an alternative formatting pattern
that is valid for the java.text.SimpleDateFormat. Note that this must result in a valid XSD DateTime value.
For more information, please see the Help HTML file for the tab (available from within Protege: Help->Plugins->Essential
Architecture Manager Report Tab

VERSION 2.4
~~~~~~~~~~~
Released: 25.11.2009
This release is an update to the ReportTab components of the Essential Widgets that creates and 
sends snapshot images of each graphical model (using GraphWidget) in addition
to sending the compressed repository snapshot to the ReportService in the Essential Viewer.
When running in Multi-User mode, the tab performs an auto-format on each image before sending it
as the layout is not stored by Protege in multi-user mode for well-documented reasons. An option 
for how this auto-layout is performed is presented to users working in Multi-User mode.

DEPENDENCIES
Essential Widgets 2.4 depends on Essential Viewer 2.2, which includes the appropriate receiver for 
the images. However, using Essential Widgets 2.4 with earlier versions of Essential will work in
sending the repository snapshot in XML but the image sending phase will fail with a bad URL error.


VERSION 2.3
~~~~~~~~~~~
Released: 23.10.2009
This release is an update to the ReportTab components of the Essential Widgets that compresses 
the repository snapshot before sending it to the ReportService in the Essential Viewer.

DEPENDENCIES
Essential Widgets 2.3 depends on Essential Viewer 2.0.4 and will not work with earlier versions
of the ReportService.

VERSION 2.2
~~~~~~~~~~~
Released: 30.05.2009
This is a patch release of the Essential Widgets to change the HTTP response codes used between
the ReportTab and the ReportService. This patch fixes the problem encountered when using Apache webserver
in front of the Tomcat web application server.


VERSION 2.1
~~~~~~~~~~~
Released: 20.04.2009
This is a patch release of the Essential Widgets to account for the changes to Protege 3.4.
Version 2.1 of the Essential Widgets is compatible with Protege versions 3.1, 3.3.1, 3.4, and all RC releases.

20.04.2009	JWC	The AutoTextWidget has been updated to fix the auto naming bug that is observed with Protege 3.4 RC2 and 
Protege 3.4. Thanks to Tania Tudorache and Timothy Redmond of the Protege Team for their help in tracking this down.


VERSION 2.0
~~~~~~~~~~~
Released: 01.01.2009
This release is a significant update to the Essential Architecture Reporting Tab tab widget.

- Reporting Service URL history is maintained.
A set of URLs that have been successfully used to publish the snapshot of the repository is
stored in a history and is accessed via an editable, drop-down combo-control that operates
like a web-browser URL entry field.
- Improved progress reporting
As the publishing process can take a few seconds for larger repositories and networks that
have reduced bandwidth, the progress in terms of what the Essential Architecture Reporting Tab
is doing has been improved. Previous versions _appeared_ to hang while taking a snapshot of the
repository, for example. Additionally, server errors (e.g. the Reporting Service in Essential
Viewer running out of memory) are now handled and reported. In version 1.x, this was just 
reported as an failure.
- Refactored Java class packages.
The Java package name for the Essential Widgets has been refactored to avoid namespace issues.

VERSION 1.x
~~~~~~~~~~~

15.02.2008	JWC	Resolved the ReportTab publishing bug when running in client-server mode. (Version 1.2. of the EAS Essential Widgets)

15.01.2008	JWC	Resolved missing events in AutoTextWidget issue in client-server mode. (Version 1.1.2 of the EAS Essential Widgets)

20.12.2007	JWC	Fixed bug with the EasReportTab widget which couldn't always find its own properties file. (Version 1.1.1 in the About page and version 1.1 of EasReportTab.java).

15.10.2007	JWC	Added the dispose() method to AutoTextWidget to release slot listeners (version 1.1.1 of AutoTextWidget)

17.09.2007	JWC	Newly refactored widgets and debugged for Protege 3.3.1






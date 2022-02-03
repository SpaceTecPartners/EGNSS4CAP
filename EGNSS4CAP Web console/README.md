EGNSS4CAP Web Console


Requirements:

•	Redhat WildFly 10 application server
•	MySQL 5.7 Database 
•	Eclipse IDE for J2EE

Install guide

1.	Setting up the database

	Install MySQL Database and import the database from the SQL file provided.
	Setup a user for the application server to connect as and give it priviledges to the “gsaweb” 	database just imported.

2.	Setting up the application server

	Install WildFly application server and setup an admin user for the application console.
	Enter the application server admin console and from Configuration→Subsystems→ 	Datasources→ Non-XA add a datasource called “GSAWebDS” that points to the MySQL 	database configured in step 1 and check for connectivity.

	In order to save pictures in the server filesystem a directory with write permissions from the 	application server user must be created, this path can be configured in the 	“config.properties” file in the JAVA source directory (step 4).
	
3.	Register to OpenCellID
	
	In order to get the cell tower data, register an account on https://opencellid.org/ and obtain 	an API token.

4.	Producing a WAR
Install Eclipse IDE for J2EE, Import the project clicking on the menu “File” → “Import...”→”General”→ “Existing projects into workspace”, and selecting the source code root directory, then select the “gsaConsole” project and click “Finish”.

In the JAVA sources directory the file “config.properties” contains the configurable items for the application:

- config.DataSourceName1 : The JAVA JNDI datasource name for the MySQL database 					 as setup in step 1.
	
	- config.fotoPath : The filesystem path for storing photos, put a filesystem 					directory with read and write permissions of the WildFly user

- config.openCellIdToken : The OpenCellID API token, insert here the API token 					 obtained from https://opencellid.org/ 

Wait for the build to finish then right click on the project name from “Project Explorer” window, select “Export” → “WAR File” and choose a destination.

5.	Deploy the application
	
	To deploy the WAR file gain access to the WildFly admin console, from Deployments → 	Add (deployment), select the 	WAR file produced in step 3 and wait for application 	deployment.

6.	The web console should be now operational.



-Install Node.js

-Install Ionic:

	npm install -g ionic

-Create a new Ionic v.1 Project:

	ionic start ECGnss blank --type ionic1

-Substitute the www folder with our version from GitHub

-Install all the required Cordova plugin:

	cordova plugin add plugin_name 

	(for the local ones you find on GitHub, the name is the full path to the plugin folder)

	you find all the required plugins in required_plugins.json

-Run the app on your smartphone:

	connect phone to usb port
	
	enable USB debugging on phone

	ionic cordova run android --device
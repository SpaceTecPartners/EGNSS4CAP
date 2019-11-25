var REMOTE_SERVER = "http://www.egnss4cap.com/gsaConsole/";
angular.module('starter', ['ionic', 'starter.controllers', 'mappa.module', 'opzioni.module', 'opzioni_gps_avanzate.module', 'sensoregps.module', 'lavorazioni.module', 'esci.module', 'about.module'])

var runApp = function($ionicPlatform, sqliteService, $translate, $rootScope) {
  console.log("app.run");

  $rootScope.rightSideMenuHidden = true;

  var firstrun = window.localStorage.getItem("vpn_firstrun");
  sqliteService.openConnection('visitapartnew.sqlite');

  var force_reset = false;

  sqliteService.getDefaultLang();

  if ( firstrun === null || force_reset ) {
    window.localStorage.setItem("vpn_firstrun", "1");
    sqliteService.getDefaultLang();
    sqliteService.populateDatabase( function success(){

    }, function error(){

    });
  }
  else {

  }

  if (window.cordova && window.cordova.plugins && window.cordova.plugins.Keyboard) {
    cordova.plugins.Keyboard.hideKeyboardAccessoryBar(true);
    cordova.plugins.Keyboard.disableScroll(true);
  }
  if (window.StatusBar) {
    StatusBar.styleDefault();
  }
};

var app = angular.module('app', [
  'ionic',
  'ngCordova',
  'login.module',
  'mappa.module',
  'app.module',
  'indexedDB',
  'pascalprecht.translate',
  'http_services',
  'opzioni.module',
  'clickAndWait',
  'opzioni_gps_avanzate.module',
  'sensoregps.module',
  'lavorazioni.module',
  'esci.module',
  'about.module'
]);

var configApp = function ($indexedDBProvider, $translateProvider, $ionicConfigProvider, $urlRouterProvider) {
  $urlRouterProvider.otherwise('/login');

  console.log("app.config");

  $ionicConfigProvider.views.swipeBackEnabled(false);

	console.log("AA");
	$translateProvider.useSanitizeValueStrategy(null);

  $translateProvider.registerAvailableLanguageKeys(['en-EN','it-IT'], {
    'en_US': 'en-EN',
    'en_UK': 'en-EN',
    'en': 'en-EN',
    'it':'it_IT'
  });

	console.log("BB");
  $translateProvider.useStaticFilesLoader({
    prefix: 'lang/',
    suffix: '.lang.json'
  });

  $indexedDBProvider
    .connection('visitaParticelleRVDB')
    .upgradeDatabase(1, function(event, db, tx){
      db.createObjectStore('lavorazioni', {keyPath: 'idLav'});

      var fotoPerPartStore = db.createObjectStore('fotoPerPart', {keyPath: ['uri_photo']});
      fotoPerPartStore.createIndex('idFoto_idx', ['uri_photo'], {unique: false});

      var trackPerPartStore = db.createObjectStore('trackPerPart', {keyPath: ['pkuid']});
      trackPerPartStore.createIndex('idTrack_idx', ['pkuid'], {unique: false});
    });
};

configApp.$inject = ['$indexedDBProvider', '$translateProvider', '$ionicConfigProvider', '$urlRouterProvider'];
runApp.$inject = ['$ionicPlatform', 'sqliteService', '$translate', '$rootScope'];


app.config(configApp);
app.run(runApp);

document.addEventListener('deviceready', function() {
  angular.bootstrap(document, ['app']);
}, false);


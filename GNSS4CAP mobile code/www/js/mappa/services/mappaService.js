var MappaService = function($http, $httpParamSerializerJQLike, $q, $indexedDB){

		var self = this;

		self.setPhotosAsUploaded = function(idLav, id_particella, arrayIdFoto){
			var deferred = $q.defer();
			$indexedDB.openStore('fotoPerPart', function(store){
				store.getAll().then(function(res){
					res.forEach(function(photo){
							for(var i = 0; i < arrayIdFoto.length; i++){
							  if(arrayIdFoto[i].date == photo.date){
									photo.id = arrayIdFoto[i].idFotoNew;
                  photo.uploaded = true;
                  store.upsert(photo);
								}
							}
					});
					deferred.resolve("");
				});
			});
			return deferred.promise;
		};

     self.insertPhotoUri = function(photo){
      var deferred = $q.defer();
			try{
        photo.uri_photo = photo.date;
        $indexedDB.openStore('fotoPerPart', function(store){
          store.insert(photo).then(function(e){
            deferred.resolve(e);
          });
        });
      }catch(err){
			  console.log("ERROR INSERT PHOTOURI: "+err)
      }
			return deferred.promise;
		};

		self.upsertPhotoUri = function(photo){
			var deferred = $q.defer();

			$indexedDB.openStore('fotoPerPart', function(store){
				 store.upsert(photo).then(function(e){
					 deferred.resolve(e);
					 });
			  });

			return deferred.promise;
		};

       self.insertTrack = function(date, pkuid, trackn, fromDevice, idLavoVisiPart){
          var track = {};
          track.idLavoVisiPart = idLavoVisiPart;
          track.pkuid = pkuid;
          track.date = date;
          track.trackWKT = trackn;
          track.uploaded = typeof idLavoVisiPart !== "undefined";
          track.fromDevice = fromDevice.toString();

          var deferred = $q.defer();

          try{
            $indexedDB.openStore('trackPerPart', function(store){
              store.insert(track).then(function(e){
                deferred.resolve(e);
              });
            });
          }catch(err){
            console.log("ERR IS: "+err)
          }

          return deferred.promise;
        };

         self.deleteTrack = function(pkuid){
             var deferred = $q.defer();
             $indexedDB.openStore('trackPerPart', function(store){
                 store.delete([pkuid]).then(function(){
                     deferred.resolve([]);
                 });
             });
             return deferred.promise;
         };

         self.deleteFoto = function(uri_photo){

          var deferred = $q.defer();

          $indexedDB.openStore('fotoPerPart', function(store){
               store.delete([uri_photo]).then(function(){
                deferred.resolve([]);
                });
              });
          return deferred.promise;
        };

       self.selectFoto = function(idLav, idParticella){
        var deferred = $q.defer();
        var retArr = [];

        $indexedDB.openStore('fotoPerPart', function(store){
          store.getAll().then(function(e){
            for(i in e){
              retArr.push(e[i]);
            }
            deferred.resolve(retArr);
          });
        });
        return deferred.promise;
      };

      self.selectFotoAll = function(){
        var deferred = $q.defer();
        var retArr = [];

        $indexedDB.openStore('fotoPerPart', function(store){
          store.getAll().then(function(e){
            for(i in e){
              retArr.push(e[i]);
            }
            deferred.resolve(retArr);
          });
        });

        return deferred.promise;

      };

      self.updateCodiceTracks = function(date, pkuid, trackWKT, fromDevice, idLavoVisiPart){

        var deferred = $q.defer();
        console.log("UPDATE COD TRACKS");
        self.insertTrack(date, pkuid, trackWKT, fromDevice, idLavoVisiPart).then(function(){
          deferred.resolve([]);
        });

        return deferred.promise;
      };

         self.selectTrack = function(){
          var deferred = $q.defer();
          var retArr = [];

          $indexedDB.openStore('trackPerPart', function(store){
            store.getAll().then(function(e){
              for(i in e){
                retArr.push(e[i]);
              }
              deferred.resolve(retArr);
            });
          });

          return deferred.promise;

        };

	};

MappaService.$inject = ['$http', '$httpParamSerializerJQLike', '$q', '$indexedDB'];
angular.module('mappa.module').service('MappaService', MappaService);

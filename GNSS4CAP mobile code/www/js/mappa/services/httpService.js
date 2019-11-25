var httpService = function($http, $httpParamSerializerJQLike, sessionService, $q, OL3Layers_service, $filter) {

  var self = this;

  self.REMOTE_SERVER_FOTO = "http://www.egnss4cap.com/gsaConsole/";

  self.blobToBase64 = function(tmpList, cnt, oriTag, destTag, blob) {
    tmpList = tmpList || [{ }];
    cnt = cnt || 0;
    var deferred = $q.defer();
    var reader = new FileReader();
    reader.onload = function() {
      var dataUrl = reader.result;
      var base64 = dataUrl.split(',')[1];
      tmpList[cnt][destTag] = dataUrl;
      deferred.resolve(dataUrl);
    };
    reader.readAsDataURL(blob);
    return deferred.promise;
  };

  self.postFoto = function(id_lav, id_part, listaFoto, successCallback, errorCallback, errorCallbackNoPhoto){
    var tmpList = [];
    var promises = [];
    var cnt = 0;
    listaFoto.forEach(function(f){
      if(!f.get('uploaded') && f.get("type") === 'PHOTO'){
        var fullObj = f.get("fullObject");
        fullObj.pointLat = fullObj.pointLat.toString();
        fullObj.pointLng = fullObj.pointLng.toString();
        tmpList.push(fullObj);

        promises.push(self.blobToBase64(tmpList, tmpList.length-1, "photoblob", "uri_photo", fullObj.photoblob));
      }
    });
    $q.all(promises).then(function(){
      if(tmpList.length > 0){
        tmpList.forEach(function(f){
          var to_send = [f];
          $http.defaults.headers.post['Content-Type'] = "application/x-www-form-urlencoded";
          var tmp_url = REMOTE_SERVER+"/UploadPhotoGSAServletAuth";
          $http.post(tmp_url,
          $httpParamSerializerJQLike({"dataInput":JSON.stringify({"lista_foto":to_send}), "t":localStorage.getItem("token")}),
          {timeout:120000}
        ).then(successCallback).catch(errorCallback);
        });
      }else{
        errorCallbackNoPhoto();
      }
    });
  };

  self.postFotoFromCamera = function(id_lav,id_part, photo, successCallback, errorCallback){
    self.blobToBase64(undefined, undefined, "photoblob", "uri_photo", photo.photoblob).then(function(pho){
      self.blobToBase64(undefined, undefined, "signatureblob", "signature", photo.signatureblob).then(function(phopho){
        photo.uri_photo = pho;
        photo.signature = phopho;
        $http.defaults.headers.post['Content-Type'] = "application/x-www-form-urlencoded";
        $http.post(self.REMOTE_SERVER_FOTO + "uploadFoto",
          $httpParamSerializerJQLike({"dataInput":JSON.stringify({"lista_foto":[photo]})}),
          {timeout:120000}
        ).then(successCallback).catch(errorCallback);
      })
    })
  };

  self.deleteFoto = function(id_lav, id_part, idFoto, successCallback, errorCallback){
    $http.defaults.headers.post['Content-Type'] = "application/x-www-form-urlencoded";
    $http.post(self.REMOTE_SERVER_FOTO + "UploadPhotoGSAServletAuth",
      $httpParamSerializerJQLike({"dataInput":JSON.stringify({"lista_foto":[{"id":idFoto}]}), "t":localStorage.getItem("token")}),
      {timeout:120000}
    ).then(successCallback).catch(errorCallback);
  };

  self.dataURItoBlob = function(dataURI) {
    var byteString = atob(dataURI.split(',')[1]);

    var mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0]

    var ab = new ArrayBuffer(byteString.length);

    var ia = new Uint8Array(ab);

    for (var i = 0; i < byteString.length; i++) {
      ia[i] = byteString.charCodeAt(i);
    }

    var blob = new Blob([ab], {type: mimeString});
    return blob;
  };

    self.getAltitudeFromLatLon = function(lat,lon){
      var deferred = $q.defer();
      $http({
        method: 'GET',
        url: "http://104.248.135.236:8080/api/v1/lookup?locations="+lat+","+lon,
        headers: {
          'Access-Control-Allow-Origin' : '*',
          'Access-Control-Allow-Methods' : 'POST, GET, OPTIONS, PUT',
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'oauth_nonce': '47901059'
        },
        data: {
        }
      }).then(function(resp) {
        deferred.resolve([resp.data]);
      }, function(err) {
        console.error('ERR', err);
        deferred.resolve([]);
      });
      return deferred.promise;
    };

  self.dowloadPhotoList = function(successCallback, errorCallback){
      $http.defaults.headers.post['Content-Type'] = "application/x-www-form-urlencoded";
      $http.post(self.REMOTE_SERVER_FOTO + "PhotosListGSAServletAuth",
        $httpParamSerializerJQLike({"dataInput":JSON.stringify({"user":localStorage.getItem("username")}), "t":localStorage.getItem("token")}),
        {timeout:120000}
      ).then(successCallback).catch(errorCallback);
  };

  self.downloadPhoto = function(id){
    var deferred = $q.defer();
    $http.defaults.headers.post['Content-Type'] = "application/x-www-form-urlencoded";
    $http.post(self.REMOTE_SERVER_FOTO + "GetPhotoGSAServlet",
    $httpParamSerializerJQLike({"dataInput":JSON.stringify({"id":id}), "t":localStorage.getItem("token")}),
      {timeout:120000}
    ).then(function success(resp){
      if(typeof resp.data !== 'undefined'){
        var photo = resp.data[Object.keys(resp.data)[0]];
        photo.photoblob = self.dataURItoBlob(photo.uri_photo);
        if(typeof photo.signature !== 'undefined'){
          photo.signatureblob = self.dataURItoBlob(photo.signature);
          delete photo.signature;
        }
        photo.uri_photo = photo.date;

        deferred.resolve(photo);
      }else if(typeof resp.data !== 'undefined' && typeof resp.data.errorMessage !== 'undefined'){
        deferred.reject("Errore nel download");
        console.log(resp.data.errorMessage);
      }else{
        deferred.reject("Errore del server durante il download");
      }

    }).catch(function error(err){
      deferred.reject(err);
    });
    return deferred.promise;
  };
};

httpService.$inject = ['$http', '$httpParamSerializerJQLike', 'sessionService', '$q', 'OL3Layers_service', '$filter'];
angular.module('http_services', []).service('httpService', httpService);

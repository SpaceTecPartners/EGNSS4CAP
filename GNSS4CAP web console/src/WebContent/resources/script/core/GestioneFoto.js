cryptoFoto = cryptoFoto || {};

cryptoFoto.gestioneFoto = function(){
	
	user = null;
	userIsAdmin = false;
	userIsAgency = false;
	fotoMap = new Map();
	requestMap = new Map();
	userInfo = {};
	
};

cryptoFoto.gestioneFoto.prototype.setUserInfo = function(obj){
	userInfo = obj;
}

cryptoFoto.gestioneFoto.prototype.setUserIsAdmin = function(bool){
	userIsAdmin = bool;
}

cryptoFoto.gestioneFoto.prototype.setUserIsAgency = function(bool){
	userIsAgency = bool;
}

cryptoFoto.gestioneFoto.prototype.setUser = function(userData){
	user = userData;
}

cryptoFoto.gestioneFoto.prototype.getUserInfo = function(){
	return {"id":userInfo.id, "role":userInfo.role, "user":user};
}

cryptoFoto.gestioneFoto.prototype.loadPageFoto = function(page, listaFoto){
	
	var user = localStorage.getItem("user");
	var token = localStorage.getItem("token");
	$('body').removeAttr('style');
    $('body').load("/"+localContext+"/"+page, function(response, status, xhr){
    	
    	datiAlfanumerici = new cryptoFoto.datiAlfanumerici([]);
    	gestioneMappa = new cryptoFoto.gestioneMappa();
    	creaLayers = new cryptoFoto.creaLayers();
    	
    	eventHandler = new cryptoFoto.eventHandler();
    	
    	gestioneMappa.creaMappa([], "map", true);
    	
    	let listaFoto = [];
    	
    	if(userIsAdmin || userIsAgency){
    		gestioneUtenti = new cryptoFoto.gestioneUtenti(user);
    		$("#buttonSpace").append("<img src='resources/img/admin-tools.png' title='User Management' id='userAdmin' class='m-left50;' />");
    		$("#userAdmin").click(function(evt){
    			gestioneUtenti.loadPageUtenti();
    		})
    		let content = '<th class="w-10">Id</th> \
						   <th class="w-15">Lat</th>\
						   <th class="w-15">Lng</th>\
    					   <th class="w-20">User</th>\
						   <th class="w-30">Upload date (UTC)</th>\
						   <th class="w-10" data-orderable="false">Action</th>';
    		$("#fotoTableContentTr").append(content);
    		$("#fotoTableFooterTd").attr("colspan",6)
    	}else{
    		let content =  '<th class="w-10">Id</th>\
							<th class="w-20">Lat</th>\
							<th class="w-20">Lng</th>\
							<th class="w-40">Upload date (UTC)</th>\
							<th class="w-10" data-orderable="false">Action</th>';
    		$("#fotoTableContentTr").append(content);
    		$("#fotoTableFooterTd").attr("colspan",5)
    	}
    	
    	
    	
    	showOverlay("Loading photos...");
    	
    	$.ajax({
    		url: REMOTE_SERVER+"PhotosListGSAServletAuth",
			dataType: "json",
			crossOrigin: true,
			type: "POST",
			data: "dataInput={user:'"+user+"'}&t="+token,
			success:function(result){
			
				var listaFoto = result.photos;
				listaFoto.forEach((f)=>{
					f.json = JSON.parse(f.json);
					f.status = JSON.parse(f.status);
					if(typeof f.cellInfo!== 'undefined'){
						f.cellInfo = JSON.parse(f.cellInfo);
					}
					let compStatus;
					if( f.status.location && (!f.status.nmea && !f.status.cell && (typeof f.json.extra_sat_count ==='undefined' || f.json.extra_sat_count == null || f.json.extra_sat_count < 0 ))){
						f.status.compStatus = false;
					}else{
						f.status.compStatus = true;
					}
				});
				datiAlfanumerici.caricaTabellaFoto(listaFoto, userInfo.role);
				removeOverlay();
			},
			error: function(error){
				console.error("error",error);
				removeOverlay();
				if(error && error.status && error.status == 403){
					alert("Session expired, please login again");
				}
			}
		});
	    	
    	
    	$("#logoutButton").click(function(){
    		$.ajax({
        		url: REMOTE_SERVER+"LogoutServletAuth",
    			dataType: "json",
    			crossOrigin: true,
    			type: "POST",
    			data: "u="+user,
    			success:function(result){
    				localStorage.clear();
    	    		location.reload();
    			},
    			error: function(error){
    				console.error("error in logout",error);
    			}
    		});
    		
    	});
    	
    	$('#nomeUtente').html(user);
 
    	$('#popup').hide();
    	
    	$('#c').click((evt)=>{
    		map.updateSize();
    	})
    	
    	$('#espandiRiduciDatiAlfanumerici').click(function(evt){
	 		
	 		if(!datiAlfanumericiFullScreen){
	 			
	 			layoutWestWidthOld = $('.layout-west').width();
	 			
	 			$('.layout-west').width($('#wrapper').width());
	 			$('.layout-east').hide();
	 			$(this).attr('src', imgPath+'/application-side-contract-icon.png');
	 			$(this).attr('title', 'Ripristina la visualizzazione a schermo diviso');
	 		}
	 		else{
	 			$('.layout-west').width(layoutWestWidthOld);
	 			$('.layout-east').show();
	 			$(this).attr('src', imgPath+'/application-side-expand-icon.png');
	 			$(this).attr('title', 'Espandi la sezione dei dati alfanumerici a tutto schermo');
	 			
	 			layoutWestWidthOld = 0;
	 		}
	 		
	 		datiAlfanumericiFullScreen = !datiAlfanumericiFullScreen;
	 	});
    });
};

cryptoFoto.gestioneFoto.prototype.abortDownloads = function(){
	requestMap.getKeys().forEach((k)=>{
		requestMap.get(k).abort();
	})
}

cryptoFoto.gestioneFoto.prototype.downloadPhoto = function(photoId, status, cb){

	var URL = REMOTE_SERVER + "/GetPhotoGSAServlet";
	
	
	 
	var dataInput = {
			id: photoId
	};
	
	if(!fotoMap.has(photoId)){
	
		
		
		let xhr = $.ajax({
			
			url:URL,
			dataType: "json",
			crossOrigin: true,
			contentType :'application/x-www-form-urlencoded; charset=UTF-8',
			type:'POST',
			data: "dataInput="+JSON.stringify(dataInput)+"&t="+localStorage.getItem("token"),
			success:function(result){
				result[photoId].status = status;
				fotoMap.set(photoId, result[photoId]);
				requestMap.remove(photoId);
				cb(result[photoId]);
			},
			error:(err)=>{
				requestMap.remove(photoId);
				console.error("Error downloading picture "+photoId,err);
				cb(); 
			}
		});
		requestMap.set(photoId, xhr);
	}else{
		cb(fotoMap.get(photoId));
	}
}

cryptoFoto.gestioneFoto.prototype.deletePhoto = function(photoId){
	var content = "<div class='deleteFotoConf'><p>Do you really want to delete this photo?</p><p><button id='confirmDelete'>Yes</button>&nbsp;&nbsp;&nbsp;<button id='cancelDelete'>No</button></p>";
	var box = $.fancybox({
		'content':content
	});
	
	$("#confirmDelete").click(function(){
		showOverlay("Deleting Photo...");
		$.ajax({
    		url: REMOTE_SERVER+"UploadPhotoGSAServletAuth",
			dataType: "json",
			crossOrigin: true,
			type: "POST",
			data: "dataInput={lista_foto:[{id:'"+photoId+"'}]}&t="+localStorage.getItem("token"),
			success:function(result){
				$.fancybox.close();
				$("#refreshList").click();
				removeOverlay();
			},
			error: function(error){
				console.error("error",error);
				$.fancybox.close();
				removeOverlay();
				if(error && error.status && error.status == 403){
					alert("Session expired, please login again");
				}else{
					alert("Error during operation");
				}
			}
		});
	});
	$("#cancelDelete").click(function(){
		$.fancybox.close();
	});
}

cryptoFoto.gestioneFoto.prototype.showPhoto = function(photoId){
	
	var _this = this;

	var content = "<div id='fotoPanel' style='width:1200px;height:700px;overflow-y:auto;'>";
	content += "<table class='tableStyle' id='detailTable' style='width:100%;'>";
	content += "<thead>";
	content += "<tr>";
	content += "<th width='50%'>Photo</th>";
	content += "<th width='10%'>Info.<br/>Camera</th>";
	content += "<th width='20%'>Metadata</th>";
	content += "<th width='20%'>Network cell tower and wifi</th>";
	content += "</tr>";
	content += "</thead>";
	content += "<tbody id='fotoTableBody'>";
	content += "</tbody>";
	content += "<tfoot>";
	content += "<tr>";
	content += "<td colspan='4'>&nbsp;</td>";
	content += "</tr>";
	content += "</tfoot>";
	content += "</table>";
	content += "<div class='rawDataPopup' id='rawDataPopup' style='display:none'></div>";
	content += "</div>";
	
	$.fancybox({
		'content':content
	});
	
	var foto = fotoMap.get(photoId);
	let detailMap;		
	try{
		
		let networkMapButton;
		
		if(typeof foto.network_info !== 'undefined'){
		
			networkMapButton = "<td style='text-align:center;'><img src='resources/img/map.png' pid='"+photoId+"' id='cellInfoBtn' style='width:32px;'/></td>";
			
		}else{
			networkMapButton = "<td style='text-align:center;'><p><b>No network info</b></p></td>";
		}
		
		var image = new Image();
		image.src = foto.uri_photo;
		image.style.maxWidth = "90%";
		image.style.maxHeight = "90%";
		image.id="img_"+photoId;
		image.title="Click to zoom photo";
		
		var nmeaParsed = [];
		
		image.onload = function(){
			
			var metadati;
			var lstData = [];
			var mostraPDF = false;
			
			var plain_data = extract_metadata(foto.uri_photo);
						
			if(!foto.status.compStatus){
				metadati = "<div style='width:150px'><div style='background:red;color:white;font-weight:bold;'>IMAGE IS INVALID OR HAS BEEN ALTERED</div>";
			}else{							
				metadati = "<div style='width:150px'><div style='background:green;color:white;font-weight:bold;'>IMAGE IS VALID</div>";
				var gps = new GPS;
				
				gps.on('data', function(parsed) {
					if(parsed.type="GGA" && parsed.lat && parsed.lon){
						nmeaParsed.push(parsed);
					}
				});
				var nmeaSentences = foto.nmea_foto.split(/\r\n|\r|\n/g);
				nmeaSentences.forEach((s)=>{
					try{
						gps.update(s);
					}catch(e){
						console.error("error in parse",e);
					}
				})
			}
			
			
			if(typeof plain_data !== 'undefined')
				lstData = plain_data.split('_');
			
			var exifObj = piexif.load(foto.uri_photo);
			
			foto.sha256 = exifObj['Exif'][piexif.ExifIFD.UserComment];
						
			if(lstData && lstData.length > 0){

				var timestampSource = null;
				var timestamp = null;
				var coordEST = null;
				var coordNORD = null;
				var UUID = null;
				var dataScatto = null;

				timestamp = lstData[0].replace(" gps","");
				coordEST = lstData[1];
				coordNORD = lstData[2];
				UUID = lstData[3];								
				
				foto.dataScatto = timestamp; 
					
				var coord = [parseFloat(coordEST), parseFloat(coordNORD)];
				var hdms = ol.coordinate.toStringHDMS(coord);
				
				var alt = parseFloat(foto.altitude).toFixed(1);
				if(isNaN(alt) || alt == 0 || alt == 'NaN'){
					alt = parseFloat(foto.altitude_locmanager).toFixed(1);
				}
				if (alt == 'NaN'){
					alt = "N/A";
				}
				foto.hdms = hdms;
				foto.uuid = UUID;
				
				var d = new Date(0); 
				
				if(typeof foto.centroid_used !=='undefined' && foto.centroid_used == 1){
					metadati += "<p><b>PHOTO HAS CORRECTION</b><br/>Precision "+foto.precision+" m</p>";
				}
				let accuracy = typeof foto.accuracy !== 'undefined' ? foto.accuracy.toFixed(1) : 'N/A';
				metadati += "<p><b>ACCURACY</b><br/>"+accuracy+" m</p>";				
				metadati += "<p><b>COORDINATES OF PHOTO</b><br/>"+hdms+"</p>";
				metadati += "<p><b>ALTITUDE OF PHOTO</b><br/>"+alt+" m</p>";
				metadati += "<p><b>TIMESTAMP OF PHOTO"+(timestampSource?("&nbsp;(from "+timestampSource+")"):"")+"</b><br/>"+foto.dataScatto+" UTC</p>";
				metadati += "<p><b>UUID OF USER DEVICE</b><br/>"+UUID+"</p>";
				if(foto.device_manufacturer && foto.device_model && foto.device_platform && foto.device_version){
					metadati += "<p><b>USER DEVICE</b><br/>"+foto.device_manufacturer+"&nbsp;"+foto.device_model+"&nbsp; Android "+foto.device_version+"</p>";
				}
				if(foto.sats_info && foto.sats_info.length > 0){
					metadati += "<p><b>SATELLITES INFO</b><br/></p>";
					metadati += "<div id='satsInfoPopup'><table>" +
							"<thead><tr>" +
							"<th>PRN</th>" +
							"<th>SNR</th>" +
							"<th>Elevation</th>" +
							"<th>Azimuth</th></tr>" +
							"<tbody>";
					for(var i=0; i<foto.sats_info.length; i++){
						metadati+="<tr>" +
								"<td>"+foto.sats_info[i].prn+"</td>"+
								"<td>"+foto.sats_info[i].snr+"</td>"+
								"<td>"+(foto.sats_info[i].elevation != null ? foto.sats_info[i].elevation : "N/A")+"</td>"+
								"<td>"+(foto.sats_info[i].azimuth != null ? foto.sats_info[i].azimuth : "N/A")+"</td></tr>";
					}
					metadati+="</tbody></table>";
				}
			}
			metadati += "</div>";
			
			var contentFoto = "<tr>";
			contentFoto += "<td align='center'><style>#img_"+photoId+":hover{cursor:zoom-in;}</style>";
			contentFoto += "<div id='foto_"+photoId+"' style='width:200px;'>";
			contentFoto += "</div>";
			contentFoto += "</td>";
			
			var motivo = "";
			
			if(foto.nota)
				motivo +="<br/>("+record.nota+")";
			
			var infoCamera = "";
			if(foto.magic)
				infoCamera += "<p><b>MAGIC</b><br/>"+foto.magic+"</p>";
			if(foto.camHeight)
				infoCamera += "<p><b>CAMERA HEIGHT</b><br/>"+foto.camHeight+"</p>";
			if(foto.alphaRound)
				infoCamera += "<p><b>ALPHA ROUND</b><br/>"+foto.alphaRound+"</p>";
			if(foto.betaRound)
				infoCamera += "<p><b>BETA ROUND</b><br/>"+foto.betaRound+"</p>";
			if(foto.gammaRound)
				infoCamera += "<p><b>GAMMA ROUND</b><br/>"+foto.gammaRound+"</p>";
			if(foto.fov)
				infoCamera += "<p><b>FOV</b><br/>"+foto.fov+"</p>";
			if(foto.detectedfov)
				infoCamera += "<p><b>DETECTED FOV</b><br/>"+foto.detectedfov+"</p>";
			if(foto.invertXCam)
				infoCamera += "<p><b>INVERT X CAM.</b><br/>"+foto.invertXCam+"</p>";
			if(foto.invertYCam)
				infoCamera += "<p><b>INVERT Y CAM.</b><br/>"+foto.invertYCam+"</p>";
			if(foto.invertXProj)
				infoCamera += "<p><b>INVERT X PROJ.</b><br/>"+foto.invertXProj+"</p>";
			if(foto.invertYProj)
				infoCamera += "<p><b>INVERT Y PROJ.</b><br/>"+foto.invertYProj+"</p>";
			if(foto.normalize)
				infoCamera += "<p><b>NORMALIZE</b><br/>"+foto.normalize+"</p>";
			if(foto.gamma)
				infoCamera += "<p><b>GAMMA</b><br/>"+foto.gamma+"</p>";
			if(foto.tilt_angle)
				infoCamera += "<p><b>TILT</b><br/>"+foto.tilt_angle+" &deg;</p>";
			if(foto.heading && !isNaN(foto.heading))
				infoCamera += "<p><b>HEADING</b><br/>"+foto.heading.toFixed(0)+" &deg;N</p>";
			
			contentFoto += "<td align='center'>"+infoCamera+"</td>";
			contentFoto += "<td align='center'>"+metadati+"</td>";

			contentFoto += "</tr>";
			contentFoto += "</table>";
			
			var rawDataPopup = "<table>";
			rawDataPopup += "<th>Timestamp</th>";
			rawDataPopup += "<th>Lat</th>";
			rawDataPopup += "<th>Lon</th>";
			rawDataPopup += "<th>Alt</th>";
			rawDataPopup += "<th>hdop</th>";
			nmeaParsed.forEach((m)=>{
				rawDataPopup += "<tr>";
				delete m.raw;

				rawDataPopup += "<td align='center'>"+DateFormat(m.time, "dd/mm/yyyy-hh:mm:ss")+"</td>";
				rawDataPopup += "<td align='center'>"+m.lat+"</td>";
				rawDataPopup += "<td align='center'>"+m.lon+"</td>";
				rawDataPopup += "<td align='center'>"+m.alt+"</td>";
				rawDataPopup += "<td align='center'>"+m.hdop+"</td>";
				rawDataPopup += "</tr>";
			})
			rawDataPopup += "</table>"
			
			$('#rawDataPopup').append(rawDataPopup);

			
			
			
			$('#fotoTableBody').append(contentFoto);
			
			$("#fotoTableBody").get(0).firstChild.innerHTML += networkMapButton;
	
			$("#cellInfoBtn").click(function(){

				var photoId = $(this).attr("pid");
				var mapPopup = "<div class='cellMapPopup' style='width:70vw; height:70vh;'>" +
						"<h4 style='text-align:center'>Network cell tower and wifi</h4>"+
						"<div id='cellInfoMap' style='width:100%; height:87%;'></div>" +
						"<div id='cellInfoText'></div>" +
						"</div>";
				
				$.fancybox({
					'content':mapPopup
				});
				
				let osmTileLayer = new ol.layer.Tile({
					zIndex : 0,
					title:"ortofotoCellInfo",
					nomeLayer: "CELLINFOLAYER",
					source: new ol.source.OSM({attributions:false})
				});
				
				let controls = ol.control.defaults({
					zoom : true,
					rotate : true,
					attribution : true
				});

				let interactions = ol.interaction.defaults({

					doubleClickZoom : false,
					keyboard : true,
					pinchRotate : true,
					dragAndDrop : false,
					dragAndDropEvent : false,
					dragPan : true,
					mouseWheelZoom : true
				});

				
				detailMap = new ol.Map({
					target: document.getElementById("cellInfoMap"),
					logo : false,
					controls : controls,
					interactions : interactions,
					layers : [osmTileLayer],
					view : new ol.View({
						projection : ol.proj.get("EPSG:3857"),
						zoom: 6,
						center : [0,0]
					})
				});
				
				gestioneMappa.visualizzaFoto(foto, foto.status, detailMap)
				setTimeout(()=>{
					gestioneMappa.fitMapToVectorLayers(detailMap);
				},500);
				
				
				
				
				// server data
				let networkInfoToAppend = "";
				let photo = fotoMap.get(photoId);
				let result = photo.cellInfo;				
				
				if(typeof result === 'undefined' || result == null){
					networkInfoToAppend +="<p>No network match</p>"
						networkInfoToAppend += "</td>";
				}else{
					try{
						result = JSON.parse(result);
						let convertedCoordinates = ol.proj.fromLonLat([result.lon, result.lat]);
						networkInfoToAppend += "<div class='celldatadetail'>" +
						"<p><b>Latitude</b> "+ result.lat+ "</p>" +
						"<p><b>Longitude</b> "+ result.lon+ "</p>" +
						"<p><b>Cell range</b> "+ result.accuracy + " m</p>";
						if(photo.status.distance){
							networkInfoToAppend += "<p><b>Distance from cell</b> "+ (!isNaN(Math.round(photo.status.distance)) ? Math.round(photo.status.distance) : "N/A") + " m</p>";
						}
						networkInfoToAppend += "<p><b>Address</b> "+ result.address + "</p>" +
						"</div>" +
						"</td>";
						
						let circle = new ol.geom.Circle(convertedCoordinates, result.accuracy);
						let feature = new ol.Feature(circle);
						
						let layer = new ol.layer.Vector({
							source : new ol.source.Vector({
								features : [ feature ]
							})
						});
						
						layer.setStyle(function style(feature, resolution) {
							
							return [new ol.style.Style({
						        fill: new ol.style.Fill({
							          color: "rgba(0,200,200,.4)"
						        }),
						        stroke: new ol.style.Stroke({
						          color: "rgba(0,200,200,.9)",
						          width: 1
						        })
						      }),
						      new ol.style.Style({
					        		geometry: new ol.geom.Point(feature.getGeometry().getCenter()),
									image: new ol.style.Icon(
										{
											src : "resources/img/celltower.png",
											anchor : [ 0.5, 0.5 ],
											anchorXUnits : 'fraction',
											anchorYUnits : 'fraction',
											opacity : 1,
											scale :  0.05
										})
								})
						      ]
						});
						
						detailMap.addLayer(layer);
						
						gestioneMappa.fitMapToVectorLayers(detailMap);
					}catch(e){
						networkInfoToAppend +="<p>No network match</p></td>";
					}
					
				}
				
				$("#cellInfoText").append(networkInfoToAppend);				
				
			})
			
			$('#foto_'+photoId).append(this);
			
			
			
			$(".showRawData").on("click",(e)=>{
				$(".showRawData").css("display","none");
				$(".rawDataPopup").css("display","");
				$(".closeRawData").css("display","");
			})
			$(".closeRawData").on("click",(e)=>{
				$(".showRawData").css("display","");
				$(".rawDataPopup").css("display","none");
				$(".closeRawData").css("display","none");
			})
			
			foto.image = this;

			$("#img_"+photoId).click(function(evt){
				
				$('#zoomFoto').fadeIn('slow');
				
				var image = new Image();
				image.src = foto.uri_photo;
				image.style.width = ((Math.round($('#zoomFoto').height())-100)*Math.round(foto.canvas_w))/Math.round(foto.canvas_h)+"px";
				image.id="img_zoom";
				$('#myImage').html(image);
				
				imageZoom("img_zoom", "myResult"); 
				
				$('#img_zoom').css("cursor","zoom-out");
				$('#chiudiZoom').click(function(evt){

					$('#myImage').empty();
					$('#myResult').empty();
					$('#zoomFoto').fadeOut('slow');
				});
			});
			
			impostaDataTableGenerico("#fotoTable");
		}
	}
	catch(e){
		console.error(e);
		removeOverlay();
	}					
	
	return [];
};


cryptoFoto.gestioneFoto.prototype.visualizzaImgElaborate = function(elaborazione){
	
   	if(elaborazione){
   		
   		if(elaborazione.phoenix){
   			
   			var phoenixImg = new Image();
   			
   			var content = "<div id='phoenixInfo'></div>";
			content += "<div id='imgPhoenix'></div>";
			
			$('#phoenixTab').append(content);
   			
   			phoenixImg.src = 'data:image/jpeg;base64,'+elaborazione.phoenix;
   			phoenixImg.width=400;
   			
   			$('#imgPhoenix').html(phoenixImg);
   		}
	   	if(elaborazione.ELA){
			var elaMin = elaborazione.ELA.elaMin;
			var elaMax = elaborazione.ELA.elaMax;
			var elaImg = elaborazione.ELA.ELAImg;
	
			
			var content = "<div id='elaInfo'>" +
					"<ul>" +
					"<li><b>Valore min:</b>&nbsp;" +elaMin+ "</li>"+
					"<li><b>Valore max:</b>&nbsp;" +elaMax+ "</li>"+
					"</ul>" +
					"</div>"
			content += "<div id='imgEla'></div>"
			$('#elaTab').append(content);
			
			var image = new Image();
			image.src = 'data:image/jpeg;base64,'+elaImg;
			$('#imgEla').html(image);
		}
	   	
	   	if(elaborazione.DQ){
			var DQMin = elaborazione.DQ.minProbValue;
			var DQMax = elaborazione.DQ.maxProbValue;
			var DQImg = elaborazione.DQ.DQImg;
	
			
			var content = "<div id='DQInfo'>" +
					"<ul>" +
					"<li><b>Valore min:</b>&nbsp;" +DQMin+ "</li>"+
					"<li><b>Valore max:</b>&nbsp;" +DQMax+ "</li>"+
					"</ul>" +
					"</div>"
			content += "<div id='imgDQ'></div>"
			$('#DQTab').append(content);
			
			var image = new Image();
			image.src = 'data:image/jpeg;base64,'+DQImg;
			$('#imgDQ').html(image);
		}
	   	
	   	if(elaborazione.DWNoise){
			var DWNoiseMin = elaborazione.DWNoise.minNoiseValue;
			var DWNoiseMax = elaborazione.DWNoise.maxNoiseValue;
			var DWNoiseImg = elaborazione.DWNoise.DWNoiseImg;
	
			
			var content = "<div id='DWNoiseInfo'>" +
					"<ul>" +
					"<li><b>Valore min:</b>&nbsp;" +DWNoiseMin+ "</li>"+
					"<li><b>Valore max:</b>&nbsp;" +DWNoiseMax+ "</li>"+
					"</ul>" +
					"</div>"
			content += "<div id='imgDWNoise'></div>"
			$('#DWNoiseTab').append(content);
			
			var image = new Image();
			image.src = 'data:image/jpeg;base64,'+DWNoiseImg;
			$('#imgDWNoise').html(image);
		}
	   	
	   	if(elaborazione.blk){
			var blkMin = elaborazione.blk.blkmin;
			var blkMax = elaborazione.blk.blkmax;
			var blkImg = elaborazione.blk.blkImg;
	
			
			var content = "<div id='blkInfo'>" +
					"<ul>" +
					"<li><b>Valore min:</b>&nbsp;" +blkMin+ "</li>"+
					"<li><b>Valore max:</b>&nbsp;" +blkMax+ "</li>"+
					"</ul>" +
					"</div>"
			content += "<div id='imgBlk'></div>"
			$('#blkTab').append(content);
			
			var image = new Image();
			image.src = 'data:image/jpeg;base64,'+blkImg;
			$('#imgBlk').html(image);
		}
	
	   	if(elaborazione.ghost){
	
			if(elaborazione.ghost.arrayGhost){
				
				var content = "";
				elaborazione.ghost.arrayGhost.forEach(function(ghost,index){
					
					var min = ghost.minValue;
					var max = ghost.maxValue;
					var difference = ghost.difference;
					var quality = ghost.quality;
					var img = ghost.ghostImg;
					
					content += "<div id='ghost"+index+"' style='border-bottom:1px solid black;padding-bottom:5px;'>";
					content += "<div id='ghostInfo"+index+"'>" +
					"<ul>" +
					"<li><b>Valore min:</b>&nbsp;" +min+ "</li>"+
					"<li><b>Valore max:</b>&nbsp;" +max+ "</li>"+
					"<li><b>Difference:</b>&nbsp;" +difference+ "</li>"+
					"<li><b>Quality:</b>&nbsp;" +quality+ "</li>"+
					"</ul>" +
					"</div>";
					content += "<div id='imgGhost"+index+"'></div>"
					content += "</div>";
					
					
				});
				
				$('#ghostTab').append(content);
				elaborazione.ghost.arrayGhost.forEach(function(ghost,index){
					
					var img = ghost.ghostImg;
					
					var image = new Image();
					image.src = 'data:image/jpeg;base64,'+img;
					$('#imgGhost'+index).html(image);
				});
			}
			
		}
   	}
   	
};
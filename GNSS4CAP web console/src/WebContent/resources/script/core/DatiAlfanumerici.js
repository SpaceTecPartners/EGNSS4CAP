cryptoFoto = cryptoFoto || {};

cryptoFoto.datiAlfanumerici = function(list){

	this.dtFoto = null;
	this.dtUsers = null;
};

cryptoFoto.datiAlfanumerici.prototype.caricaTabellaUtenti = function(listaUtenti){
var _this = this;
	
	if(this.dtUsers != null){
		this.dtUsers.fnDestroy();
	}
	
	this.dtUsers = $('#usersTable').dataTable( {
		
		"aaData": listaUtenti,
		"aoColumns": [{ "mData": "id",
			"mRender": function(data,type,full){
				
				return alignTDDataTable(data,"center");
			}
		},
		{ "mData": "user",
			"mRender": function(data,type,full){

				return alignTDDataTable(data,"center");
			}
		},
		{ "mData": "data_inizio",
			"mRender": function(data,type,full){
				
				return alignTDDataTable(data,"center");
			}
		},
		{ "mData": "data_fine",
			"mRender": function(data,type,full){
				
				return alignTDDataTable(data,"center");
			}
		},
		{ "mData": "roleDesc",
			"mRender": function(data,type,full){
				
				let dts = data;
				if(full.idAffiliation > 0 && full.role == 'USER'){
					for(let i=0; i<gestioneUtenti.adminsList.length;i++){
						if(gestioneUtenti.adminsList[i].id == full.idAffiliation){
							dts += "->"+gestioneUtenti.adminsList[i].user;
						}
					}
				}
				return alignTDDataTable(dts,"center");
			}
		},
		{ "mData": "action",
			"mRender": function(data,type,full){
				
				
				var img = "<img src='"+imgPath+"/modify.png' width='16px' height='16px' class='modifyUser' id='modifyUser' title='Modify User'/>" +
						  "<img src='"+imgPath+"/disable.png' width='16px' height='16px' class='disableUser' id='disableUser' title='Disable User' "+(typeof full.data_fine !== 'undefined' ? "style='display:none'" : "")+"/>" +
						  "<img src='"+imgPath+"/check-icon.png' width='16px' height='16px' class='enableUser' id='enableUser' title='Enable User' "+(typeof full.data_fine === 'undefined' ? "style='display:none'" : "")+"/>" ;
				
				return alignTDDataTable(img,"center");
			}
		}],
		"bSort": false,
		"bRetrieve": true, 
		"bProcessing": true,
		"bDestroy": true,
		"sPaginationType": "full_numbers",
		"fnHeaderCallback": function( nHead, aData, iStart, iEnd, aiDisplay ) {
			
		},
		"fnInitComplete":  function(settings, json) {
		    	
		    	var createUserButton = '<span id="createUser" class="createUser"><img src="./resources/img/create.png" style="width:35px"/></span>';
		    	$(createUserButton).appendTo('div.dataTables_filter');
		    	$("#createUser").on('click',function(evt){
		    		$("#crudShade").show();
		    		$("#username").val("");
		    		$("#password").val("");
		    		$("#confirmp").val("");
		    		$("#userIdHid").val("");
		    		$("#roleSelect").show();
		    		$("#roleSelect").val("USER");
		    		$("#roleSelectLabel").show();
					$("#adminSelect").show();
					$("#adminSelectLabel").show();
					$("#adminSelect").html("");
					for(let i=0;i<gestioneUtenti.adminsList.length;i++){
						$("#adminSelect").append("<option value='"+gestioneUtenti.adminsList[i].id+"'>"+gestioneUtenti.adminsList[i].user+"</option>");
					}
					
		    	});
		    	
		    	var refreshButton = '<span id="refreshList" class="refreshButton"><img src="./resources/img/refresh.svg"/></span>';
		    	var user = localStorage.getItem("user");
		    	var token = localStorage.getItem("token");
		    	$(refreshButton).appendTo('div.dataTables_filter');
		    	$("#refreshList").on('click',function(evt){
		    		evt.stopPropagation();
		    		showOverlay("Loading Users...");
		        	
		    		$.ajax({
		        		url: REMOTE_SERVER+"UsersListGSAServletAuth",
		    			dataType: "json",
		    			crossOrigin: true,
		    			type: "POST",
		    			data: "t="+token,
		    			success:function(result){
		    			
		    				removeOverlay();
		    				_this.caricaTabellaUtenti(result.users);
		    			},
		    			error: function(error){
		    				console.error("error",error);
		    				removeOverlay();
		    				if(error && error.status && error.status == 403){
		    					alert("Session expired, please login again");
		    				}
		    			}
		    		});
		    		
		    	})
		 },
		"fnCreatedRow": function( nRow, aData, iDataIndex) {
			$(nRow).find('.modifyUser').on('click',function(evt){
				evt.stopPropagation();
				$("#crudShade").show();
				$("#username").val(aData.user);
				$("#password").val("");
	    		$("#confirmp").val("");
	    		$("#userIdHid").val(aData.id);
	    		$("#roleSelect").val(aData.role);
				
				
				if(aData.role == 'USER'){
					$("#adminSelect").show();
					$("#adminSelectLabel").show();
					$("#adminSelect").val(aData.idAffiliation);
				}else{
					$("#adminSelect").hide();
					$("#adminSelectLabel").hide();
				}
	    		
			});
			$(nRow).find('.enableUser').on('click',function(evt){
				evt.stopPropagation();
				gestioneUtenti.enableUser(aData.id, aData.user);
			});
			$(nRow).find('.disableUser').on('click',function(evt){
				evt.stopPropagation();
				gestioneUtenti.disableUser(aData.id, aData.user);
			});
		}
	});
}

cryptoFoto.datiAlfanumerici.prototype.caricaTabellaFoto = function(listaFoto, role){
	var _this = this;
	var _dataLoading = false;
	
	if(this.dtFoto != null){
		this.dtFoto.fnDestroy();
	}
	
	let aoColumns;
	if(role == 'SUPERUSER' || role == 'PAYING_AGENCY'){
		aoColumns = [{ "mData": "id",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "json.pointLat",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "json.pointLng",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "json.username",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "date",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "id",
				"mRender": function(data,type,full){
					
					var img = "<img src='"+imgPath+"/Search-icon.png' width='16px' height='16px' class='visualizzaFoto' style='display:none' title='Photo details'/>" +
							"<img src='"+imgPath+"/spinner.svg' width='16px' height='16px' class='loadingFoto' title='Loading...'/>" +
							"<img src='"+imgPath+"/no-icon.png' width='16px' height='16px' class='errorFoto' style='display:none' title='An error has occurred'/>";;
					img+= "&nbsp;<img src='"+imgPath+"/trash.png' width='16px' height='16px' class='deleteFoto' title='Delete Photo'/>";
					
					return alignTDDataTable(img,"center");
				}
			}];
	}else{
		aoColumns = [{ "mData": "id",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "json.pointLat",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "json.pointLng",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "date",
				"mRender": function(data,type,full){
					
					return alignTDDataTable(data,"center");
				}
			},
			{ "mData": "id",
				"mRender": function(data,type,full){
					
					var img = "<img src='"+imgPath+"/Search-icon.png' width='16px' height='16px' class='visualizzaFoto' style='display:none'/>" +
							"<img src='"+imgPath+"/spinner.svg' width='16px' height='16px' class='loadingFoto'/>" +
							"<img src='"+imgPath+"/no-icon.png' width='16px' height='16px' class='errorFoto' style='display:none'/>";;
					
					return alignTDDataTable(img,"center");
				}
			}];
	}
	
	let downloadTablePictures = function(dt){
		gestioneFoto.abortDownloads();
		let pageSize = dt._iDisplayLength;
		let tableStart = dt._iDisplayStart;
		gestioneMappa.clearPhotoLayer();
		for(let idx = tableStart ; idx < (tableStart+pageSize); idx++ ){
			let i = dt.aiDisplay[idx];
			if(typeof dt.aoData[i] !== 'undefined'){
				let nTr = dt.aoData[i].nTr;
				let last = (i == (tableStart+pageSize)-1 ? true : false);
				
				let status = (typeof dt.aoData[i]._aData.status === 'object' ? dt.aoData[i]._aData.status : JSON.parse(dt.aoData[i]._aData.status));
				gestioneFoto.downloadPhoto(dt.aoData[i]._aData.id, status, (photo)=>{
					if(photo && photo.id){
						gestioneMappa.visualizzaFoto(photo, status);
						
						setTimeout(()=>{gestioneMappa.fitMapToPhotos();},200);
						
						var plain_data = extract_metadata(photo.uri_photo);
						if(typeof plain_data === 'undefined' || plain_data == ""){
							$(nTr).attr('class', 'bgcErr1');	
						}
						$(nTr).find(".visualizzaFoto").css("display","");
						$(nTr).find(".loadingFoto").css("display","none");
						$(nTr).find(".errorFoto").css("display","none");
					}else{
						//error in download
						$(nTr).find(".errorFoto").css("display","");
						$(nTr).find(".loadingFoto").css("display","none");
						if(photo && photo.status && photo.status == 403 && i == (tableStart+pageSize)-1){
	    					alert("Session expired, please login again");
						}
					}
				})
			}
		}
	}
	
	this.dtFoto = $('#fotoTable').dataTable( {
		
		"aaData": listaFoto,
		"aoColumns": aoColumns,
		"bSort": true,
		"bRetrieve": true, 
		"bProcessing": true,
		"bDestroy": true,
		"sPaginationType": "full_numbers",
		"fnHeaderCallback": function( nHead, aData, iStart, iEnd, aiDisplay ) {
			
		},
		"fnInitComplete":  function(settings, json) {
		    	removeOverlay();
		    	downloadTablePictures(settings);
		    	var refreshButton = '<span id="refreshList" class="refreshButton"><img src="./resources/img/refresh.svg"/></span>';
		    	var user = localStorage.getItem("user");
		    	var token = localStorage.getItem("token");
		    	$(refreshButton).appendTo('div.dataTables_filter');
		    	$("#refreshList").on('click',function(evt){
		    		evt.stopPropagation();
		    		showOverlay("Loading photos...");
		        	
		        	$.ajax({
		        		url: REMOTE_SERVER+"PhotosListGSAServletAuth",
		        		contentType :'application/x-www-form-urlencoded; charset=UTF-8',
		    			dataType: "json",
		    			crossOrigin: true,
		    			type: "POST",
		    			data: "dataInput={user='"+user+"'}&t="+token,
		    			success:function(result){
		    			
		    				var listaFoto = result.photos;
		    				listaFoto.forEach((f)=>{
		    					f.json = JSON.parse(f.json);
		    					f.status = JSON.parse(f.status);
		    					let compStatus;
		    					if( f.status.location && (!f.status.nmea && !f.status.cell && (typeof f.json.extra_sat_count ==='undefined' || f.json.extra_sat_count == null || f.json.extra_sat_count < 0 ))){
		    						f.status.compStatus = false;
		    					}else{
		    						f.status.compStatus = true;
		    					}
		    					if(typeof f.cellInfo!== 'undefined'){
		    						f.cellInfo = JSON.parse(f.cellInfo);
		    					}
		    				});
		    				_this.caricaTabellaFoto(listaFoto,localStorage.getItem("userRole"));
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
		    		
		    	})
		 },
		"fnCreatedRow": function( nRow, aData, iDataIndex) {
			$(nRow).css('cursor','pointer');
			
			
			let status;
			if(typeof aData.status !== 'undefined' && aData.status != ""){
				status = (typeof aData.status === 'object' ? aData.status : JSON.parse(aData.status));
				if(!status.compStatus){
					$(nRow).attr('class', 'bgcErr1');
				}else{
					$(nRow).attr('class', 'bgcOk');
				}
			}else{
				$(nRow).attr('class', 'bgcErr3');
			}
						
			$(nRow).find('.visualizzaFoto').on('click',function(evt){
				evt.stopPropagation();
				gestioneFoto.showPhoto(aData.id);
			});
			
			$(nRow).find('.deleteFoto').on('click',function(evt){
				evt.stopPropagation();
				gestioneFoto.deletePhoto(aData.id);
			});
			
			$(nRow).hover(function(){

				highlightPhoto(aData);
			}, function(){
				
			});
		}
	});
	
	if(typeof $._data($("#fotoTable").get(0), "events").page === 'undefined'){
		$('#fotoTable').on( 'page.dt', function (evt, dt) { 
			downloadTablePictures(dt);
		});
		$('#fotoTable').on( 'search.dt', function (evt, dt) {
		    downloadTablePictures(dt);
		    
		});
	}
	
}

function impostaDataTableGenerico(id){
	
	$(id).dataTable({
		"bSort": false,
		"bRetrieve": true, 
		"bProcessing": true,
		"bDestroy": true,
		"fnCreatedRow": function( nRow, aData, iDataIndex) {
			
			$(nRow).css('cursor','pointer');
			$(nRow).addClass('bgc'+(iDataIndex%2));
		},
		"sPaginationType": "full_numbers",
		"bProcessing": true,
		"aLengthMenu": [[10, 25, 50, -1], [10, 25, 50, "Tutti"]]
	});
}

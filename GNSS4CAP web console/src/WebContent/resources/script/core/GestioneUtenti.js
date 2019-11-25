cryptoFoto = cryptoFoto || {};

cryptoFoto.gestioneUtenti = function(user){
	
	userName = user;
	adminsList = [];
};

cryptoFoto.gestioneUtenti.prototype.enableUser = function(userId, username){
	let choice = confirm("Enable user '"+username+"' ?")
	if(choice){
		let token = localStorage.getItem("token");
		let data = "a=activate&t="+token+"&userid="+userId;
		
		$.ajax({
    		url: REMOTE_SERVER+"UserActionsGSAServletAuth",
			dataType: "json",
			crossOrigin: true,
			type: "POST",
			data: data,
			success:function(result){
				alert("User successfully enabled");
				gestioneUtenti.loadPageUtenti();
			},
			error: function(error){
				console.error("error in user action",error);
				alert("Error during operation");
			}
		});
	}
}

cryptoFoto.gestioneUtenti.prototype.disableUser = function(userId, username){
	let choice = confirm("Disable user '"+username+"' ?")
	if(choice){
		let token = localStorage.getItem("token");
		let data = "a=suspend&t="+token+"&userid="+userId;
		
		$.ajax({
    		url: REMOTE_SERVER+"UserActionsGSAServletAuth",
			dataType: "json",
			crossOrigin: true,
			type: "POST",
			data: data,
			success:function(result){
				alert("User successfully suspended");
				gestioneUtenti.loadPageUtenti();
			},
			error: function(error){
				console.error("error in user action",error);
				alert("Error during operation");
			}
		});
	}
}

cryptoFoto.gestioneUtenti.prototype.loadPageUtenti = function(){
	
	var _this = this;
	
	$('body').removeAttr('style');
	//var adminsList;
	var role = gestioneFoto.getUserInfo().role;
    $('body').load("/"+localContext+"/userConsole.html", function(response, status, xhr){
    	
    	
    	datiAlfanumerici = new cryptoFoto.datiAlfanumerici([]);
    	
    	$('#nomeUtente').html(localStorage.getItem("user"));
    	
    	// ajax per lista utenti  	
    	showOverlay("Loading users...");  			
		
		if(role != 'SUPERUSER'){
			$("#roleSelect").remove();
			$("#roleSelectLabel").remove();
			$("#adminSelect").remove();
			$("#adminSelectLabel").remove();
		}else{
			$("#roleSelect").change((val)=>{
					let selRole = val.target.selectedOptions[0].value;
					if(selRole != "USER"){
						$("#adminSelect").hide();
						$("#adminSelectLabel").hide();
					}else{
						$("#adminSelect").show();
						$("#adminSelectLabel").show();
					}
				})
		}
		
    	var token = localStorage.getItem("token")
    	
    	$.ajax({
    		url: REMOTE_SERVER+"UsersListGSAServletAuth",
			dataType: "json",
			crossOrigin: true,
			type: "POST",
			data: "t="+token,
			success:function(result){
			
				if(typeof result.admins !== 'undefined' && role == 'SUPERUSER'){
					_this.adminsList = result.admins;
					$("#adminSelect").html("");
					for(let i=0;i<_this.adminsList.length;i++){
						$("#adminSelect").append("<option value='"+_this.adminsList[i].id+"'>"+_this.adminsList[i].user+"</option>");
					}					
				}
				removeOverlay();
				datiAlfanumerici.caricaTabellaUtenti(result.users);
				
			},
			error: function(error){
				console.error("error",error);
				removeOverlay();
				if(error && error.status && error.status == 403){
					alert("Session expired, please login again");
				}
			}
		});
    	
    	$("#backBtn").click(function(){
    		gestioneFoto.loadPageFoto('fotoConsole.jsp',[]);
    	})
	    	
    	$("#userSubmit").click(function(){
    		let username = $("#username").val();
    		let password = $("#password").val();
    		let confirmp = $("#confirmp").val();
    		let userid = $("#userIdHid").val();
			//let superu = $("#superu").prop('checked');
    		let selRole = $("#roleSelect").val();
			
			let valid = true;
			let message = "";
			
			if(username.length == 0){
				valid = false;
				message += "Username cannot be blank\n";	
			}
			if(username.length > 32){
				valid = false;
				message += "Username cannot be more than 32 characters\n";
			}
			if(password.length > 0 && password.length < 5){
				valid = false;
				message += "Password cannot be less than 5 characters\n";
			}
			if(password != confirmp){
				valid = false;
				message += "Password and confirm password field must be the same\n";
			}
						
			if(valid){
				let token = localStorage.getItem("token");
				let action = "create";
				let data = "t="+token;
				if(typeof userid !== 'undefined' && userid != ''){
					action = "update";
					data += "&userid="+userid;
				}else{
					data += "&username="+username;
				}
				data += "&a="+action+"&password="+password;;
				
				let adminRole = gestioneFoto.getUserInfo().role;
				if(adminRole == 'SUPERUSER'){
					data += "&role="+selRole;
					if(selRole == 'USER'){
						let adminId = $("#adminSelect").val();
						//if(adminId != "0"){
							data += "&idp="+adminId;
						//}
					}
					
				}
				
				$.ajax({
	        		url: REMOTE_SERVER+"UserActionsGSAServletAuth",
	    			dataType: "json",
	    			crossOrigin: true,
	    			type: "POST",
	    			data: data,
	    			success:function(result){
	    				alert("User successfully "+(action == 'create' ? 'created' : 'updated'));
	    				
						$("#username").val("");
			    		$("#password").val("");
			    		$("#confirmp").val("");
			    		$("#userIdHid").val("");
						$("#roleSelect").val("USER")
						$("#crudShade").hide();
						
						gestioneUtenti.loadPageUtenti();
	    			},
	    			error: function(error){
	    				console.error("error in user action",error);
	    				alert("Error during user "+(action == 'create' ? 'creation' : 'update'));$("#username").val("");
			    		$("#password").val("");
			    		$("#confirmp").val("");
			    		$("#userIdHid").val("");
						$("#superu").prop('checked', false);
						$("#crudShade").hide();
	    			}
	    		});
			}else{
				alert("Problems detected:\n\n"+message);
			}
			
			
    	})
    	
    	$("#closeCrud").click(function(){
    		$("#crudShade").hide();
    	})
    	
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
    				//localStorage.clear();
    	    		//location.reload();
    			}
    		});
    		
    	});
    	
    	$('#nomeUtente').html(user);
    	$('#accordion').accordion({
    		heightStyle: "content",
    		header: "h3",
    		collapsible: true,
			// active: true
    	});
    	
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
var campagnaSel = null;
var localContext = "gsaConsole";

$(document).ready(function(){
	
	
	var hostName = (document.location.hostname == "" ? "localhost" : document.location.hostname);
	REMOTE_SERVER = window.location.protocol+"//"+document.location.hostname+":"+document.location.port+"/"+localContext+"/";
	
	$('#loginBtn').click(function(evt){

		var user = $('#user').val().trim();
		var password = $('#password').val().trim();
		var campagna = $('#campagna').val();
		campagnaSel = campagna;
		$('#errorMessage').empty();
		
		if(user == ''){
			
			$('#errorMessage').html("Inserire un Utente");
		}
		else if(password == ''){
			$('#errorMessage').html("Digitare una password per l'utente "+user);
		}
		else{
			
			showOverlay();
			
			$.ajax({ //autenticazione al server con BASIC AUTH
				url: REMOTE_SERVER+"authServlet",						
				dataType: "json",
				crossOrigin: true,
				type:"POST",
				data: "u="+user+"&p="+password,
 				success: function(res){
 					removeOverlay();
 					if(res && res.result){
                        localStorage.setItem("logged",true);
                        localStorage.setItem("token", res.token);
                        localStorage.setItem("user", user);
                        localStorage.setItem("userId", res.userInfo.id);
                        localStorage.setItem("userRole", res.userInfo.role);
                        
						gestioneFoto = new cryptoFoto.gestioneFoto();
						gestioneFoto.setUser(user);
						gestioneFoto.setUserInfo(res.userInfo);
						if(res.userInfo.role == 'SUPERUSER'){
							gestioneFoto.setUserIsAdmin(true);
						}else if(res.userInfo.role == 'PAYING_AGENCY'){
							gestioneFoto.setUserIsAgency(true);
						}
						
						gestioneFoto.loadPageFoto('fotoConsole.jsp',[]);
 					}
 					else{
 						
 						$('#errorMessage').html("Wrong username or password ");
 					}
 				},
 				error: function(res){
					removeOverlay();
 					$('#errorMessage').html("Login error.");
 				}
			});
		}
	});
});
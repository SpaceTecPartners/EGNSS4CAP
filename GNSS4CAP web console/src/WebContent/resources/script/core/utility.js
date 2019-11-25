function extract_metadata(imgBase64){
	var exifObj = piexif.load(imgBase64);
	var metadata = exifObj['Exif'][piexif.ExifIFD.UserComment];

	return metadata;
}

function showOverlay(messaggio)
{
	var overlay, tmp, loading, wrap,loadingTimer, loadingFrame = 1;
	
	if(!messaggio)
		messaggio = "Operazione in corso..."; 
	
	$('body').append(
			overlay	= $('<div id="overlay"></div>'),
			caricamento = $('<div id="caricamento"><b>'+messaggio+'</b></div>'),
			loading	= $('<div id="loading"><div></div></div>')
		);
	
	overlay.css({
		'background-color' : '#FFF',
		'opacity' : 0.8,
		'cursor' : 'auto',
		'height' : $(document).height()
	});

	_animate_loading = function() {
		if (!loading.is(':visible')){
			clearInterval(loadingTimer);
			return;
		}

		$('div', loading).css('top', (loadingFrame * -40) + 'px');

		loadingFrame = (loadingFrame + 1) % 12;
	};
	
	
	overlay.show();
	clearInterval(loadingTimer);

	loading.show();
	loadingTimer = setInterval(_animate_loading, 66);
}

function removeOverlay()
{
	$('#overlay').remove();
	$('#caricamento').remove();
	$('#loading').remove();
}

function alignTDDataTable(value, align)
{
	if(value != null && value != undefined)
		return "<div style='text-align:"+align+";'>"+value+"</div>";
	else
		return "";
}

function showTooltip(nRow, title, appendTo){
	
	if(!appendTo)
		appendTo = "body";
	
	if(title != null && title != undefined && title != ''){
		
		$(nRow).hover(function(){
	        // Hover over code
	        $(this).data('tipText', title).removeAttr('title');
	        $('<p class="tooltip"></p>')
	        .text(title)
	        .appendTo(appendTo)
	        .fadeIn('slow');
		}, function() {
		        // Hover out code
		        $(this).attr('title', $(this).data('tipText'));
		        $('.tooltip').remove();
		}).mousemove(function(e) {
		        var mousex = e.pageX + 20; //Get X coordinates
		        var mousey = e.pageY + 10; //Get Y coordinates
		        $('.tooltip')
		        .css({ top: mousey, left: mousex });
		});
	}
}

function imageZoom(imgID, resultID) {
	  var img, lens, result, cx, cy;
	  img = document.getElementById(imgID);
	  result = document.getElementById(resultID);
	  /*create lens:*/
	  lens = document.createElement("DIV");
	  lens.setAttribute("class", "img-zoom-lens");
	  lens.setAttribute("style", "z-index:999");
	  /*insert lens:*/
	  img.parentElement.insertBefore(lens, img);
	  /*calculate the ratio between result DIV and lens:*/
	  cx = result.offsetWidth / lens.offsetWidth;
	  cy = result.offsetHeight / lens.offsetHeight;
	  /*set background properties for the result DIV*/
	  result.style.backgroundImage = "url('" + img.src + "')";
	  result.style.backgroundSize = (img.width * cx) + "px " + (img.height * cy) + "px";
	  /*execute a function when someone moves the cursor over the image, or the lens:*/
	  lens.addEventListener("mousemove", moveLens);
	  img.addEventListener("mousemove", moveLens);
	  /*and also for touch screens:*/
	  lens.addEventListener("touchmove", moveLens);
	  img.addEventListener("touchmove", moveLens);
	  function moveLens(e) {
	    var pos, x, y;
	    /*prevent any other actions that may occur when moving over the image*/
	    e.preventDefault();
	    /*get the cursor's x and y positions:*/
	    pos = getCursorPos(e);
	    /*calculate the position of the lens:*/
	    x = pos.x - (lens.offsetWidth / 2);
	    y = pos.y - (lens.offsetHeight / 2);
	    /*prevent the lens from being positioned outside the image:*/
	    if (x > img.width - lens.offsetWidth) {x = img.width - lens.offsetWidth;}
	    if (x < 0) {x = 0;}
	    if (y > img.height - lens.offsetHeight) {y = img.height - lens.offsetHeight;}
	    if (y < 0) {y = 0;}
	    /*set the position of the lens:*/
	    lens.style.left = x + "px";
	    lens.style.top = y + "px";
	    /*display what the lens "sees":*/
	    result.style.backgroundPosition = "-" + (x * cx) + "px -" + (y * cy) + "px";
	  }
	  function getCursorPos(e) {
	    var a, x = 0, y = 0;
	    e = e || window.event;
	    /*get the x and y positions of the image:*/
	    a = img.getBoundingClientRect();
	    /*calculate the cursor's x and y coordinates, relative to the image:*/
	    x = e.pageX - a.left;
	    y = e.pageY - a.top;
	    /*consider any page scrolling:*/
	    x = x - window.pageXOffset;
	    y = y - window.pageYOffset;
	    return {x : x, y : y};
	  }
	}

function DateFormat(date, formatString){
    
	var DateToFormat = null;
	
	if (typeof date=='undefined'){
    	DateToFormat=new Date();
    }
    else{
        DateToFormat=date;
    }
    var DAY         = DateToFormat.getDate();
    var DAYidx      = DateToFormat.getDay();
    var MONTH       = DateToFormat.getMonth()+1;
    var MONTHidx    = DateToFormat.getMonth();
    var YEAR        = DateToFormat.getYear();
    var FULL_YEAR   = DateToFormat.getFullYear();
    var HOUR        = DateToFormat.getHours();
    var MINUTES     = DateToFormat.getMinutes();
    var SECONDS     = DateToFormat.getSeconds();

    var arrMonths = new Array("January","February","March","April","May","June","July","August","September","October","November","December");
    var arrDay=new Array('Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday');
    var strMONTH;
    var strDAY;
    var strHOUR;
    var strMINUTES;
    var strSECONDS;
    var Separator;

    if(parseInt(MONTH)< 10 && MONTH.toString().length < 2)
        strMONTH = "0" + MONTH;
    else
        strMONTH=MONTH;
    if(parseInt(DAY)< 10 && DAY.toString().length < 2)
        strDAY = "0" + DAY;
    else
        strDAY=DAY;
    if(parseInt(HOUR)< 10 && HOUR.toString().length < 2)
        strHOUR = "0" + HOUR;
    else
        strHOUR=HOUR;
    if(parseInt(MINUTES)< 10 && MINUTES.toString().length < 2)
        strMINUTES = "0" + MINUTES;
    else
        strMINUTES=MINUTES;
    if(parseInt(SECONDS)< 10 && SECONDS.toString().length < 2)
        strSECONDS = "0" + SECONDS;
    else
        strSECONDS=SECONDS;

    switch (formatString){
        case "hh:mm:ss":
            return strHOUR + ':' + strMINUTES + ':' + strSECONDS;
            
        case "dd/mm/yyyy-hh:mm:ss":
        	return strDAY+"/"+strMONTH+"/"+FULL_YEAR+"-"+strHOUR + ':' + strMINUTES + ':' + strSECONDS;
        	break;
        default:
        	console.log("NO FORMAT");
        //More cases to meet your requirements.
    }
}

function IsFullScreenCurrently() {
	var full_screen_element = document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement || document.msFullscreenElement || null;
	
	// If no element is in full-screen
	if(full_screen_element === null)
		return false;
	else
		return true;
}

/* Get into full screen */
function GoInFullscreen(element) {
	if(element.requestFullscreen)
		element.requestFullscreen();
	else if(element.mozRequestFullScreen)
		element.mozRequestFullScreen();
	else if(element.webkitRequestFullscreen)
		element.webkitRequestFullscreen();
	else if(element.msRequestFullscreen)
		element.msRequestFullscreen();
}

/* Get out of full screen */
function GoOutFullscreen() {
	if(document.exitFullscreen)
		document.exitFullscreen();
	else if(document.mozCancelFullScreen)
		document.mozCancelFullScreen();
	else if(document.webkitExitFullscreen)
		document.webkitExitFullscreen();
	else if(document.msExitFullscreen)
		document.msExitFullscreen();
}
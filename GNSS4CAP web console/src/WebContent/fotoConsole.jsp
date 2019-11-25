<input type='hidden' id='idPagina' />
<input type='hidden' id='lavorazione' />
<div id="canvasFoto"></div>
<div id='zoomFoto'>
	<div id='chiudiZoom'>
		<center><h3>Close Zoom</h3></center>
	</div>
	<div class='img-zoom-container'>
		<div id='myImage'></div>
		<div id='myResult' class='img-zoom-result'></div>
	</div>
</div>

<div id="container">
	<div id="wrapper">
		<div class="target" id="target"></div>
		<div class="expand" id="expand"></div>
		<div class="back" id="back"></div>
		<div class="mainHeaderLogo" id="mainHeaderLogo"></div>
		<div class="button-full nn" id="buttonSpace">
		</div>
		<input checked="checked" id="c" type="checkbox" /> <label id="icon_r"
			for="c"></label>

		<div class="layout-west col-sm-12 col-md-6">
			<div class="f-left col-sm-12 nn">
				<div class="col-lg-12 col-xl-8 float-left title-console nn">
					<h1 class="font-b nn">Geotagged photos Console</h1>
				</div>
				<div class="col-lg-12 col-xl-4 user float-left nn">
					<h4>
						<img src="resources/img/user.png">
						<span class="font-b" id='nomeUtente'></span>&nbsp;
						<img src="resources/img/Logout-icon.png" id="logoutButton" title="Logout">
					</h4>
				</div>
			</div>
			<div id='intestazione' class="float-left col-12 nn"></div>
			<div class="w-100 f-left">


				<div id='accordion' class="f-left col-12">				
					<h4 id='fotoTableH3' class='fotoTableClass'>Photos list</h4>
					<div id='fotoTableDIV'
						class='accordionContent fotoTableClass'>
						<table id='fotoTable' class='tableStyle W-100'>
							<thead>
								<tr id="fotoTableContentTr">
								</tr>
							</thead>
							<tfoot>
								<tr>
									<td id="fotoTableFooterTd" colspan='5'></td>
								</tr>
							</tfoot>
						</table>
					</div>
				</div>
				<div id='message' class="d-none" align='center'></div>
			</div>
		</div>
		<div class="layout-east" id="containerMap">
			<div id="map" class="map" tabindex="0">
				<div id="popup" class="ol-popup">
					<a href="#" id="popup-closer" class="ol-popup-closer"></a>
					<div id="popup-content"></div>
				</div>
			</div>
		</div>
		<label id="icon_d" for="d"></label> <input id="d" type="checkbox" />
		<div id='toolbarMappa'></div>
	</div>
</div>

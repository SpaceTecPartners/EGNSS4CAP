package it.erasmicoin.crypto.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import it.erasmicoin.dao.MetodiDAOMySQL;
import it.erasmicoin.utility.Utility;
/**
 * Servlet implementation class UploadFotoServlet
 */
public class UploadPhotoGSAServletAuth extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	public static Logger logger = Logger.getLogger(UploadPhotoGSAServletAuth.class);

    public UploadPhotoGSAServletAuth() {
        super();
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		logger.debug("UploadPhotoGSAServletAuth");
		JSONObject jsonObject = null;
		JSONArray listaFoto = null;
		
		String idFoto = null;
		Long idFotoNew = null;
		
		JSONArray jsonArrayOutput = new JSONArray();
		JSONArray jsonArrayTamperingConsole = new JSONArray();

		
		MetodiDAOMySQL daoMySQL = new MetodiDAOMySQL();
		
		try {

        	String dataInput = request.getParameter("dataInput");
        	
        	jsonObject = new JSONObject(dataInput);
			
			if(jsonObject.has("lista_foto")){
				
	        	listaFoto = jsonObject.getJSONArray("lista_foto");
				
				if(listaFoto != null){

					for(int i = 0; i < listaFoto.length(); i++){
						
						int progFoto = (i+1);
						
						JSONObject fotoObj = listaFoto.getJSONObject(i);
						
						idFoto = Utility.getStringFromJSONField("id", fotoObj);
						
						if(idFoto == null){
						
							fotoObj.put("progFoto", ""+progFoto);
							String base64Image = fotoObj.getString("uri_photo").split(",")[1]; 						
		                	idFotoNew = daoMySQL.insertGSAPhoto(fotoObj);
		                	
		                	String fileName = "photo_"+idFotoNew+"."+Utility.getStringFromJSONField("ext", fotoObj);
			                
							try{
								byte[] fileContentByteArray = Base64.decodeBase64(base64Image);
								Utility.salvaImg(fileName, ""+idFotoNew ,fileContentByteArray);
								Path pathImg = Paths.get(Utility.getFolderName(""+idFotoNew)+"/"+fileName);
								Metadata metadata2 = ImageMetadataReader.readMetadata(pathImg.toFile());
								logger.error("METADATA RECEIVED");
								ExifSubIFDDirectory exifSubIFD = metadata2.getOrCreateDirectory(ExifSubIFDDirectory.class);
						        
						        String userComment = exifSubIFD.getString(ExifSubIFDDirectory.TAG_USER_COMMENT);
						        JSONObject status = new JSONObject();
						        status.put("userComment", false);
						        status.put("nmea", false);
						        status.put("location", false);
						        if(!userComment.isEmpty()) {
						        	status.put("userComment", true);
						        }
						        
						        if(fotoObj.has("nmea_foto") && !fotoObj.getString("nmea_foto").isEmpty()){
						        	status.put("nmea", true);
						        }
						        
						        if(fotoObj.has("pointLat") && fotoObj.has("pointLng") && !fotoObj.get("pointLat").toString().equalsIgnoreCase("NaN") && !fotoObj.get("pointLng").toString().equalsIgnoreCase("NaN")){
						        	status.put("location", true);
						        }
						        
						        /* OpenCellID Call */
						        try {
						        	JSONObject cellInfoResponse = Utility.getNetworkCellData(fotoObj);
									if(cellInfoResponse != null) {
										daoMySQL.insertCellResponse(cellInfoResponse, idFotoNew);
										if(cellInfoResponse.has("lat") && cellInfoResponse.has("lon")) {
											Double distance = Utility.getDistanceFromCoordinates(cellInfoResponse.get("lat").toString(), cellInfoResponse.get("lon").toString(), fotoObj.get("pointLat").toString(), fotoObj.get("pointLng").toString());
											if(distance > 1000) {
												status.put("cell", false);
											}else {
												status.put("cell", true);
											}
											status.put("distance", distance);
											status.put("distance_unit", "m");
										}
									}
						        }catch(Exception e) {
						        	logger.error(">>>>>>>>>>Cannot update cell info",e);
						        }
						        
						        daoMySQL.updateStatus(status, idFotoNew);
						        
								logger.error(userComment);
							}
							catch(Exception e){
								logger.error("Error saving photo: "+Utility.stackTraceToString(e));
							}
			                
			                JSONObject jsonObjectOutput = new JSONObject();
			                JSONObject jsonObjectTamperingConsole = new JSONObject();
							jsonObjectOutput.put("date", Utility.getStringFromJSONField("date", fotoObj));
							jsonObjectOutput.put("idFotoNew", idFotoNew);
							jsonObjectOutput.put("ext", Utility.getStringFromJSONField("ext", fotoObj));
							
							jsonObjectTamperingConsole.put("base64Image", base64Image);
							jsonObjectTamperingConsole.put("user", Utility.getStringFromJSONField("user", fotoObj));
							jsonObjectTamperingConsole.put("ext", Utility.getStringFromJSONField("ext", fotoObj));
							
							jsonArrayOutput.put(jsonObjectOutput);
							jsonArrayTamperingConsole.put(jsonObjectTamperingConsole);
						}
						else{
							
							daoMySQL.cancellaIdFoto(idFoto);
						}
					}
					
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					JSONObject jsonObjRitorno=new JSONObject();
					
					if(jsonArrayOutput.length() > 0){
						
						try {
							jsonObjRitorno.put("arrayIdFoto", jsonArrayOutput);
							
						} catch (JSONException e1) {
							logger.error(Utility.stackTraceToString(e1));
						}
					}
					else{
						
						try {
							
							jsonObjRitorno.put("esitoOperazione", "OK");
							
						} catch (JSONException e1) {
							logger.error(Utility.stackTraceToString(e1));
						}
					}
					
					out.print(jsonObjRitorno);
					out.flush();
					out.close();
				}
			}
			else{
				throw new Exception("Photos missing.");
			}
			
        } catch (Exception e) {

        	response.reset();
        	logger.error(Utility.stackTraceToString(e));
        	response.setContentType("application/json");
			PrintWriter out = response.getWriter(); 
			JSONObject jsonObj=new JSONObject();
			
			try {
				jsonObj.put("errorMessage", Utility.stackTraceToString(e));
			} catch (JSONException e1) {
				logger.error(Utility.stackTraceToString(e1));
			}
			out.print(jsonObj);
			out.flush();
       }
	}
}


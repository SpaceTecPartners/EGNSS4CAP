package it.erasmicoin.utility;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.erasmicoin.configurazione.ApplicationConfig;

public class Utility {

	public static Logger logger = Logger.getLogger(Utility.class.getName());
	private static final String rootPath = ApplicationConfig.getFotoPath();
	private static final String openCellIdToken = ApplicationConfig.getOpenCellIDToken();
	
	public static JSONObject getNetworkCellData(JSONObject photoJson) throws Exception {
		
		JSONObject response = null;
		
		if(photoJson.has("network_info")) {
			JSONObject cellRequest = new JSONObject();
			JSONObject photoData = photoJson.getJSONObject("network_info");
			cellRequest.put("token", openCellIdToken);
			cellRequest.put("radio", photoData.get("radio"));
			cellRequest.put("mcc", photoData.get("mcc"));
			cellRequest.put("mnc", photoData.get("mnc"));
			JSONArray cells = new JSONArray();
			JSONObject cellObject = new JSONObject();
			cellObject.put("lac", photoData.get("lac"));
			cellObject.put("cid", photoData.get("cid"));
			cells.put(cellObject);
			cellRequest.put("cells",cells);
			cellRequest.put("address",1);
			
			if(photoData.has("wifi")) {
				JSONObject wifiPData = photoData.getJSONObject("wifi");
				
				JSONArray wifi = new JSONArray();
				JSONObject wifiObject = new JSONObject();
				wifiObject.put("bssid", wifiPData.get("bssid"));
				wifiObject.put("channel", wifiPData.get("channel"));
				wifiObject.put("frequency", wifiPData.get("frequency"));
				wifiObject.put("signal", wifiPData.get("signal"));
				
				wifi.put(wifiObject);
				cellRequest.put("wifi",wifi);
			}
			
			String resp = Utility.excutePost("https://eu1.unwiredlabs.com/v2/process.php", cellRequest.toString());
			response = new JSONObject(resp);
		}
		return response;
				
	}
	
	private static String excutePost(String targetURL, String urlParameters)
	  {
	    URL url;
	    HttpURLConnection connection = null;  
	    try {
	      //Create connection
	      url = new URL(targetURL);
	      connection = (HttpURLConnection)url.openConnection();
	      connection.setRequestMethod("POST");
	      connection.setRequestProperty("Content-Type","application/json; charset=utf-8");				
	      connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
	      connection.setRequestProperty("Content-Language", "en-US");  
				
	      connection.setUseCaches (false);
	      connection.setDoInput(true);
	      connection.setDoOutput(true);

	      //Send request
	      DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
	      wr.writeBytes (urlParameters);
	      wr.flush ();
	      wr.close ();

	      //Get Response	
	      InputStream is = connection.getInputStream();
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	      String line;
	      StringBuffer response = new StringBuffer(); 
	      while((line = rd.readLine()) != null) {
	        response.append(line);
	        response.append('\r');
	      }
	      rd.close();
	      return response.toString();

	    } catch (Exception e) {

	      e.printStackTrace();
	      return null;

	    } finally {

	      if(connection != null) {
	        connection.disconnect(); 
	      }
	    }
	  }
	
	public static String generateToken(String username, int tokenLength) throws JSONException {
		
		final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		SecureRandom rnd = new SecureRandom();
		
	    StringBuilder sb = new StringBuilder( tokenLength );
	    for( int i = 0; i < tokenLength; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
	    
	    return sb.toString();

	}
	
	private static void creaRootPath(){

		File path = new File(rootPath);
		
		if(!path.exists())
			path.mkdir();
	}
	
	public static String encodeFileToBase64Binary(String fileName)
			throws IOException {

		File file = new File(fileName);
		byte[] bytes = loadFile(file);
		byte[] encoded = Base64.encodeBase64(bytes);
		String encodedString = new String(encoded);

		return encodedString;
	}
	
	private static byte[] loadFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);

	    long length = file.length();
	    if (length > Integer.MAX_VALUE) {
	        // File is too large
	    }
	    byte[] bytes = new byte[(int)length];
	    
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }

	    if (offset < bytes.length) {
	    	is.close();
	        throw new IOException("Could not completely read file "+file.getName());
	    }

	    is.close();
	    return bytes;
	}

	public static String getFolderName(String pathFoto){
		
		String folderName = rootPath+pathFoto;
		
		return folderName;
	}

	
	public static void salvaImg(String fileName, String pathFoto, byte[] fileContentByteArray) throws Exception{
		
		creaRootPath();
		
		String folderName = getFolderName(pathFoto);

		Path path = Paths.get(folderName);
		
		if (path.toFile() == null || !path.toFile().exists()) {
		    Files.createDirectories(path);
        }
		
		Path pathImg = Paths.get(folderName+"/"+fileName);
		if(pathImg.toFile() == null || !pathImg.toFile().exists()){
			Files.write(pathImg, fileContentByteArray);
		}
	}	
	
	public static String stackTraceToString(Throwable e)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		e.printStackTrace(ps);
		ps.flush();
		String string = baos.toString();
		ps.close();
		return string;
	}
	
	public static String getStringFromJSONField(String campo, JSONObject jsonObj) throws Exception{
		
		String ret = null;
		
		try {
			
			if(jsonObj.has(campo) && !jsonObj.isNull(campo) && !String.valueOf(jsonObj.get(campo)).isEmpty())
				ret = String.valueOf(jsonObj.get(campo));
			
		} catch (Exception e) {
			ret = null;
		}
		
		return ret;
	}
	
	public static int getIntFromJSONField(String campo, JSONObject jsonObj) throws Exception{
		
		int ret = 0;
		
		try {
			
			if(jsonObj.has(campo) && !jsonObj.isNull(campo))
				ret = jsonObj.getInt(campo);
			
		} catch (Exception e) {
			ret = 0;
		}
		
		return ret;
	}
	
	public static double getDoubleFromJSONField(String campo, JSONObject jsonObj) throws Exception{
		
		double ret = 0;
		
		try {
			
			if(jsonObj.has(campo) && !jsonObj.isNull(campo))
				ret = jsonObj.getDouble(campo);
			
		} catch (Exception e) {
			ret = 0;
		}
		
		return ret;
	}
	
	public static Double getDistanceFromCoordinates(String coordALat, String coordALon, String coordBLat, String coordBLon) {
		
		Double latA = Double.parseDouble(coordALat);
		Double lonA = Double.parseDouble(coordALon);
		
		Double latB = Double.parseDouble(coordBLat);
		Double lonB = Double.parseDouble(coordBLon);
		
		Double constP = Math.PI/180;
		Double constR = new Double(12742);
		
		Double a = 0.5 - Math.cos((latB - latA) * constP)/2 + Math.cos(latA * constP) * Math.cos(latB * constP) * (1 - Math.cos((lonB - lonA) * constP))/2;
		
		Double distance = constR * Math.asin(Math.sqrt(a));
		return distance*1000;
	}
	
}
package it.erasmicoin.crypto.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import it.erasmicoin.dao.MetodiDAOMySQL;
import it.erasmicoin.utility.Utility;

/**
 * Implementazione della servlet DownloadFotoServlet
 */
public class GetPhotoGSAServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	public static Logger logger = Logger.getLogger(GetPhotoGSAServlet.class);		
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetPhotoGSAServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		this.doTask(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		this.doTask(request, response);
	}
	
	protected void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		
		JSONObject jsonOutput = new JSONObject();

		MetodiDAOMySQL daoMySQL = new MetodiDAOMySQL();
		
		try {

			String dataInput = request.getParameter("dataInput");
			JSONObject jsonObject = new JSONObject(dataInput);

			if(jsonObject.has("id")){
				
				String idFoto = Utility.getStringFromJSONField("id", jsonObject);
				if(idFoto != null && !idFoto.equals("")) {
					jsonOutput.put(idFoto, daoMySQL.getGSAPhoto(idFoto));
				}
					
			}
			response.setContentType("application/json; charset=utf-8");
			PrintWriter out = response.getWriter();
			out.print(jsonOutput);
			out.flush();
			out.close();
			
		} catch (Exception e) {

			response.reset();
        	logger.error(Utility.stackTraceToString(e));
        	response.setContentType("application/json; charset=utf-8");
			PrintWriter out = response.getWriter();
			JSONObject jsonObj=new JSONObject();
			
			try {
				jsonObj.put("errorMessage", Utility.stackTraceToString(e));
			} catch (JSONException e1) {
				logger.error(Utility.stackTraceToString(e1));
			}
			out.print(jsonObj);
			out.flush();
			out.close();
		}
	}
}

package it.erasmicoin.crypto.servlet;

import it.erasmicoin.dao.MetodiDAOMySQL;
import it.erasmicoin.utility.Utility;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class ListaFotoServlet
 */
public class PhotosListGSAServletAuth extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static Logger logger = Logger.getLogger(PhotosListGSAServletAuth.class);		
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public PhotosListGSAServletAuth() {
        super();
    }


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doTask(request, response);
	}
	
	protected void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		logger.debug("PhotosListGSAServletAuth");
		String dataInput = request.getParameter("dataInput");


		JSONObject jsonOutput = new JSONObject();
		
		MetodiDAOMySQL dao = new MetodiDAOMySQL();
				
		
		if(dataInput != null){
			try {
				JSONObject jsonInput = new JSONObject(dataInput);
				
				jsonOutput = dao.getGSAPhotoList(jsonInput);
				
				if(jsonOutput == null) {
					response.setStatus(500);
				}else {
					response.setContentType("application/json; charset=utf-8");
					PrintWriter out = response.getWriter();
					out.print(jsonOutput);
					out.flush();
					out.close();
				}				
			} catch (Exception e) {

				response.reset();
	        	logger.error(Utility.stackTraceToString(e));
	        	response.setContentType("application/json; charset=utf-8");
	        	response.setStatus(500);
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
}

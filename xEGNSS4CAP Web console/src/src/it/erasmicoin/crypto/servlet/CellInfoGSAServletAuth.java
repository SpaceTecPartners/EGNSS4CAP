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
 * Servlet implementation class ListaFotoServlet
 */
public class CellInfoGSAServletAuth extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static Logger logger = Logger.getLogger(CellInfoGSAServletAuth.class);		
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CellInfoGSAServletAuth() {
        super();
    }


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doTask(request, response);
	}
	
	protected void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		logger.debug("CellInfoGSAServletAuth");
		String dataInput = request.getParameter("dataInput");


		JSONObject jsonOutput = new JSONObject();
		
		MetodiDAOMySQL dao = new MetodiDAOMySQL();
				
		
		if(dataInput != null){
			try {
				JSONObject jsonInput = new JSONObject(dataInput);
				String idFoto = Utility.getStringFromJSONField("id", jsonInput);
				JSONObject photoJson = dao.getGSAPhoto(idFoto);
				
				jsonOutput = Utility.getNetworkCellData(photoJson);
				
				if(jsonOutput == null) {
					response.setStatus(500);
				}else {
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					out.print(jsonOutput);
					out.flush();
					out.close();
				}				
			} catch (Exception e) {

				response.reset();
	        	logger.error(Utility.stackTraceToString(e));
	        	response.setContentType("application/json");
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

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
public class UsersListGSAServletAuth extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static Logger logger = Logger.getLogger(UsersListGSAServletAuth.class);		
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public UsersListGSAServletAuth() {
        super();
        // TODO Auto-generated constructor stub
    }


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doTask(request, response);
	}
	
	protected void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		logger.debug("UsersListGSAServletAuth");

		JSONObject jsonOutput = new JSONObject();
		
		MetodiDAOMySQL dao = new MetodiDAOMySQL();
				
		try {
			String reqToken = request.getParameter("t");
			jsonOutput = dao.getUsers(reqToken);
			logger.debug("tornato "+jsonOutput.toString());
			
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.print(jsonOutput);
			out.flush();
			out.close();
							
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
				// TODO Auto-generated catch block
				logger.error(Utility.stackTraceToString(e1));
			}
			out.print(jsonObj);
			out.flush();
			out.close();
		}
	}
}

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

public class LogoutServletAuth extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1717264005426394058L;
	
	public static Logger logger = Logger.getLogger(LogoutServletAuth.class);
	
	public LogoutServletAuth() {
		super();
	}


	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		JSONObject jsonObj = new JSONObject();
		
		logger.debug("Logout servlet");
				
		String user = request.getParameter("u");
		
		MetodiDAOMySQL daoMySQL = new MetodiDAOMySQL();
		
		
		try {
			if(user == null || user.isEmpty()){
				jsonObj.put("result",false);
			}else {
				jsonObj = daoMySQL.logoutUser(user);
			}
			
		}catch(Exception e) {
			try {
				jsonObj.put("result", false);
				jsonObj.put("message", Utility.stackTraceToString(e));
			} catch (JSONException e1) {
				logger.error(Utility.stackTraceToString(e1));
			}
		}
				
    	response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		
		out.print(jsonObj);
		out.flush();
		out.close();
		
	}

	
}

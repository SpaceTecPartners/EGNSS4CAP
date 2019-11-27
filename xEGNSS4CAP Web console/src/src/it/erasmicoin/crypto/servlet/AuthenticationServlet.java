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

public class AuthenticationServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1717264005426394058L;
	
	private static final int tokenLength = 64;

	public static Logger logger = Logger.getLogger(AuthenticationServlet.class);
	
	public AuthenticationServlet() {
		super();
	}


	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		JSONObject jsonObj = new JSONObject();
		
		logger.debug("Auth servlet");
		
		String username = request.getParameter("u");
		String password = request.getParameter("p");
		String fromApp = request.getParameter("from_app");
		
		MetodiDAOMySQL daoMySQL = new MetodiDAOMySQL();
		
		logger.debug("fromapp>>>"+fromApp);
		try {
			if(username == null || password == null || username.isEmpty() || password.isEmpty()) {
				jsonObj.put("result",false);
			}else {
				jsonObj = daoMySQL.loginUser(username, password, tokenLength, fromApp);
				if(jsonObj.getBoolean("result")) {
					JSONObject userInfo = daoMySQL.getUserInfo(username);// isSuperUser(username);
					jsonObj.put("userInfo", userInfo);
					if(userInfo.getString("role").equalsIgnoreCase("SUPERUSER") || userInfo.getString("role").equalsIgnoreCase(MetodiDAOMySQL.USER_TYPE_PAYING_AGENCY)) {
						jsonObj.put("superUser", true);
					}
					logger.debug("user type = "+userInfo.getString("role"));
					if(fromApp != null && fromApp.trim().equals("1") && 
							(userInfo.getString("role").equalsIgnoreCase("SUPERUSER") || userInfo.getString("role").equalsIgnoreCase(MetodiDAOMySQL.USER_TYPE_PAYING_AGENCY))) {
						logger.debug("agency cannot login from app");
						jsonObj = new JSONObject();
						jsonObj.put("result",false);
					}
				}
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

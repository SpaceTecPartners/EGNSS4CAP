package it.erasmicoin.crypto.servlet;

import it.erasmicoin.dao.MetodiDAOMySQL;
import it.erasmicoin.utility.Utility;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class ListaFotoServlet
 */
public class UserActionsGSAServletAuth extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static Logger logger = Logger.getLogger(UserActionsGSAServletAuth.class);		
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public UserActionsGSAServletAuth() {
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
		
		logger.debug("UserActionsGSAServletAuth");

		JSONObject jsonOutput = new JSONObject();
		
		MetodiDAOMySQL dao = new MetodiDAOMySQL(); 
		
		// Print out every passed parameter
		Enumeration<String> params = request.getParameterNames(); 
		while(params.hasMoreElements()){
		 String paramName = params.nextElement();
		  logger.debug("Parameter Name - "+paramName+", Value - "+request.getParameter(paramName));
		}
		
		
		try {
			String action = request.getParameter("a");
			boolean actionStatus = false;
			int newUserId = 0;
			if(action.equalsIgnoreCase("create")) {
				logger.debug("action create");
				String username = request.getParameter("username");
				String password = request.getParameter("password");
				String role = request.getParameter("role");
				
				JSONObject currentUser = dao.getUserFromActiveToken(request.getParameter("t"));
				
				Long idParent = null;
				if(currentUser.getString("role").equalsIgnoreCase(MetodiDAOMySQL.USER_TYPE_PAYING_AGENCY)) {
					idParent = currentUser.getLong("id");
				}else if(currentUser.getString("role").equalsIgnoreCase(MetodiDAOMySQL.USER_TYPE_SUPERUSER) && role.equalsIgnoreCase(MetodiDAOMySQL.USER_TYPE_USER)) {
					idParent = Long.parseLong(request.getParameter("idp"));
				}
				newUserId = dao.createUser(username, password, role, idParent);
				if(newUserId > 0)
					actionStatus = true;
				
				jsonOutput.put("status", actionStatus);
				jsonOutput.put("id", newUserId);
			}else if(action.equalsIgnoreCase("update")) {
				logger.debug("action update");
				Long userId = Long.parseLong(request.getParameter("userid"));
				String password = request.getParameter("password");
				String role = request.getParameter("role");
				Long idSuper = request.getParameter("idSuper") != null ? Long.parseLong(request.getParameter("idSuper")) : null;
				logger.debug("action parameters "+userId+"-"+password+"-"+role+"-"+idSuper);
				actionStatus = dao.updateUser(userId, password, role, idSuper);
				jsonOutput.put("status", actionStatus);
			}else if(action.equalsIgnoreCase("suspend")) {
				logger.debug("action suspend");
				int userId = Integer.parseInt(request.getParameter("userid"));
				actionStatus = dao.suspendUser(userId);
				jsonOutput.put("status", actionStatus);
			}else if(action.equalsIgnoreCase("activate")) {
				logger.debug("action activate");
				int userId = Integer.parseInt(request.getParameter("userid"));
				actionStatus = dao.activateUser(userId);
				jsonOutput.put("status", actionStatus);
			}else {
				logger.debug("unknown action");
				response.setContentType("application/json");
	        	response.setStatus(500);
	        	return;
			}
			
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

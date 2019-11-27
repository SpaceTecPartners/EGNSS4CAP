package it.erasmicoin.crypto.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import it.erasmicoin.dao.MetodiDAOMySQL;
import it.erasmicoin.utility.Utility;

public class AuthTokenFilter implements Filter {
	
	public static Logger logger = Logger.getLogger(Utility.class.getName());
	private static final int tokenDurationInMsec = (60 * 60 * 24) * 1000;
	FilterConfig fc;
	@Override
	public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException, ServletException {
		

		HttpServletResponse httpResponse = (HttpServletResponse)response;
		
		logger.debug("AUTH TOKEN FILTER");
		String token = "";
		try {
			token = request.getParameter("t").toString();
			logger.debug("Token sent");
		}catch(Exception e) {
			logger.error("Token retrieval error",e);
			throw e;
		}
		
		
		MetodiDAOMySQL daoMySQL = new MetodiDAOMySQL();
		boolean validToken = false;
		
		try {
			logger.debug("IS TOKEN VALID");
			validToken = daoMySQL.checkToken(token, tokenDurationInMsec);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			logger.error("Sending 500",e1);
			httpResponse.setStatus(500);
			e1.printStackTrace();
		}
	    
		if(!validToken) {
			httpResponse.setStatus(403);
			return;
		}
	    
	    chain.doFilter(request, response);
	}
	
	@Override
	public void destroy() {
		this.fc=null;
		
	}

	@Override
	public void init(FilterConfig fc) throws ServletException {
		this.fc = fc;
		
	}

}

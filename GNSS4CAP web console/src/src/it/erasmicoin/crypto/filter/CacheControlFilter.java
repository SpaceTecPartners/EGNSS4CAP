package it.erasmicoin.crypto.filter;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CacheControlFilter implements Filter {

	final long CACHE_DURATION_IN_SECOND = 60 * 60 * 24 * 5; // 5 giorni
	final long   CACHE_DURATION_IN_MS = CACHE_DURATION_IN_SECOND  * 1000;
	FilterConfig fc;
	@Override
	public void doFilter(ServletRequest request,ServletResponse response,FilterChain chain) throws IOException, ServletException {
		long expiry = new Date().getTime() + CACHE_DURATION_IN_MS;

	    HttpServletResponse httpResponse = (HttpServletResponse)response;
	    for (Enumeration e=fc.getInitParameterNames(); e.hasMoreElements();) {
	    	
	    	String headerName = (String)e.nextElement();
	        httpResponse.setHeader(headerName, fc.getInitParameter(headerName));
	    }

	    httpResponse.setDateHeader("Expires", expiry);
	    
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

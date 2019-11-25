package it.erasmicoin.log4j;

import javax.servlet.ServletContext;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Log4jInit extends HttpServlet{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static private Logger logger = Logger.getLogger(Log4jInit.class.getName());

    public void init(){
    	
    	try{
            ServletContext context = getServletContext();
            java.net.URL url = context.getResource("/WEB-INF/classes/log4j.properties");
            PropertyConfigurator.configure(url);
            System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "error");
            logger.warn("Log init success....");
        }catch(Exception e){
            logger.error(e.getMessage());
        }
    }
}

package it.erasmicoin.configurazione;

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

public class ApplicationConfig implements ServletContextListener {
    private static final String ATTRIBUTE_NAME = "config";
   	private Properties config = new Properties();
    
    static Logger log = Logger.getLogger(ApplicationConfig.class.toString());
    
    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            config.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties"));
            event.getServletContext().setAttribute(ATTRIBUTE_NAME, this);
            
            ApplicationConfig config = ApplicationConfig.getInstance(event.getServletContext());
            ApplicationConfig.setDataSourceName1(config.getProperty("config.DataSourceName1"));  // data source 1
            ApplicationConfig.setFotoPath(config.getProperty("config.fotoPath"));
            ApplicationConfig.setOpenCellIDToken(config.getProperty("config.openCellIdToken"));
        } catch (Exception e) {
            
				try {
					throw e;
				} catch (Exception e1) {
					e1.printStackTrace();
				}	
        }   
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // NOOP.
    }

    public static ApplicationConfig getInstance(ServletContext context) {
        return (ApplicationConfig) context.getAttribute(ATTRIBUTE_NAME);
    }

    public String getProperty(String key) {
        return config.getProperty(key);
    }

	//DATA SOURCE
    private static String DataSourceName1;
    private static String FotoPath;
    private static String OpenCellIDToken;

	public static String getDataSourceName1() {return DataSourceName1;}
	private static void setDataSourceName1(String dataSourceName1) {DataSourceName1 = dataSourceName1;}

	public static String getFotoPath() {
		return FotoPath;
	}

	public static void setFotoPath(String _FotoPath) {
		ApplicationConfig.FotoPath = _FotoPath;
	}
	
	public static String getOpenCellIDToken() { return OpenCellIDToken;}
	
	public static void setOpenCellIDToken(String _token) {
		ApplicationConfig.OpenCellIDToken = _token;
	}
	
	
	
}

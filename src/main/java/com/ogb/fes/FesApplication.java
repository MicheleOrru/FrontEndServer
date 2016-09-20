package com.ogb.fes;




import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.ogb.fes.domainRepositories.UserRepository;
import com.ogb.fes.filesystem.FileManager;
import com.ogb.fes.ndn.NDNInsertManager;
import com.ogb.fes.ndn.NDNInsertResolver;
import com.ogb.fes.net.NetManager;
import com.ogb.fes.service.ContentService;
import com.ogb.fes.service.GeoJSONProcessor;
import com.ogb.fes.service.QueryService;

@SpringBootApplication
public class FesApplication {
	
	public NDNInsertManager uploadWatchDirDeamon = null;
	public NDNInsertResolver batchInsert          = null;
	
	
	@Autowired
	public Environment env;
	
	@Autowired 
	public UserRepository userRepo;
	
		
    public static void main(String[] args) 
    {
        SpringApplication.run(FesApplication.class, args);
    }
    
    
	@Bean CommandLineRunner init() 
    {
        return (String[] args) -> {
            FileManager.createUploadDir();
            FileManager.createConfigDir();
            new FileManager().createDefaultConfigFile();
            
            if (FileManager.checkConfigFile() == false)
            	throw new Exception("Unable lo find config file!");
            
            uploadWatchDirDeamon = NDNInsertManager.sharedInstance();
            
            batchInsert = NDNInsertResolver.sharedInstance();
            
            NetManager.AUC_URL = env.getProperty("fes.auc.url");
            QueryService.serverIP = env.getProperty("fes.nfd.ip");
            ContentService.serverIP = env.getProperty("fes.nfd.ip");
            GeoJSONProcessor.userRepo=userRepo;
        };
    }
	
	@Bean
	public EmbeddedServletContainerFactory servletContainer() {
		TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
		tomcat.addAdditionalTomcatConnectors(createStandardConnector());
		return tomcat;
	}

	private Connector createStandardConnector() {
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		connector.setPort(Integer.parseInt(env.getProperty("server-http.port")));
		return connector;
	}
}
 
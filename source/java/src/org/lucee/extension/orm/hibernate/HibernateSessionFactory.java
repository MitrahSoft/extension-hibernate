package org.lucee.extension.orm.hibernate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.filter.ResourceFilter;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.Component;
import lucee.runtime.InterfacePage;
import lucee.runtime.Mapping;
import lucee.runtime.Page;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.config.Config;
import lucee.runtime.db.DataSource;
import lucee.runtime.db.DatasourceConnection;
import lucee.runtime.exp.PageException;
import lucee.runtime.listener.ApplicationContext;
import lucee.runtime.orm.ORMConfiguration;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.util.TemplateUtil;

import org.hibernate.MappingException;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.lucee.extension.orm.hibernate.jdbc.ConnectionProviderImpl;
import org.lucee.extension.orm.hibernate.jdbc.ConnectionProviderProxy;
import org.w3c.dom.Document;


public class HibernateSessionFactory {

	public static final String HIBERNATE_3_PUBLIC_ID = "-//Hibernate/Hibernate Mapping DTD 3.0//EN";
	public static final String HIBERNATE_3_SYSTEM_ID = "http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd";
	public static final String HIBERNATE_3_DOCTYPE_DEFINITION = "<!DOCTYPE hibernate-mapping PUBLIC \""+HIBERNATE_3_PUBLIC_ID+"\" \""+HIBERNATE_3_SYSTEM_ID+"\">";
	

	public static Configuration createConfiguration(Log log,String mappings, DatasourceConnection dc, SessionFactoryData data) throws SQLException, IOException, PageException {
		/*
		 autogenmap
		 cacheconfig
		 cacheprovider
		 cfclocation
		 datasource
		 dbcreate
		 eventHandling
		 flushatrequestend
		 ormconfig
		 sqlscript
		 useDBForMapping
		 */ 
		
		ORMConfiguration ormConf = data.getORMConfiguration();
		
		// dialect
		DataSource ds = dc.getDatasource();
		String dialect=null;
		try	{
			if (Class.forName(ormConf.getDialect()) != null) {
				dialect = ormConf.getDialect();
			}
		}
		catch (Exception e) {
			// MZ: The dialect value could not be bound to a classname or instantiation causes an exception - ignore and use the default dialect entries
		}
		if (dialect == null) {
			dialect = Dialect.getDialect(ormConf.getDialect());
			if(Util.isEmpty(dialect)) dialect=Dialect.getDialect(ds);
		}
		if(Util.isEmpty(dialect))
			throw ExceptionUtil.createException(data,null,"A valid dialect definition inside the application event listener (Application.cfc)"
		+" is missing. The dialect cannot be determinated automatically",null);
		
		// Cache Provider
		String cacheProvider = ormConf.getCacheProvider();
		Class<? extends RegionFactory> regionFactory=null;

		if(Util.isEmpty(cacheProvider) || "EHCache".equalsIgnoreCase(cacheProvider)) {
			regionFactory=net.sf.ehcache.hibernate.EhCacheRegionFactory.class;
			// regionFactory=net.sf.ehcache.hibernate.SingletonEhCacheRegionFactory.class;
			// regionFactory=(Class<? extends RegionFactory>) CFMLEngineFactory.getInstance().getClassUtil().loadClass("net.sf.ehcache.hibernate.EhCacheRegionFactory");
			cacheProvider=regionFactory.getName();
		}
		else if("JBossCache".equalsIgnoreCase(cacheProvider)) 	cacheProvider="org.hibernate.cache.TreeCacheProvider";
		else if("HashTable".equalsIgnoreCase(cacheProvider)) 	cacheProvider="org.hibernate.cache.HashtableCacheProvider";
		else if("SwarmCache".equalsIgnoreCase(cacheProvider)) 	cacheProvider="org.hibernate.cache.SwarmCacheProvider";
		else if("OSCache".equalsIgnoreCase(cacheProvider)) 		cacheProvider="org.hibernate.cache.OSCacheProvider";

		Resource cacheConfig = ormConf.getCacheConfig();
		Configuration configuration = new Configuration();
		
		// ormConfig
		Resource conf = ormConf.getOrmConfig();
		if(conf!=null){
			try {
				Document doc = CommonUtil.toDocument(conf,null);
				configuration.configure(doc);
			} 
			catch (Throwable t) {
				if(t instanceof ThreadDeath) throw (ThreadDeath)t;
				log.log(Log.LEVEL_ERROR, "hibernate", t);
				
			}
		}
		
		try{
			configuration.addXML(mappings);
		}
		catch(MappingException me){
			throw ExceptionUtil.createException(data,null, me);
		}
		

		// make sure the connection provider has the DBUtil instance, this is a little bit of a mess but the only way to make our pool availbale without importing the railo core 
		// providing reference to connection pool here.
		
		
		configuration
        // Database connection settings
		.setProperty("lucee.datasource.name", ds.getName())// uded by custom connctionprovider
		.setProperty("lucee.datasource.id", ds.id())// uded by custom connctionprovider
		
    	.setProperty("hibernate.connection.driver_class", ds.getClassDefinition().getClassName())
        
    	.setProperty("hibernate.connection.url", ds.getDsnTranslated());
		if(!Util.isEmpty(ds.getUsername())) {
			configuration.setProperty("lucee.datasource.user", ds.getUsername());
			if(!Util.isEmpty(ds.getPassword()))
				configuration.setProperty("lucee.datasource.password", ds.getPassword());
		}
		
		
    	ConnectionProviderImpl.dataSources.put(ds.id(),ds);
    	
		ConnectionProviderProxy.provider=new ConnectionProviderImpl();
    	//.setProperty("hibernate.connection.release_mode", "after_transaction")
    	configuration.setProperty("hibernate.transaction.flush_before_completion", "false")
    	.setProperty("hibernate.transaction.auto_close_session", "false")
    	
    	// use Lucee connection pool to avoid dynamic-import:*
    	.setProperty(Environment.CONNECTION_PROVIDER, 
    			//UserSuppliedConnectionProviderImpl.class.getName()
    			ConnectionProviderImpl.class.getName()	
    			)
    	
    	// SQL dialect
    	.setProperty("hibernate.dialect", dialect)
    	// Enable Hibernate's current session context
    	.setProperty("hibernate.current_session_context_class", "thread")
    	
    	// Echo all executed SQL to stdout
    	.setProperty("hibernate.show_sql", CommonUtil.toString(ormConf.logSQL()))
    	.setProperty("hibernate.format_sql", CommonUtil.toString(ormConf.logSQL()))
    	// Specifies whether secondary caching should be enabled
    	.setProperty("hibernate.cache.use_second_level_cache", CommonUtil.toString(ormConf.secondaryCacheEnabled()))
		// Drop and re-create the database schema on startup
    	.setProperty("hibernate.exposeTransactionAwareSessionFactory", "false")
		//.setProperty("hibernate.hbm2ddl.auto", "create")
		.setProperty("hibernate.default_entity_mode", "dynamic-map");
		
		if(!Util.isEmpty(ormConf.getCatalog()))
			configuration.setProperty("hibernate.default_catalog", ormConf.getCatalog());
		if(!Util.isEmpty(ormConf.getSchema()))
			configuration.setProperty("hibernate.default_schema",ormConf.getSchema());
		
		try{
		if(ormConf.secondaryCacheEnabled()){
			if(cacheConfig!=null && cacheConfig.isFile())
				configuration.setProperty("hibernate.cache.provider_configuration_file_resource_path",cacheConfig.getAbsolutePath());
			if(regionFactory!=null || CFMLEngineFactory.getInstance().getClassUtil().isInstaneOf(cacheProvider, RegionFactory.class))
				configuration.setProperty("hibernate.cache.region.factory_class", cacheProvider);
			else
				configuration.setProperty("hibernate.cache.provider_class", cacheProvider);
			
			configuration.setProperty("hibernate.cache.use_query_cache", "true");
	    	
	    	//hibernate.cache.provider_class=org.hibernate.cache.EhCacheProvider
		}
		}
		catch(Throwable t){if(t instanceof ThreadDeath) throw (ThreadDeath)t;}
		
		schemaExport(log,configuration,dc,data);
		
		return configuration;
	}

	private static void schemaExport(Log log,Configuration configuration, DatasourceConnection dc, SessionFactoryData data) throws PageException, SQLException, IOException {
		ORMConfiguration ormConf = data.getORMConfiguration();
		
		if(ORMConfiguration.DBCREATE_NONE==ormConf.getDbCreate()) {
			return;
		}
		else if(ORMConfiguration.DBCREATE_DROP_CREATE==ormConf.getDbCreate()) {
			SchemaExport export = new SchemaExport(configuration);
			export.setHaltOnError(true);
	            
			export.execute(false,true,false,false);
            printError(log,data,export.getExceptions(),false);
            executeSQLScript(ormConf,dc);
		}
		else if(ORMConfiguration.DBCREATE_UPDATE==ormConf.getDbCreate()) {
			SchemaUpdate update = new SchemaUpdate(configuration);
            update.setHaltOnError(true);
            update.execute(false, true);
            printError(log,data,update.getExceptions(),false);
        }
	}

	private static void printError(Log log,SessionFactoryData data, List<Exception> exceptions,boolean throwException) throws PageException {
		if(exceptions==null || exceptions.size()==0) return;
		Iterator<Exception> it = exceptions.iterator();
        if(!throwException || exceptions.size()>1){
			while(it.hasNext()) {
				log.log( Log.LEVEL_ERROR, "hibernate", it.next());
	        } 
        }
        if(!throwException) return;
        
        it = exceptions.iterator();
        while(it.hasNext()) {
        	throw ExceptionUtil.createException(data,null,it.next());
        } 
	}

	private static void executeSQLScript(ORMConfiguration ormConf,DatasourceConnection dc) throws SQLException, IOException {
        Resource sqlScript = ormConf.getSqlScript();
        if(sqlScript!=null && sqlScript.isFile()) {
        	BufferedReader br = CommonUtil.toBufferedReader(sqlScript,(Charset)null);
        	String line;
            StringBuilder sql=new StringBuilder();
            String str;
            Statement stat = dc.getConnection().createStatement();
        	try{
	        	while((line=br.readLine())!=null){
	            	line=line.trim();
	            	if(line.startsWith("//") || line.startsWith("--")) continue;
	            	if(line.endsWith(";")){
	            		sql.append(line.substring(0,line.length()-1));
	            		str=sql.toString().trim();
	            		if(str.length()>0)stat.execute(str);
	            		sql=new StringBuilder();
	            	}
	            	else {
	            		sql.append(line).append(" ");
	            	}	
	            }
	        	str=sql.toString().trim();
        		if(str.length()>0){
        			stat.execute(str);
	            }
        	}
    		finally {
    			CFMLEngineFactory.getInstance().getDBUtil().closeSilent(stat);
    		}
        }
    }


	public static Map<Key,String> createMappings(ORMConfiguration ormConf, SessionFactoryData data) {
		Map<Key,String> mappings=new HashMap<Key,String>();
		Iterator<Entry<Key, Map<String, CFCInfo>>> it = data.getCFCs().entrySet().iterator();
		while(it.hasNext()){
			Entry<Key, Map<String, CFCInfo>> e = it.next();
			
			Set<String> done=new HashSet<String>();
			StringBuilder mapping=new StringBuilder();
			mapping.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			mapping.append(HIBERNATE_3_DOCTYPE_DEFINITION+"\n");
			mapping.append("<hibernate-mapping>\n");
			Iterator<Entry<String, CFCInfo>> _it = e.getValue().entrySet().iterator();
			Entry<String, CFCInfo> entry;
			while(_it.hasNext()){
				entry = _it.next();
				createMappings(ormConf,entry.getKey(),entry.getValue(),done,mapping,data);
				
			}
			mapping.append("</hibernate-mapping>");
			mappings.put(e.getKey(), mapping.toString());
		}
		return mappings;
	}

	private static void createMappings(ORMConfiguration ormConf, String key, CFCInfo value,Set<String> done,StringBuilder mappings, SessionFactoryData data) {
		if(done.contains(key)) return;
		CFCInfo v;
		String ext = value.getCFC().getExtends();
		if(!Util.isEmpty(ext)){
			try {
				Component base = data.getEntityByCFCName(ext, false);
				ext=HibernateCaster.getEntityName(base);
			} catch (Throwable t) {if(t instanceof ThreadDeath) throw (ThreadDeath)t;}
			
			
			ext=HibernateUtil.id(CommonUtil.last(ext, ".").trim());
			if(!done.contains(ext)) {
				v = data.getCFC(ext,null);
				if(v!=null)createMappings(ormConf, ext, v, done, mappings,data);
			}
		}
		
		mappings.append(value.getXML());
		done.add(key);
	}

	public static List<Component> loadComponents(PageContext pc,HibernateORMEngine engine, ORMConfiguration ormConf) throws PageException {
		CFMLEngine en = CFMLEngineFactory.getInstance();
		String[] ext=HibernateUtil.merge(en.getInfo().getCFMLComponentExtensions(), en.getInfo().getLuceeComponentExtensions());
		
		ResourceFilter filter = en.getResourceUtil().getExtensionResourceFilter(ext,true);
		List<Component> components=new ArrayList<Component>();
		loadComponents(pc,engine,components,ormConf.getCfcLocations(),filter,ormConf);
		return components;
	}
	
	private static void loadComponents(PageContext pc, HibernateORMEngine engine,List<Component> components,Resource[] reses,ResourceFilter filter,ORMConfiguration ormConf) throws PageException {
		Mapping[] mappings = createMappings(pc, reses);
		ApplicationContext ac=pc.getApplicationContext();
		Mapping[] existing = ac.getComponentMappings();
		if(existing==null) existing=new Mapping[0];
		try{
			Mapping[] tmp = new Mapping[existing.length+1];
			for(int i=1;i<tmp.length;i++){
				tmp[i]=existing[i-1];
			}
			ac.setComponentMappings(tmp);
			for(int i=0;i<reses.length;i++){
				if(reses[i]!=null && reses[i].isDirectory()){
					tmp[0] = mappings[i];
					ac.setComponentMappings(tmp);
					loadComponents(pc,engine,mappings[i],components,reses[i], filter,ormConf);
				}
			}
		}
		finally {
			ac.setComponentMappings(existing);
		}
	}
	
	private static void loadComponents(PageContext pc, HibernateORMEngine engine,Mapping cfclocation,List<Component> components,Resource res,ResourceFilter filter,ORMConfiguration ormConf) throws PageException {
		if(res==null) return;

		if(res.isDirectory()){
			Resource[] children = res.listResources(filter);
			
			// first load all files
			for(int i=0;i<children.length;i++){
				if(children[i].isFile())loadComponents(pc,engine,cfclocation,components,children[i], filter,ormConf);
			}
			
			// and then invoke subfiles
			for(int i=0;i<children.length;i++){
				if(children[i].isDirectory())loadComponents(pc,engine,cfclocation,components,children[i], filter,ormConf);
			}
		}
		else if(res.isFile()){
			if(!HibernateUtil.isApplicationName(pc,res.getName()))	{
				try {
					
					// MUST still a bad solution
					PageSource ps = pc.toPageSource(res,null);
					if(ps==null || ps.getComponentName().indexOf("..")!=-1) {
						PageSource ps2=null;
						Resource root = cfclocation.getPhysical();
		                String path = CFMLEngineFactory.getInstance().getResourceUtil().getPathToChild(res, root);
		                if(!Util.isEmpty(path,true)) {
		                	ps2=cfclocation.getPageSource(path);
		                }
		                if(ps2!=null)ps=ps2;
					}
					
					
					//Page p = ps.loadPage(pc.getConfig());
					String name=res.getName();
					name=HibernateUtil.removeExtension(name, name);
					
					TemplateUtil tu = CFMLEngineFactory.getInstance().getTemplateUtil();
					
					Page p = tu.loadPage(pc, ps,true);
					if(!(p instanceof InterfacePage)){
						Component cfc = tu.loadComponent(pc, p, name, true,true,false,true);
						if(cfc.isPersistent()){
							components.add(cfc);
						}
					}
				} 
				catch (PageException e) {
					if(!ormConf.skipCFCWithError())throw e;
					//e.printStackTrace();
				}
			}
		}
	}

	
	public static Mapping[] createMappings(PageContext pc,Resource[] resources) {
			
			Mapping[] mappings=new Mapping[resources.length];
			Config config=pc.getConfig();
			for(int i=0;i<mappings.length;i++) {
				mappings[i]=CommonUtil.createMapping(config,
						"/",
						resources[i].getAbsolutePath()
						);
			}
			return mappings;
		}
}

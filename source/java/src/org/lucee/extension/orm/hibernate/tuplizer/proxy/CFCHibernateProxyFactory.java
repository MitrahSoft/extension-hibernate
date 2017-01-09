package org.lucee.extension.orm.hibernate.tuplizer.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.CompositeType;


public class CFCHibernateProxyFactory implements ProxyFactory {
	private String entityName;
	private String nodeName;

	/*public void postInstantiate(
		final String entityName, 
		final Class persistentClass,
		final Set interfaces, 
		final Method getIdentifierMethod,
		final Method setIdentifierMethod,
		AbstractComponentType componentIdType) throws HibernateException {
		int index=entityName.indexOf('.');
		this.nodeName = entityName;
		this.entityName = entityName.substring(index+1);
	}*/
	


	@Override
	public void postInstantiate(
			String entityName, 
			Class persistentClass, Set interfaces, Method getIdentifierMethod,
			Method setIdentifierMethod, CompositeType compositeType) throws HibernateException {
		int index=entityName.indexOf('.');
		this.nodeName = entityName;
		this.entityName = entityName.substring(index+1);
		
	}
	
	

	public void postInstantiate(PersistentClass pc) {
		this.nodeName =pc.getNodeName();
		this.entityName =pc.getEntityName();
	}

	@Override
	public HibernateProxy getProxy(final Serializable id,  final SessionImplementor session) {
		try {
			return new CFCHibernateProxy(new CFCLazyInitializer(entityName, id, session));
		}
		catch(Throwable t){
			if(t instanceof ThreadDeath) throw (ThreadDeath)t;
			return new CFCHibernateProxy(new CFCLazyInitializer(nodeName, id, session));
		}
	}
}
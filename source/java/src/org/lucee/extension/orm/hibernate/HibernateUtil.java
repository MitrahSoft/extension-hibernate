package org.lucee.extension.orm.hibernate;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import lucee.Info;
import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.Component;
import lucee.runtime.PageContext;
import lucee.runtime.PageSource;
import lucee.runtime.component.Property;
import lucee.runtime.config.Config;
import lucee.runtime.db.DatasourceConnection;
import lucee.runtime.exp.PageException;
import lucee.runtime.orm.ORMConfiguration;
import lucee.runtime.orm.ORMSession;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Struct;

public class HibernateUtil {

	public static final short FIELDTYPE_ID = 0;
	public static final short FIELDTYPE_COLUMN = 1;
	public static final short FIELDTYPE_TIMESTAMP = 2;
	public static final short FIELDTYPE_RELATION = 4;
	public static final short FIELDTYPE_VERSION = 8;
	public static final short FIELDTYPE_COLLECTION = 16;

	private static final String[] KEYWORDS = new String[] { "absolute", "access", "accessible", "action", "add", "after", "alias", "all", "allocate", "allow", "alter", "analyze",
			"and", "any", "application", "are", "array", "as", "asc", "asensitive", "assertion", "associate", "asutime", "asymmetric", "at", "atomic", "audit", "authorization",
			"aux", "auxiliary", "avg", "backup", "before", "begin", "between", "bigint", "binary", "bit", "bit_length", "blob", "boolean", "both", "breadth", "break", "browse",
			"bufferpool", "bulk", "by", "cache", "call", "called", "capture", "cardinality", "cascade", "cascaded", "case", "cast", "catalog", "ccsid", "change", "char",
			"char_length", "character", "character_length", "check", "checkpoint", "clob", "close", "cluster", "clustered", "coalesce", "collate", "collation", "collection",
			"collid", "column", "comment", "commit", "compress", "compute", "concat", "condition", "connect", "connection", "constraint", "constraints", "constructor", "contains",
			"containstable", "continue", "convert", "corresponding", "count", "count_big", "create", "cross", "cube", "current", "current_date", "current_default_transform_group",
			"current_lc_ctype", "current_path", "current_role", "current_server", "current_time", "current_timestamp", "current_timezone", "current_transform_group_for_type",
			"current_user", "cursor", "cycle", "data", "database", "databases", "date", "day", "day_hour", "day_microsecond", "day_minute", "day_second", "days", "db2general",
			"db2genrl", "db2sql", "dbcc", "dbinfo", "deallocate", "dec", "decimal", "declare", "default", "defaults", "deferrable", "deferred", "delayed", "delete", "deny",
			"depth", "deref", "desc", "describe", "descriptor", "deterministic", "diagnostics", "disallow", "disconnect", "disk", "distinct", "distinctrow", "distributed", "div",
			"do", "domain", "double", "drop", "dsnhattr", "dssize", "dual", "dummy", "dump", "dynamic", "each", "editproc", "else", "elseif", "enclosed", "encoding", "end",
			"end-exec", "end-exec1", "endexec", "equals", "erase", "errlvl", "escape", "escaped", "except", "exception", "excluding", "exclusive", "exec", "execute", "exists",
			"exit", "explain", "external", "extract", "false", "fenced", "fetch", "fieldproc", "file", "fillfactor", "filter", "final", "first", "float", "float4", "float8", "for",
			"force", "foreign", "found", "free", "freetext", "freetexttable", "from", "full", "fulltext", "function", "general", "generated", "get", "get_current_connection",
			"global", "go", "goto", "grant", "graphic", "group", "grouping", "handler", "having", "high_priority", "hold", "holdlock", "hour", "hour_microsecond", "hour_minute",
			"hour_second", "hours", "identified", "identity", "identity_insert", "identitycol", "if", "ignore", "immediate", "in", "including", "increment", "index", "indicator",
			"infile", "inherit", "initial", "initially", "inner", "inout", "input", "insensitive", "insert", "int", "int1", "int2", "int3", "int4", "int8", "integer", "integrity",
			"intersect", "interval", "into", "is", "isobid", "isolation", "iterate", "jar", "java", "join", "key", "keys", "kill", "language", "large", "last", "lateral",
			"leading", "leave", "left", "level", "like", "limit", "linear", "lineno", "lines", "linktype", "load", "local", "locale", "localtime", "localtimestamp", "locator",
			"locators", "lock", "lockmax", "locksize", "long", "longblob", "longint", "longtext", "loop", "low_priority", "lower", "ltrim", "map", "master_ssl_verify_server_cert",
			"match", "max", "maxextents", "maxvalue", "mediumblob", "mediumint", "mediumtext", "method", "microsecond", "microseconds", "middleint", "min", "minus", "minute",
			"minute_microsecond", "minute_second", "minutes", "minvalue", "mlslabel", "mod", "mode", "modifies", "modify", "module", "month", "months", "names", "national",
			"natural", "nchar", "nclob", "new", "new_table", "next", "no", "no_write_to_binlog", "noaudit", "nocache", "nocheck", "nocompress", "nocycle", "nodename", "nodenumber",
			"nomaxvalue", "nominvalue", "nonclustered", "none", "noorder", "not", "nowait", "null", "nullif", "nulls", "number", "numeric", "numparts", "nvarchar", "obid",
			"object", "octet_length", "of", "off", "offline", "offsets", "old", "old_table", "on", "online", "only", "open", "opendatasource", "openquery", "openrowset", "openxml",
			"optimization", "optimize", "option", "optionally", "or", "order", "ordinality", "out", "outer", "outfile", "output", "over", "overlaps", "overriding", "package",
			"pad", "parameter", "part", "partial", "partition", "path", "pctfree", "percent", "piecesize", "plan", "position", "precision", "prepare", "preserve", "primary",
			"print", "prior", "priqty", "privileges", "proc", "procedure", "program", "psid", "public", "purge", "queryno", "raiserror", "range", "raw", "read", "read_write",
			"reads", "readtext", "real", "reconfigure", "recovery", "recursive", "ref", "references", "referencing", "regexp", "relative", "release", "rename", "repeat", "replace",
			"replication", "require", "resignal", "resource", "restart", "restore", "restrict", "result", "result_set_locator", "return", "returns", "revoke", "right", "rlike",
			"role", "rollback", "rollup", "routine", "row", "rowcount", "rowguidcol", "rowid", "rownum", "rows", "rrn", "rtrim", "rule", "run", "runtimestatistics", "save",
			"savepoint", "schema", "schemas", "scope", "scratchpad", "scroll", "search", "second", "second_microsecond", "seconds", "secqty", "section", "security", "select",
			"sensitive", "separator", "session", "session_user", "set", "sets", "setuser", "share", "show", "shutdown", "signal", "similar", "simple", "size", "smallint", "some",
			"source", "space", "spatial", "specific", "specifictype", "sql", "sql_big_result", "sql_calc_found_rows", "sql_small_result", "sqlcode", "sqlerror", "sqlexception",
			"sqlid", "sqlstate", "sqlwarning", "ssl", "standard", "start", "starting", "state", "static", "statistics", "stay", "stogroup", "stores", "straight_join", "style",
			"subpages", "substr", "substring", "successful", "sum", "symmetric", "synonym", "sysdate", "sysfun", "sysibm", "sysproc", "system", "system_user", "table",
			"tablespace", "temporary", "terminated", "textsize", "then", "time", "timestamp", "timezone_hour", "timezone_minute", "tinyblob", "tinyint", "tinytext", "to", "top",
			"trailing", "tran", "transaction", "translate", "translation", "treat", "trigger", "trim", "true", "truncate", "tsequal", "type", "uid", "under", "undo", "union",
			"unique", "unknown", "unlock", "unnest", "unsigned", "until", "update", "updatetext", "upper", "usage", "use", "user", "using", "utc_date", "utc_time", "utc_timestamp",
			"validate", "validproc", "value", "values", "varbinary", "varchar", "varchar2", "varcharacter", "variable", "variant", "varying", "vcat", "view", "volumes", "waitfor",
			"when", "whenever", "where", "while", "window", "with", "within", "without", "wlm", "work", "write", "writetext", "xor", "year", "year_month", "zerofill", "zone" };
	private static final Set<String> keywords = new HashSet<String>();
	static {
		for (int i = 0; i < KEYWORDS.length; i++) {
			keywords.add(KEYWORDS[i]);
		}
	}

	public static boolean isKeyword(String word) {
		if (word == null) return false;
		return keywords.contains(word.trim().toLowerCase());
	}

	public static Type getPropertyType(ClassMetadata metaData, String name) throws HibernateException {
		try {
			return metaData.getPropertyType(name);
		}
		catch (HibernateException he) {
			if (name.equalsIgnoreCase(metaData.getIdentifierPropertyName())) return metaData.getIdentifierType();

			String[] names = metaData.getPropertyNames();
			for (int i = 0; i < names.length; i++) {
				if (names[i].equalsIgnoreCase(name)) return metaData.getPropertyType(names[i]);
			}
			throw he;
		}
	}

	public static Type getPropertyType(ClassMetadata metaData, String name, Type defaultValue) {
		try {
			return metaData.getPropertyType(name);
		}
		catch (HibernateException he) {
			if (name.equalsIgnoreCase(metaData.getIdentifierPropertyName())) return metaData.getIdentifierType();

			String[] names = metaData.getPropertyNames();
			for (int i = 0; i < names.length; i++) {
				if (names[i].equalsIgnoreCase(name)) return metaData.getPropertyType(names[i]);
			}
			return defaultValue;
		}
	}

	public static String validateColumnName(ClassMetadata metaData, String name) throws PageException {
		String res = validateColumnName(metaData, name, null);
		if (res != null) return res;
		throw ExceptionUtil.createException((ORMSession) null, null, "invalid name, there is no property with name [" + name + "] in the entity [" + metaData.getEntityName() + "]",
				"valid properties names are [" + CommonUtil.toList(metaData.getPropertyNames(), ", ") + "]");

	}

	public static String validateColumnName(ClassMetadata metaData, String name, String defaultValue) {
		Type type = metaData.getIdentifierType();
		// composite id
		if (type.isComponentType()) {
			String res = _validateColumnName(((ComponentType) type).getPropertyNames(), name);
			if (res != null) return res;
		}
		// regular id
		String id = metaData.getIdentifierPropertyName();
		if (id != null && name.equalsIgnoreCase(id)) return metaData.getIdentifierPropertyName();

		String res = _validateColumnName(metaData.getPropertyNames(), name);
		if (res != null) return res;
		return defaultValue;
	}

	private static String _validateColumnName(String[] names, String name) {
		if (names == null) return null;
		for (int i = 0; i < names.length; i++) {
			if (names[i].equalsIgnoreCase(name)) return names[i];
		}
		return null;
	}

	//

	public static Property[] createPropertiesFromTable(DatasourceConnection dc, String tableName) {
		Struct properties = CommonUtil.createStruct();
		try {
			DatabaseMetaData md = dc.getConnection().getMetaData();
			String dbName = CFMLEngineFactory.getInstance().getDBUtil().getDatabaseName(dc);
			Collection.Key name;

			// get all columns
			ResultSet res = md.getColumns(dbName, null, tableName, null);
			while (res.next()) {
				name = CommonUtil.createKey(res.getString("COLUMN_NAME"));
				properties.setEL(name, CommonUtil.createProperty(name.getString(), res.getString("TYPE_NAME")));
			}

			// ids
			res = md.getPrimaryKeys(null, null, tableName);
			Property p;
			while (res.next()) {
				name = CommonUtil.createKey(res.getString("COLUMN_NAME"));
				p = (Property) properties.get(name, null);
				if (p != null) p.getDynamicAttributes().setEL(CommonUtil.FIELDTYPE, "id");
			}

			// MUST foreign-key relation

		}
		catch (Throwable t) {
			if (t instanceof ThreadDeath) throw (ThreadDeath) t;
			return new Property[0];
		}

		Iterator<Object> it = properties.valueIterator();
		Property[] rtn = new Property[properties.size()];
		for (int i = 0; i < rtn.length; i++) {
			rtn[i] = (Property) it.next();
		}

		return rtn;
	}

	public static Property[] getProperties(Component component, int fieldType, Property[] defaultValue) {
		Property[] props = component.getProperties(true, false, false, false);
		java.util.List<Property> rtn = new ArrayList<Property>();

		if (props != null) {
			for (int i = 0; i < props.length; i++) {
				if (fieldType == getFieldType(props[i], FIELDTYPE_COLUMN)) rtn.add(props[i]);
			}
		}
		return rtn.toArray(new Property[rtn.size()]);
	}

	private static int getFieldType(Property property, int defaultValue) {
		return getFieldType(CommonUtil.toString(property.getDynamicAttributes().get(CommonUtil.FIELDTYPE, null), null), defaultValue);

	}

	private static int getFieldType(String fieldType, int defaultValue) {
		if (Util.isEmpty(fieldType, true)) return defaultValue;
		fieldType = fieldType.trim().toLowerCase();

		if ("id".equals(fieldType)) return FIELDTYPE_ID;
		if ("column".equals(fieldType)) return FIELDTYPE_COLUMN;
		if ("timestamp".equals(fieldType)) return FIELDTYPE_TIMESTAMP;
		if ("relation".equals(fieldType)) return FIELDTYPE_RELATION;
		if ("version".equals(fieldType)) return FIELDTYPE_VERSION;
		if ("collection".equals(fieldType)) return FIELDTYPE_COLLECTION;
		return defaultValue;
	}

	public static String convertTableName(SessionFactoryData data, String tableName) throws PageException {
		if (tableName == null) return null;
		return data.getNamingStrategy().convertTableName(tableName);
	}

	public static String convertColumnName(SessionFactoryData data, String columnName) throws PageException {
		if (columnName == null) return null;
		return data.getNamingStrategy().convertColumnName(columnName);
	}

	public static boolean isEntity(ORMConfiguration ormConf, Component cfc, String cfcName, String name) {
		if (!Util.isEmpty(cfcName)) {
			if (cfc.equalTo(cfcName)) return true;

			if (cfcName.indexOf('.') != -1) {
				Info info = CFMLEngineFactory.getInstance().getInfo();
				String[] extensions = HibernateUtil.merge(info.getCFMLComponentExtensions(), info.getLuceeComponentExtensions());
				String prefix = cfcName.replace('.', '/') + ".";
				Resource[] locations = ormConf.getCfcLocations();
				Resource res;
				for (int i = 0; i < locations.length; i++) {
					for (int y = 0; y < extensions.length; y++) {
						res = locations[i].getRealResource(prefix + extensions[y]);
						if (res.equals(cfc.getPageSource().getResource())) return true;
					}
				}
				return false;
			}
		}

		if (cfc.equalTo(name)) return true;
		return name.equalsIgnoreCase(HibernateCaster.getEntityName(cfc));
	}

	public static String id(String id) {
		return id.toLowerCase().trim();
	}

	public static Struct checkTable(DatasourceConnection dc, String tableName, SessionFactoryData data) throws PageException {

		try {
			String dbName = CFMLEngineFactory.getInstance().getDBUtil().getDatabaseName(dc);
			DatabaseMetaData md = dc.getConnection().getMetaData();
			Struct rows = checkTableFill(md, dbName, tableName);
			if (rows.size() == 0) {
				String tableName2 = checkTableValidate(md, dbName, tableName);
				if (tableName2 != null) rows = checkTableFill(md, dbName, tableName2);
			}

			if (rows.size() == 0) {
				return null;
			}
			return rows;
		}
		catch (SQLException e) {
			throw CommonUtil.toPageException(e);
		}
	}

	private static Struct checkTableFill(DatabaseMetaData md, String dbName, String tableName) throws SQLException, PageException {
		Struct rows = CFMLEngineFactory.getInstance().getCreationUtil().createCastableStruct(tableName, Struct.TYPE_LINKED);
		ResultSet columns = md.getColumns(dbName, null, tableName, null);
		try {
			String name;
			Object nullable;
			while (columns.next()) {
				name = columns.getString("COLUMN_NAME");

				nullable = columns.getObject("IS_NULLABLE");
				rows.setEL(CommonUtil.createKey(name),
						new ColumnInfo(name, columns.getInt("DATA_TYPE"), columns.getString("TYPE_NAME"), columns.getInt("COLUMN_SIZE"), CommonUtil.toBooleanValue(nullable)));
			}
		}
		finally {
			CommonUtil.closeEL(columns);
		} // Table susid defined for cfc susid does not exist.

		return rows;
	}

	private static String checkTableValidate(DatabaseMetaData md, String dbName, String tableName) {

		ResultSet tables = null;
		try {
			tables = md.getTables(dbName, null, null, null);
			String name;
			while (tables.next()) {
				name = tables.getString("TABLE_NAME");
				if (name.equalsIgnoreCase(tableName) && tables.getString("TABLE_TYPE").toUpperCase().indexOf("SYSTEM") == -1) return name;
			}
		}
		catch (Throwable t) {
			if (t instanceof ThreadDeath) throw (ThreadDeath) t;
		}
		finally {
			CommonUtil.closeEL(tables);
		}
		return null;

	}

	public static HibernateORMEngine getORMEngine(PageContext pc) throws PageException {
		if (pc == null) pc = CommonUtil.pc();
		Config config = pc.getConfig();
		return (HibernateORMEngine) config.getORMEngine(pc);// TODO add this method to the public interface
	}

	public static HibernateORMSession getORMSession(PageContext pc, boolean create) throws PageException {
		return (HibernateORMSession) pc.getORMSession(create);// TODO add this method to the public interface
	}

	public static Property[] getIDProperties(Component c, boolean onlyPeristent, boolean includeBaseProperties) {
		Property[] props = CommonUtil.getProperties(c, onlyPeristent, includeBaseProperties, false, false);
		java.util.List<Property> tmp = new ArrayList<Property>();
		for (int i = 0; i < props.length; i++) {
			if ("id".equalsIgnoreCase(CommonUtil.toString(props[i].getDynamicAttributes().get(CommonUtil.FIELDTYPE, null), ""))) tmp.add(props[i]);
		}
		return tmp.toArray(new Property[tmp.size()]);
	}

	public static long getCompileTime(PageContext pc, PageSource ps) throws PageException {
		return CFMLEngineFactory.getInstance().getTemplateUtil().getCompileTime(pc, ps);
	}

	public static String removeExtension(String filename, String defaultValue) {
		int index = filename.lastIndexOf('.');
		if (index == -1) return defaultValue;
		return filename.substring(0, index);
	}

	public static String[] merge(String[] arr1, String[] arr2) {
		String[] ret = new String[arr1.length + arr2.length];
		for (int i = 0; i < arr1.length; i++) {
			ret[i] = arr1[i];
		}
		for (int i = 0; i < arr2.length; i++) {
			ret[arr1.length + i] = arr2[i];
		}
		return ret;
	}

	public static boolean isApplicationName(PageContext pc, String name) {
		String lcn = name.toLowerCase();
		if (!lcn.startsWith("application.")) return false;

		Info info = CFMLEngineFactory.getInstance().getInfo();
		String[] extensions = pc.getRequestDialect() == CFMLEngine.DIALECT_CFML ? info.getCFMLComponentExtensions() : info.getLuceeComponentExtensions();

		for (int i = 0; i < extensions.length; i++) {
			if (lcn.equalsIgnoreCase("application." + extensions[i])) return true;
		}
		return false;
	}
}

package com.x.server.console.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.x.base.core.project.Dependency;
import com.x.base.core.project.Deployable;
import com.x.base.core.project.x_base_core_project;
import com.x.base.core.project.config.Config;
import com.x.base.core.project.tools.DefaultCharset;

import io.github.classgraph.ClassInfo;

public abstract class JettySeverTools {

	protected static final String PATH_COMMONS = "commons";
	protected static final String PATH_COMMONS_EXT = "commons/ext";
	protected static final String PATH_STORE = "store";
	protected static final String PATH_STORE_JARS = "store/jars";
	protected static final String PATH_CUSTOM = "custom";
	protected static final String PATH_CUSTOM_JARS = "custom/jars";

	protected static void addHttpsConnector(Server server, Integer port) throws Exception {
		SslContextFactory sslContextFactory = new SslContextFactory();
		// File file = new File(configDir, "o2.keystore");
		// if (!file.exists() || file.isDirectory()) {
		// file = new File(new File(configDir, "sample"), "o2.keystore");
		// }
		sslContextFactory.setKeyStorePath(Config.sslKeyStore().getAbsolutePath());
		sslContextFactory.setKeyStorePassword(Config.token().getSslKeyStorePassword());
		sslContextFactory.setKeyManagerPassword(Config.token().getSslKeyManagerPassword());
		sslContextFactory.setTrustAll(true);
		// sslContextFactory.setTrustStorePath(Config.sslKeyStore().getAbsolutePath());
		// sslContextFactory.setTrustStorePassword(Config.token().getTrustStorePassword());
		HttpConfiguration config = new HttpConfiguration();
		config.setSecureScheme("https");
		config.setOutputBufferSize(32768);
		config.setRequestHeaderSize(8192);
		config.setResponseHeaderSize(8192);
		config.setSendServerVersion(true);
		config.setSendDateHeader(false);
		ServerConnector sslConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(config));
		sslConnector.setPort(port);
		server.addConnector(sslConnector);
	}

	protected static void addHttpConnector(Server server, Integer port) throws Exception {
		HttpConfiguration config = new HttpConfiguration();
		config.setOutputBufferSize(32768);
		config.setRequestHeaderSize(8192);
		config.setResponseHeaderSize(8192);
		config.setSendServerVersion(true);
		config.setSendDateHeader(false);
		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(config));
		http.setIdleTimeout(30000);
		http.setPort(port);
		server.addConnector(http);
	}

	protected static void cleanDirectory(File dir) throws Exception {
		FileUtils.forceMkdir(dir);
		FileUtils.cleanDirectory(dir);
	}

//	protected static void createDeployDescriptor(Class<?> clazz, File webappsDir, File workDir, File storeDir,
//			File extDir, File jarsDir) throws Exception {
//		StringBuffer buffer = new StringBuffer();
//		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
//		if (Config.currentNode().getQuickStartWebApp()) {
//			buffer.append("<Configure class=\"org.eclipse.jetty.quickstart.QuickStartWebApp\">");
//			buffer.append("<Set name=\"autoPreconfigure\">true</Set>");
//			/*
//			 * 使用org.eclipse.jetty.quickstart.QuickStartWebApp 启动速度大约快50% 需要设置
//			 * autoPreconfigure=true
//			 */
//		} else {
//			buffer.append("<Configure class=\"org.eclipse.jetty.webapp.WebAppContext\">");
//		}
//		buffer.append("<Set name=\"contextPath\">/" + clazz.getSimpleName() + "</Set>");
//		File war = new File(storeDir, clazz.getSimpleName() + ".war");
//		buffer.append("<Set name=\"war\">" + war.getAbsolutePath() + "</Set>");
//		String extraClasspath = calculateExtraClassPath(clazz, extDir, jarsDir);
//		buffer.append("	<Set name=\"extraClasspath\">" + extraClasspath + "</Set>");
//		String tempDirectory = new File(workDir, clazz.getSimpleName()).getAbsolutePath();
//		buffer.append("	<Set name=\"tempDirectory\">" + tempDirectory + "</Set>");
//		buffer.append("	</Configure>");
//		File file = new File(webappsDir, clazz.getSimpleName() + ".xml");
//		FileUtils.write(file, buffer.toString(), DefaultCharset.charset);
//	}

	protected static void createOfficialDeployDescriptor(ClassInfo info) throws Exception {
		StringBuffer buffer = new StringBuffer();
		Class<?> cls = Class.forName(info.getName());
		buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		if (Config.currentNode().getQuickStartWebApp()) {
			buffer.append("<Configure class=\"org.eclipse.jetty.quickstart.QuickStartWebApp\">");
			buffer.append("<Set name=\"autoPreconfigure\">true</Set>");
		} else {
			buffer.append("<Configure class=\"org.eclipse.jetty.webapp.WebAppContext\">");
		}
		buffer.append("<Set name=\"contextPath\">/" + info.getSimpleName() + "</Set>");
		File war = new File(Config.dir_store(), info.getSimpleName() + ".war");
		buffer.append("<Set name=\"war\">" + war.getAbsolutePath() + "</Set>");
		String extraClasspath = calculateExtraClassPath(cls);
		buffer.append("<Set name=\"extraClasspath\">" + extraClasspath + "</Set>");
		String tempDirectory = new File(Config.dir_servers_applicationServer_work(), info.getSimpleName())
				.getAbsolutePath();
		buffer.append("<Set name=\"tempDirectory\">" + tempDirectory + "</Set>");
		buffer.append("</Configure>");
		File file = new File(Config.dir_servers_applicationServer_webapps(), info.getSimpleName() + ".xml");
		FileUtils.write(file, buffer.toString(), DefaultCharset.charset);
	}

	protected static String calculateExtraClassPath(Class<?> cls) throws Exception {
		List<String> jars = new ArrayList<>();
		jars.addAll(calculateExtraClassPathDefault());
		Deployable deployable = (Deployable) cls.newInstance();
		Dependency dependency = deployable.dependency();
		for (String str : dependency.storeJars) {
			File file = new File(Config.dir_store_jars(), str + ".jar");
			if (file.exists()) {
				jars.add(file.getAbsolutePath());
			}
		}
		for (String str : dependency.customJars) {
			File file = new File(Config.dir_custom_jars(), str + ".jar");
			if (file.exists()) {
				jars.add(file.getAbsolutePath());
			}
		}
		for (String str : dependency.dynamicJars) {
			File file = new File(Config.dir_dynamic_jars(), str + ".jar");
			if (file.exists()) {
				jars.add(file.getAbsolutePath());
			}
		}
		return StringUtils.join(jars, ";");
	}

//	private static String calculateExtraClassPath(ClassInfo classInfo, File commons_ext_dir, File store_jars_dir,
//			File custom_jars_dir) throws Exception {
//		List<String> jars = new ArrayList<>();
//		jars.addAll(calculateExtraClassPathDefault(commons_ext_dir, store_jars_dir));
//		jars.addAll(calculateExtraClassPathDependency(classInfo, commons_ext_dir, store_jars_dir, custom_jars_dir));
//		return StringUtils.join(jars, ";");
//	}

//	protected static List<String> calculateExtraClassPathDefault(File commons_ext_dir, File store_jars_dir) {
//		List<String> jars = new ArrayList<>();
//		IOFileFilter filter = new WildcardFileFilter(x_base_core_project.class.getSimpleName() + "*.jar");
//		for (File o : FileUtils.listFiles(store_jars_dir, filter, null)) {
//			jars.add(o.getAbsolutePath());
//		}
//		filter = new WildcardFileFilter("slf4j-api-*.jar");
//		filter = FileFilterUtils.or(filter, new WildcardFileFilter("slf4j-simple-*.jar"));
//		filter = FileFilterUtils.or(filter, new WildcardFileFilter("jul-to-slf4j-*.jar"));
//		filter = FileFilterUtils.or(filter, new WildcardFileFilter("openjpa-*.jar"));
//		filter = FileFilterUtils.or(filter, new WildcardFileFilter("ehcache-*.jar"));
//		filter = FileFilterUtils.or(filter, new WildcardFileFilter("jetty-all-*.jar"));
//		filter = FileFilterUtils.or(filter, new WildcardFileFilter("quartz-*.jar"));
//		for (File o : FileUtils.listFiles(commons_ext_dir, filter, null)) {
//			jars.add(o.getAbsolutePath());
//		}
//		return jars;
//	}

	private static List<String> calculateExtraClassPathDefault() throws Exception {
		List<String> jars = new ArrayList<>();
		IOFileFilter filter = new WildcardFileFilter(x_base_core_project.class.getSimpleName() + "*.jar");
		for (File o : FileUtils.listFiles(Config.dir_store_jars(), filter, null)) {
			jars.add(o.getAbsolutePath());
		}
		filter = new WildcardFileFilter("slf4j-api-*.jar");
		filter = FileFilterUtils.or(filter, new WildcardFileFilter("slf4j-simple-*.jar"));
		filter = FileFilterUtils.or(filter, new WildcardFileFilter("jul-to-slf4j-*.jar"));
		filter = FileFilterUtils.or(filter, new WildcardFileFilter("openjpa-*.jar"));
		filter = FileFilterUtils.or(filter, new WildcardFileFilter("ehcache-*.jar"));
		filter = FileFilterUtils.or(filter, new WildcardFileFilter("jetty-all-*.jar"));
		filter = FileFilterUtils.or(filter, new WildcardFileFilter("quartz-*.jar"));
		for (File o : FileUtils.listFiles(Config.dir_commons_ext(), filter, null)) {
			jars.add(o.getAbsolutePath());
		}
		return jars;
	}

//	private static List<String> calculateExtraClassPathDependency(ClassInfo classInfo, File commons_ext_dir,
//			File store_jars_dir, File custom_jars_dir) throws Exception {
//		List<String> jars = new ArrayList<>();
//		IOFileFilter filter = FalseFileFilter.FALSE;
//		@SuppressWarnings("unchecked")
//		Class<?> cls = Class.forName(classInfo.getName());
//		List<Class<?>> list = (List<Class<?>>) FieldUtils.readStaticField(cls, "dependents");
//		for (Class<?> o : list) {
//			filter = FileFilterUtils.or(filter, new WildcardFileFilter(o.getSimpleName() + ".jar"));
//		}
//		for (File o : FileUtils.listFiles(store_jars_dir, filter, null)) {
//			jars.add(o.getAbsolutePath());
//		}
//		return jars;
//	}

}

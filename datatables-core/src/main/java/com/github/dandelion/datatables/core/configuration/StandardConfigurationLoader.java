/*
 * [The "BSD licence"]
 * Copyright (c) 2012 Dandelion
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of Dandelion nor the names of its contributors 
 * may be used to endorse or promote products derived from this software 
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.dandelion.datatables.core.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dandelion.datatables.core.constants.SystemConstants;
import com.github.dandelion.datatables.core.exception.ConfigurationLoadingException;
import com.github.dandelion.datatables.core.util.BundleUtils;
import com.github.dandelion.datatables.core.util.ClassUtils;
import com.github.dandelion.datatables.core.util.StringUtils;
import com.github.dandelion.datatables.core.util.UTF8Control;

/**
 * <p>
 * Default implementation of the {@link ConfigurationLoader}.
 * 
 * <p>
 * Note that a custom {@link ConfigurationLoader} can be used thanks to the
 * {@link DatatablesConfigurator}.
 * 
 * @author Thibault Duchateau
 * @since 0.9.0
 * @see DatatablesConfigurator
 */
public class StandardConfigurationLoader implements ConfigurationLoader {

	// Logger
	private static Logger logger = LoggerFactory.getLogger(StandardConfigurationLoader.class);

	protected static Properties defaultProperties;
	private Properties userProperties;
	private Set<String> groups;

	/**
	 * {@inheritDoc}
	 */
	public Properties loadDefaultConfiguration() throws ConfigurationLoadingException {

		if (defaultProperties == null) {

			logger.debug("Loading default configuration...");

			// Initialize properties
			Properties propertiesResource = new Properties();

			// Get default file as stream
			InputStream propertiesStream = null;

			try {
				propertiesStream = Thread.currentThread().getContextClassLoader()
						.getResourceAsStream(DT_DEFAULT_PROPERTIES);
				Reader reader = new InputStreamReader(propertiesStream, "UTF-8");
				propertiesResource.load(reader);
			} catch (IOException e) {
				throw new ConfigurationLoadingException("Unable to load the default configuration file", e);
			}
			finally {
				if (propertiesStream != null) {
					try {
						propertiesStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			defaultProperties = propertiesResource;

			logger.debug("Default configuration loaded");
		}

		return defaultProperties;
	}

	/**
	 * {@inheritDoc}
	 */
	public Properties loadUserConfiguration(Locale locale) {

		ResourceBundle userBundle = null;
		
		// First check if the resource bundle is externalized
		if (StringUtils.isNotBlank(System.getProperty(SystemConstants.DANDELION_DT_CONFIGURATION))) {

			String path = System.getProperty(SystemConstants.DANDELION_DT_CONFIGURATION);

			try {
				URL resourceURL = new File(path).toURI().toURL();
				URLClassLoader urlLoader = new URLClassLoader(new URL[] { resourceURL });
				userBundle = ResourceBundle.getBundle(DT_USER_PROPERTIES, locale, urlLoader, new UTF8Control());
			} catch (MalformedURLException e) {
				logger.warn("Wrong path to the externalized bundle", e);
			} catch (MissingResourceException e) {
				logger.info("No *.properties file in {}. Trying to lookup in classpath...", path);
			}

		}

		// No system property is set, retrieves the bundle from the classpath
		if (userBundle == null) {
			try {
				userBundle = ResourceBundle.getBundle(DT_USER_PROPERTIES, locale, new UTF8Control());
			} catch (MissingResourceException e) {
				// if no resource bundle is found, try using the context
				// classloader
				try {
					userBundle = ResourceBundle.getBundle(DT_USER_PROPERTIES, locale, Thread.currentThread()
							.getContextClassLoader(), new UTF8Control());
				} catch (MissingResourceException mre) {
					logger.debug("No custom configuration. Using default one.");
				}
			}
		}

		userProperties = BundleUtils.toProperties(userBundle);
		return userProperties;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set<String> resolveGroups(Locale locale) {

		logger.debug("Resolving groups for the locale {}...", locale);

		Set<String> groups = new HashSet<String>();

		if(userProperties != null && !userProperties.isEmpty()){
			for (Entry<Object, Object> entry : userProperties.entrySet()) {
				String key = entry.getKey().toString();
				if(!key.contains("i18n.locale.resolver")){
					groups.add(key.substring(0, key.indexOf(".")));
				}
			}
		}

		// The 'global' group is always added
		groups.add(DEFAULT_GROUP_NAME);
		
		logger.debug("{} groups resolved ({}).", groups.size(), groups.toString());
		
		this.groups = groups;
		
		return this.groups;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void resolveConfigurations(Map<String, TableConfiguration> map, Locale locale, HttpServletRequest request) throws ConfigurationLoadingException {

		logger.debug("Resolving configurations for the locale {}...", locale);

		loadTemplateEngineRelatedConfiguration(userProperties);
		
		// Retrieve the configuration for the 'global' group
		// The 'global' group contains all defaut properties, some of which may
		// have been overriden by user
		// The group information is removed from the key before storing the
		// property in the properties file
		Properties globalProperties = new Properties();
		for (Entry<Object, Object> entry : defaultProperties.entrySet()) {
			String key = entry.getKey().toString();
			if(!key.equals("i18n.locale.resolver")){
				globalProperties.put(key.substring(key.indexOf(".") + 1), entry.getValue());
			}
		}
		for (Entry<Object, Object> entry : userProperties.entrySet()) {
			String key = entry.getKey().toString();
			if (key.startsWith(DEFAULT_GROUP_NAME)) {
				globalProperties.put(key.substring(key.indexOf(".") + 1), entry.getValue());
			}
		}

		// Compute configuration to apply on each group
		Map<Configuration, Object> stagingConf = null;
		for (String groupName : groups) {
			
			logger.debug("Resolving configurations for the group {}...", groupName);
			
			// groupedProperties = globalProperties + current group
			Properties groupedProperties = new Properties();
			groupedProperties.putAll(globalProperties);
			for (Entry<Object, Object> entry : userProperties.entrySet()) {
				String key = entry.getKey().toString();
				if (key.startsWith(groupName)) {
					groupedProperties.put(key.substring(key.indexOf(".") + 1), entry.getValue());
				}
			}
			
			logger.debug("The group '{}' is initialized and contains {} properties", groupName,
					groupedProperties.size());

			stagingConf = new HashMap<Configuration, Object>();

			for (Entry<Object, Object> entry : groupedProperties.entrySet()) {
				String key = entry.getKey().toString();
				Configuration configuration = Configuration.findByName(key);
				if (configuration != null) {
					stagingConf.put(configuration, entry.getValue().toString());
				} else {
					logger.warn("The property '{}' (inside the '{}' group) doesn't exist", key, groupName);
				}
			}

			map.put(groupName, new TableConfiguration(stagingConf, request));
		}
		
		logger.debug("{} group(s) resolved ({}) for the locale {}", groups.size(), groups.toString(), locale);
	}
	

	/**
	 * TODO
	 * @param userProps
	 */
	private void loadTemplateEngineRelatedConfiguration(Properties userProps){
		
		boolean jstlPresent = ClassUtils.isPresent("javax.servlet.jsp.jstl.core.Config");

		if(jstlPresent && userProps != null){
			if(!userProps.isEmpty()){
				for(Entry<Object, Object> entry : userProps.entrySet()){
					String key = entry.getKey().toString();
					if (key.contains(Configuration.INTERNAL_MESSAGE_RESOLVER.getName())
							&& StringUtils.isBlank(entry.getValue().toString())) {
						userProps.put(entry.getKey(), "com.github.dandelion.datatables.jsp.i18n.JstlMessageResolver");
					}
				}
			}
			else{
				userProps.put("global." + Configuration.INTERNAL_MESSAGE_RESOLVER.getName(),
						"com.github.dandelion.datatables.jsp.i18n.JstlMessageResolver");
			}
		}
	}
}
/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.integration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.hibernate.search.exception.AssertionFailure;

/**
 * Helper class for integration testing of the JBoss Modules generated by the
 * hibernate-search-modules Maven module.
 *
 * The slot version is set as a property in the parent pom, and passed to the JVM
 * of Arquillian as a system property of the Maven maven-failsafe-plugin.
 *
 * @since 5.0
 */
public class VersionTestHelper {

	private static String hibernateSearchVersion = null;
	private static String hibernateSearchModuleSlot = null;
	private static String luceneFullVersion = null;
	private static String hibernateAnnotationsFullVersion = null;

	private VersionTestHelper() {
		//not meant to be created
	}

	/**
	 * @return the slot version of the Hibernate Search modules generated by this project
	 */
	private static synchronized String getModuleSlotString() {
		if ( hibernateSearchModuleSlot == null ) {
			String versionHibernateSearch = getDependencyVersionHibernateSearch();
			String[] split = versionHibernateSearch.split( "\\." );
			hibernateSearchModuleSlot = split[0] + '.' + split[1];
		}
		return hibernateSearchModuleSlot;
	}

	private static synchronized String getDependencyVersionHibernateSearch() {
		if ( hibernateSearchVersion == null ) {
			hibernateSearchVersion = injectVariables( "${dependency.version.HibernateSearch}" );
		}
		return hibernateSearchVersion;
	}

	public static synchronized String getDependencyVersionLucene() {
		if ( luceneFullVersion == null ) {
			luceneFullVersion = injectVariables( "${dependency.version.Lucene}" );
		}
		return luceneFullVersion;
	}

	public static synchronized String getDependencyVersionHibernateCommonsAnnotations() {
		if ( hibernateAnnotationsFullVersion == null ) {
			hibernateAnnotationsFullVersion = injectVariables( "${dependency.version.hcann}" );
		}
		return hibernateAnnotationsFullVersion;
	}

	/**
	 * @return the StringAsset to be used in a Manifest descriptor to enable the Hibernate-Search-ORM module
	 */
	public static Asset moduleDependencyManifest() {
		String manifest = Descriptors.create( ManifestDescriptor.class )
				.attribute( "Dependencies", getWildFlyModuleDependencies() )
				.exportAsString();
		return new StringAsset( manifest );
	}

	private static String getWildFlyModuleDependencies() {
		return "org.hibernate.search.orm:" + getModuleSlotString() + " services";
	}

	/**
	 * Adds the needed Manifest to a deployment to enable the Hibernate-Search-ORM module
	 *
	 * @param optionalDependencies additional dependencies to include in the Manifest descriptor
	 * @param archive it will contain the Manifest
	 */
	public static void addDependencyToSearchModule(Archive<?> archive) {
		archive.add( VersionTestHelper.moduleDependencyManifest(), "META-INF/MANIFEST.MF" );
	}

	private static String getRequiredSystemProperty(String property) {
		String systemProperty = System.getProperty( property );
		if ( systemProperty == null ) {
			throw new AssertionFailure(
					"The test setup requires the system property '" + property + "' to be set. Check your environment!"
			);
		}
		return systemProperty;
	}

	private static String injectVariables(String dependencies) {
		Properties projectCompilationProperties = new Properties();
		final InputStream resourceAsStream = VersionTestHelper.class.getClassLoader().getResourceAsStream( "module-versions.properties" );
		try {
			projectCompilationProperties.load( resourceAsStream );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
		finally {
			try {
				resourceAsStream.close();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
		Set<Entry<Object,Object>> entrySet = projectCompilationProperties.entrySet();
		for ( Entry<Object,Object> entry : entrySet ) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			dependencies = dependencies.replace( "${" + key + "}", value );
		}
		return dependencies;
	}

}

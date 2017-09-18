/*
 * Copyright 2012 Red Hat
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.drools.karaf.itest;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.osgi.CamelContextFactory;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.options.UrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class KarafIntegrationTestSupport extends CamelTestSupport {

    protected static final transient Logger LOG = LoggerFactory.getLogger(KarafIntegrationTestSupport.class);
    private static final String DROOLS_KARAF_FEATURES_CLASSIFIER = "drools.karaf.features.classifier";

    @Inject
    protected BundleContext bundleContext;

    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        for (Bundle b : bundleContext.getBundles()) {
            LOG.warn("Bundle: " + b.getSymbolicName());
        }
        throw new RuntimeException("Bundle " + symbolicName + " does not exist");
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        LOG.info("Get the bundleContext is " + bundleContext);
        LOG.info("Application installed as bundle id: " + bundleContext.getBundle().getBundleId());

        setThreadContextClassLoader();

        CamelContextFactory factory = new CamelContextFactory();
        factory.setBundleContext(bundleContext);
        factory.setRegistry(createRegistry());
        return factory.createContext();
    }

    protected void setThreadContextClassLoader() {
        // set the thread context classloader current bundle classloader
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
    }

    public static MavenArtifactProvisionOption getFeatureUrl(String groupId, String camelId) {
        return mavenBundle().groupId(groupId).artifactId(camelId);
    }

    public static UrlReference getCamelKarafFeatureUrl() {
        return getCamelKarafFeatureUrl(null);
    }

    public static UrlReference getCamelKarafFeatureUrl(String version) {

        String type = "xml/features";
        MavenArtifactProvisionOption mavenOption = mavenBundle().groupId("org.apache.camel.karaf").artifactId("apache-camel");
        if (version == null) {
            return mavenOption.versionAsInProject().type(type);
        } else {
            return mavenOption.version(version).type(type);
        }
    }

    public static Option loadCamelFeatures(String... features) {
        List<String> result = new ArrayList<String>();
        result.add("camel-core");
        result.add("camel-spring");
        result.add("camel-test");
        for (String feature : features) {
            result.add(feature);
        }
        return features(getCamelKarafFeatureUrl(), result.toArray(new String[result.size()]));
    }

    public static Option loadBrmsFeatures(String... features) {
        List<String> result = new ArrayList<String>();
        result.add("drools-module");
        for (String feature : features) {
            result.add(feature);
        }
        return features(getFeatureUrl("org.apache.karaf.assemblies.features", "brms-features").type("xml").classifier("features").versionAsInProject(), result.toArray(new String[4 + features.length]));
    }

    public static Option loadDroolsFeatures(String... features) {
        List<String> result = new ArrayList<String>();
        result.add("drools-module");
        for (String feature : features) {
            result.add(feature);
        }
        return features(getFeatureUrl("org.jboss.integration.fuse", "karaf-features").type("xml").classifier("features").versionAsInProject(), result.toArray(new String[4 + features.length]));
    }

    public static Option loadBrmsRepo() {
        return features(maven().groupId("org.apache.karaf.assemblies.features").artifactId("brms-features").type("xml").classifier("features").versionAsInProject().getURL());
    }

    public static Option loadDroolsRepo() {
        String droolsClassifier = System.getProperty(DROOLS_KARAF_FEATURES_CLASSIFIER) != null ? System.getProperty(DROOLS_KARAF_FEATURES_CLASSIFIER) : "features";
        return features(maven().groupId("org.drools").artifactId("drools-karaf-features").type("xml").classifier(droolsClassifier).versionAsInProject().getURL());
    }

    private static String getKarafVersion() {
        String karafVersion = System.getProperty("karafVersion");
        if (karafVersion == null) {
            // setup the default version of it
            karafVersion = "2.3.3";
        }
        return karafVersion;
    }

    public static Option getKarafDistributionOption() {
        String karafVersion = getKarafVersion();
        LOG.info("*** The karaf version is " + karafVersion + " ***");
        String localRepo = System.getProperty("maven.repo.local", "");
        if (localRepo.length() > 0) {
            LOG.info("Using alternative local Maven repository in {}.", new File(localRepo).getAbsolutePath());
            localRepo = new File(localRepo).getAbsolutePath().toString()+"@id=local,";
        }
        return new DefaultCompositeOption(KarafDistributionOption.karafDistributionConfiguration()
                                      .frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("tar.gz").versionAsInProject())
                                      .karafVersion(karafVersion)
                                      .name("Apache Karaf")
                                      .useDeployFolder(false).unpackDirectory(new File("target/paxexam/unpack/"))
            ,
            KarafDistributionOption.editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories",
                    localRepo+
                    "http://repo1.maven.org/maven2@id=central," +
                            "    http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix," +
                            "    http://repository.springsource.com/maven/bundles/release@id=springsource.release," +
                            "    http://repository.springsource.com/maven/bundles/external@id=springsource.external," +
                            "    https://oss.sonatype.org/content/repositories/releases/@id=sonatype, " +
                            "    http://download.eng.bos.redhat.com/brewroot/repos/jb-fuse-6.2-build/latest/maven@id=fuse62brew, " +
                            "    https://repository.jboss.org/nexus/content/groups/ea@id=ea"
            ));

    }

}

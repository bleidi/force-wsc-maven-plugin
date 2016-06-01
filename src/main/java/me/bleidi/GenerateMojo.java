package me.bleidi;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.stringtemplate.v4.STGroupDir;

import com.sforce.ws.codegen.Generator;
import com.sforce.ws.tools.wsdlc;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractMojo {

	private static final String TEMPLATE_DIR = String.format("%s/templates",
			Generator.class.getPackage().getName().replace('.', '/'));

	@Parameter(property = "force.wsc.packagePrefix", required = false)
	private String packagePrefix;

	@Parameter(property = "force.wsc.compile", required= false, defaultValue = "true")
	private Boolean compile;

	@Parameter(property = "force.wsc.standalone", required = false, defaultValue = "false")
	private Boolean standalone;

	@Parameter(property = "force.wsc.wsdlUrl", required = true)
	private String wsdlUrl;

	@Parameter(property = "force.wsc.destJarFilename", required = true, defaultValue ="${project.build.finalName}")
	private String destJarFilename;
	
	@Parameter(property = "force.wsc.destDir", required = true, defaultValue = "${project.build.directory}")
    private File destDir;

	public void execute() throws MojoExecutionException {
		try {
			getLog().info("Starting generating Java stubs for "+ wsdlUrl+".");

			wsdlc.run(wsdlUrl, destJarFilename, packagePrefix, standalone, new STGroupDir(TEMPLATE_DIR, '$', '$'), destDir.getAbsolutePath(),
					compile);
			getLog().info("Java stubs sucessfully generated!");
		} catch (Exception e) {
			throw new MojoExecutionException("Error generating stubs.", e);
		} finally {

		}
	}
	
	public static void main(String[] args) {
		String packagePrefix = System.getProperty(wsdlc.PACKAGE_PREFIX);
        boolean standAlone = Boolean.parseBoolean(System.getProperty(wsdlc.STANDALONE_JAR, "false"));
        System.out.println("Beginning run of multiple calls to wsdlc");

        // Parse input parameters
        if (args.length != 4) {
            System.out.println("show usage!");
            System.exit(2);
        }
        if (!"wsdldir".equalsIgnoreCase(args[0]) || !"jardir".equalsIgnoreCase(args[2])) {
            System.out.println("show usage");
            System.exit(2);
        }
        String wsdlDir = args[1];
        String jarDir = args[3];

        try {
            // Validate input parameters
            File wDir = new File(wsdlDir).getCanonicalFile();
            File jDir = new File(jarDir).getCanonicalFile();
            if (!wDir.exists() || !wDir.isDirectory()) {
                System.out.println("###  Input wsdldir '" + wsdlDir + "' does not exist or is not a directory");
                System.exit(1);
            }
            if (!jDir.exists() || !jDir.isDirectory()) {
                System.out.println("###  Input jardir '" + jarDir + "' does not exist or is not a directory");
                System.exit(1);
            }
            wsdlDir = wDir.getCanonicalPath();
            jarDir = jDir.getCanonicalPath();

            // Make the list of wsdl files and validate it.
            // The list will not contain any path info, just filenames:
            String[] wsdlFiles = wDir.list(WsdlFilter);
            if (wsdlFiles == null || wsdlFiles.length == 0) {
                System.out.println("###  Input wsdldir '" + wsdlDir + "' does not contain any " + WsdlSuffix + " files.");
                System.out.println("show usage");
                System.exit(1);
            }
            for (String aWsdl : wsdlFiles) {
                if (!aWsdl.endsWith(WsdlSuffix)) {
                    System.out.println("###  Software Error: Input wsdldir '" + wsdlDir
                            + "' produced listing of non-wsdl file '" + aWsdl + "'");
                    System.out.println("show usage");
                    System.exit(1);
                }
            }

            // Generate new jar names from wsdl names:
            // The jar files go to a different directory than the input wsdl files.
            // The jars have a special extension to make them easy to delete during build cleaning.
            // Also generate wsdl full paths, for wsdlc:
            String[] jarFiles = new String[wsdlFiles.length];
            String[] wsdlPaths = new String[wsdlFiles.length];
            for (int ix = 0; ix < wsdlFiles.length; ix++) {
                // The validation loop above guarantees this actually does a replace for each name:
                String jarName = wsdlFiles[ix].replace(WsdlSuffix, UniqueJarSuffix);
                jarFiles[ix] = new File(jarDir, jarName).getCanonicalPath();
                wsdlPaths[ix] = new File(wDir, wsdlFiles[ix]).getCanonicalPath();
            }

            // Delete any existing jar files. wsdlc won't delete anything:
            for (int ix = 0; ix < wsdlFiles.length; ix++) {
                File aJar = new File(jarFiles[ix]);
                if (aJar.exists()) {
                    System.out.println("Deleting existing " + aJar.getAbsolutePath());
                    aJar.delete();
                }
            }

            STGroupDir templates = new STGroupDir(wsdlc.TEMPLATE_DIR, '$', '$');
            // Run wsdlc on each wsdl in the wsdl directory:
            for (int ix = 0; ix < wsdlFiles.length; ix++) {
                System.out.println("Running wsdlc on " + wsdlPaths[ix] + "\n       to create " + jarFiles[ix]);
                wsdlc.run(wsdlPaths[ix], jarFiles[ix], packagePrefix, standAlone, templates, null, true);
            }
        }catch(Exception e){
        	e.printStackTrace();
        }
	}
        
	 private static final String WsdlSuffix = ".wsdl";
	    private static final String UniqueJarSuffix = ".apextest.jar";

	    private static final FilenameFilter WsdlFilter = new FilenameFilter() {
	        public boolean accept(File unused, String name) {
	            if (name == null) return false;
	            return (name.endsWith(WsdlSuffix));
	        }
	    }; 
}

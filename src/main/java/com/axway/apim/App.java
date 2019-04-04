package com.axway.apim;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axway.apim.lib.AppException;
import com.axway.apim.lib.CommandParameters;
import com.axway.apim.lib.ErrorCode;
import com.axway.apim.swagger.APIChangeState;
import com.axway.apim.swagger.APIImportConfigAdapter;
import com.axway.apim.swagger.APIManagerAdapter;
import com.axway.apim.swagger.api.state.IAPI;

/**
 * This is the Entry-Point of program and responsible to:</br>
 * - read the command-line parameters to create a <code>CommandParameters</code></br>
 * - next is to read the API-Contract by creating an <code>APIImportConfig</code> instance and calling getImportAPIDefinition()</br>
 * - the <code>APIManagerAdapter</code> method: <code>getAPIManagerAPI()</code> is used to create the API-Manager API state</br>
 * - An <code>APIChangeState</code> is created based on ImportAPI & API-Manager API
 * - Finally the APIManagerAdapter:applyChanges() is called to replicate the state into the APIManager.   
 * 
 * @author cwiechmann@axway.com
 */
public class App {

	private static Logger LOG = LoggerFactory.getLogger(App.class);

	public static void main(String args[]) { 
		int rc = run(args);
		System.exit(rc);
	}
		
	public static int run(String args[]) {
		try {
			LOG.info("------------------------------------------------------------------------");
			LOG.info("API-Manager Promote Version: 1.4.2");
			LOG.info("                                                                        ");
			LOG.info("To report issues or get help, please visit: ");
			LOG.info("https://github.com/Axway-API-Management-Plus/apimanager-swagger-promote");
			LOG.info("------------------------------------------------------------------------");
			
			Options options = new Options();
			Option option;
			
			option = new Option("a", "apidefinition", true, "(Optional) The API Definition either as Swagger (JSON-Formated) or a WSDL for SOAP-Services:\n"
					+ "- in local filesystem using a relative or absolute path. Example: swagger_file.json\n"
					+ "  Please note: Local filesystem is not supported for WSDLs. Please use direct URL or a URL-Reference-File.\n"
					+ "- a URL providing the Swagger-File or WSDL-File. Examples:\n"
					+ "  [username/password@]https://any.host.com/my/path/to/swagger.json\n"
					+ "  [username/password@]http://www.dneonline.com/calculator.asmx?wsdl\n"
					+ "- a reference file called anyname-i-want.url which contains a line with the URL\n"
					+ "  (same format as above for Swagger or WSDL)."
					+ "  If not specified, the API Definition configuration is read directly from the JSON-Formatted API-Config");
				option.setRequired(false);
				option.setArgName("swagger_file.json");
			options.addOption(option);
			
			option = new Option("c", "contract", true, "This is the JSON-Formatted API-Config containing information how to expose the API");
				option.setRequired(true);
				option.setArgName("api_config.json");
			options.addOption(option);
			
			option = new Option("s", "stage", true, "The stage this API should be imported.\n"
					+ "Will be used to lookup stage specific API-Config overrides (e.g.: api_config.preprod.json)");
				option.setArgName("preprod");
			options.addOption(option);
			
			option = new Option("h", "host", true, "The API-Manager hostname the API should be imported");
				option.setRequired(true);
				option.setArgName("api-host");
			options.addOption(option);
			
			option = new Option("port", true, "Optional parameter to declare the API-Manager port. Defaults to 8075.");
			option.setArgName("8181");
			options.addOption(option);
			
			option = new Option("u", "username", true, "Username used to authenticate. Please note, that this user must have Admin-Role");
				option.setRequired(true);
				option.setArgName("apiadmin");
			options.addOption(option);
			
			option = new Option("p", "password", true, "Password used to authenticate");
				option.setRequired(true);
				option.setArgName("changeme");
			options.addOption(option);
			
			option = new Option("f", "force", true, "Breaking changes can't be imported without this flag, unless the API is unpublished.");
				option.setArgName("true/[false]");
			options.addOption(option);
			
			option = new Option("iq", "ignoreQuotas", true, "Use this flag to ignore configured API quotas.");
			option.setArgName("true/[false]");
			options.addOption(option);
			
			option = new Option("clientOrgsMode", true, "Controls how configured Client-Organizations are treated. Defaults to replace!");
			option.setArgName("ignore|replace|add");
			options.addOption(option);
			
			option = new Option("clientAppsMode", true, "Controls how configured Client-Applications are treated. Defaults to replace!");
			option.setArgName("ignore|replace|add");
			options.addOption(option);	
			
			CommandLineParser parser = new DefaultParser();
			HelpFormatter formatter = new HelpFormatter();
			formatter.setWidth(140);
			CommandLine cmd = null;
			String scriptExt = "sh";
			if(System.getProperty("os.name").toLowerCase().contains("win")) scriptExt = "bat";
			try {
				cmd = parser.parse( options, args, false);
			} catch (ParseException e) {
				formatter.printHelp("Swagger-Import", options, true);
				System.out.println("\n");
				System.out.println("ERROR: " + e.getMessage());
				System.out.println();
				System.out.println("You may run one of the following examples:");
				System.out.println("scripts"+File.separator+"run-swagger-import."+scriptExt+" -a samples/petstore.json -c samples/minimal-config.json -h localhost -u apiadmin -p changeme");
				System.out.println("scripts"+File.separator+"run-swagger-import."+scriptExt+" -a samples/petstore.json -c samples/minimal-config.json -h localhost -u apiadmin -p changeme -s prod");
				System.out.println("scripts"+File.separator+"run-swagger-import."+scriptExt+" -a samples/petstore.json -c samples/complete-config.json -h localhost -u apiadmin -p changeme");
				System.out.println("scripts"+File.separator+"run-swagger-import."+scriptExt+" -a samples/petstore.json -c samples/org-and-apps-config.json -h localhost -u apiadmin -p changeme");
				System.out.println("scripts"+File.separator+"run-swagger-import."+scriptExt+" -a samples/petstore.url -c samples/minimal-config.json -h localhost -u apiadmin -p changeme");
				System.out.println("scripts"+File.separator+"run-swagger-import."+scriptExt+" -a https://petstore.swagger.io/v2/swagger.json -c samples/minimal-config.json -h localhost -u apiadmin -p changeme");
				System.out.println("scripts"+File.separator+"run-swagger-import."+scriptExt+" -a http://www.dneonline.com/calculator.asmx?wsdl -c samples/minimal-config-wsdl.json -h localhost -u apiadmin -p changeme");
				System.out.println("scripts"+File.separator+"run-swagger-import."+scriptExt+" -c samples/minimal-config-wsdl-api-definition.json -h localhost -u apiadmin -p changeme");
				System.out.println();
				System.out.println("For more information visit: https://github.com/Axway-API-Management-Plus/apimanager-swagger-promote/wiki");
				
				System.exit(99);
			}
			
			CommandParameters params = CommandParameters.getInstance();
			params.setCmd(cmd);
			
			APIManagerAdapter apimAdapter = new APIManagerAdapter();
			
			APIImportConfigAdapter contract = new APIImportConfigAdapter(params.getOptionValue("contract"), params.getOptionValue("stage"), params.getOptionValue("apidefinition"));
			IAPI desiredAPI = contract.getDesiredAPI();
			IAPI actualAPI = APIManagerAdapter.getAPIManagerAPI(APIManagerAdapter.getExistingAPI(desiredAPI.getPath()), desiredAPI);
			APIChangeState changeActions = new APIChangeState(actualAPI, desiredAPI);			
			
			apimAdapter.applyChanges(changeActions);
			LOG.info("Successfully replicated API-State into API-Manager");
			return 0;
		} catch (AppException ap) {
			if(ap.isLogStackStrace()) {
				LOG.error(ap.getMessage(), ap);
			} else {
				LOG.warn(ap.getMessage());
			}
			return ap.getErrorCode().getCode();
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			return ErrorCode.UNXPECTED_ERROR.getCode();
		}
	}
}

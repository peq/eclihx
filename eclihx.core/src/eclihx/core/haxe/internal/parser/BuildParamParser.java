package eclihx.core.haxe.internal.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import eclihx.core.haxe.internal.HaxePreferencesManager;
import eclihx.core.haxe.internal.configuration.HaxeConfiguration;
import eclihx.core.haxe.internal.configuration.HaxeConfigurationList;
import eclihx.core.haxe.internal.configuration.HaxeConfiguration.Platform;
import eclihx.core.util.console.parser.Builder;
import eclihx.core.util.console.parser.IIntValue;
import eclihx.core.util.console.parser.IParamExistense;
import eclihx.core.util.console.parser.IStringValue;
import eclihx.core.util.console.parser.core.InitializeParseError;
import eclihx.core.util.console.parser.core.Parameter;
import eclihx.core.util.console.parser.core.ParseError;
import eclihx.core.util.console.parser.core.Parser;

/**
 * Class which should parse haXe builder parameters and store them
 * into the <code>HaxeConfigurationList</code>
 */
public final class BuildParamParser {
	
	private final class MultiParameterRestrictor {
		
		private int counter = 0;
		private final String errorMessage;
		
		public MultiParameterRestrictor(String errorMessage) {
			this.errorMessage = errorMessage;
		}
		
		public void check() throws ParseError {
			if (++counter > 1) {
				throw new ParseError(
					String.format(errorMessage)
				);
			}
		}
		
		public void reset() {
			counter = 0;
		}
	}
	
	private final MultiParameterRestrictor mainParam = 
		new MultiParameterRestrictor(
			String.format("Multiple: %s.", 
				HaxePreferencesManager.PARAM_PREFIX_STARTUP_CLASS));
	
	private final MultiParameterRestrictor phpFrontParam = 
		new MultiParameterRestrictor(
			String.format("Multiple: %s.", 
				HaxePreferencesManager.PARAM_PREFIX_STARTUP_CLASS));
	
	private final MultiParameterRestrictor platformParam =
		new MultiParameterRestrictor(
				String.format("Multiple targets"));	
	
	private final Parser parser;
	private HaxeConfiguration currentConfig = null;
	private boolean parsingFile = false;
	private boolean confintueConfig = false;
	
	private class LibraryParam implements IStringValue {
		public void save(String value) throws ParseError {
			currentConfig.addLibrary(value);
		}
	}
	
	private class CompilationFlagParam implements IStringValue {
		public void save(String value) throws ParseError {
			currentConfig.addCompilationFlag(value);
		}
	}

	private class SwfOutput implements IStringValue {
		public void save(String value) throws ParseError {
			
			platformParam.check();				
			currentConfig.setPlatform(Platform.Flash);
				
			currentConfig.getFlashConfig().setOutputFile(value);
		}
	}
	
	private class Swf9Output implements IStringValue {
		public void save(String value) throws ParseError {
			
			platformParam.check();
			currentConfig.setPlatform(Platform.Flash);
			
			currentConfig.getFlashConfig().setOutputFile(value);
			currentConfig.getFlashConfig().setVersion(9);
		}
	}
	
	private class SwfVersion implements IIntValue {
		public void save(int value) throws ParseError {
			currentConfig.getFlashConfig().setVersion(value);
		}
	}
	
	private class MainClass implements IStringValue {
		public void save(String value) throws ParseError {
			mainParam.check();
			currentConfig.setStartupClass(value);
		}
	}
	
	private class HxmlStringParam implements IStringValue {

		public void save(String value) throws ParseError {
			
			if (parsingFile) {

				// If we're already parsing file we shouldn't accept this parameter
				throw new ParseError("Invalid parameter in the hxml-file");
			
			} else {
				
				if (value.startsWith("-")) {
					throw new ParseError(
						String.format("Invalid option: %s", value));
					
				} else if (value.endsWith("hxml")) {
					confintueConfig = true;
					parseFile(value);
				} else {
					
					mainParam.check();
					currentConfig.setStartupClass(value);
					
				}		
				
			}			
		}		
	}
	
	private class SourceDirectory implements IStringValue {
		public void save(String value) throws ParseError {
			currentConfig.addSourceDirectory(value);	
		}		
	}
	
	private class NekoOutput implements IStringValue {
		@Override
		public void save(String value) throws ParseError {
			platformParam.check();
			currentConfig.setPlatform(Platform.Neko);
			currentConfig.getNekoConfig().setOutputFile(value);
		}
	}
	private void init() {
		Parameter params[] = new Parameter[] {
			
			// -cp <path> : add a directory to find source files
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_SOURCE_DIRECTORY, 
				new SourceDirectory()),
						
			// -js <file> : compile code to JavaScript file
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_JAVA_SCRIPT_OUTPUT, 
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						platformParam.check();
						currentConfig.setPlatform(Platform.JavaScript);
						currentConfig.getJSConfig().setOutputFile(value);
					}
				}),
			
			// -as3 <directory> : generate AS3 code into target directory
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_ACTION_SCRIPT3_DIRECTORY, 
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						platformParam.check();
						currentConfig.setPlatform(Platform.ActionScript);
						currentConfig.getASConfig().setOutputDirectory(value);						
					}
				}),
			
			// -swf <file> : compile code to Flash SWF file
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_SWF_OUTPUT, 
				new SwfOutput()),
				
			// -swf9 <file> : compile code to Flash9 SWF file
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_SWF9_OUTPUT, 
				new Swf9Output()),
			
			// -swf-version <version> : change the SWF version (6,7,8,9)
			Builder.createIntParam(
				HaxePreferencesManager.PARAM_PREFIX_SWF_VERSION, 
				new SwfVersion()),
			
			// -swf-header <header> : define SWF header (width:height:fps:color)
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_SWF_HEADER, 
				new IStringValue () {
					@Override
					public void save(String value) throws ParseError {
						currentConfig.getFlashConfig().setHeader(value);
					}
				}),
			
			// -swf-lib <file> : add the SWF library to the compiled SWF
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_SWF_LIB,
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						currentConfig.getFlashConfig();
					}
				}),
			
			// -neko <file> : compile code to Neko Binary
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_NEKO_OUTPUT, 
				new NekoOutput()),
			
			// -php <directory> : generate PHP code into target directory
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_PHP_DIRECTORY, 
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						platformParam.check();
						currentConfig.setPlatform(Platform.PHP);
						currentConfig.getPHPConfig().setOutputDirectory(value);
					}
				}),
			
			// -x <file> : shortcut for compiling and executing a neko file
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_NEKO_SHORT_OUTPUT, 
				new NekoOutput()),
			
			// -xml <file> : generate XML types description
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_XML_DESCRIPTION_OUTPUT, 
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						currentConfig.setXmlDescriptionFile(value);
					}
				}),
			
			// -main <class> : select startup class
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_STARTUP_CLASS, 
				new MainClass()),
			
			// -lib <library[:version]> : use an haxelib library
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_HAXE_LIB, 
				new LibraryParam()),
			
			// -D <var> : define a conditional compilation flag
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_COMPILATION_FLAG, 
				new CompilationFlagParam()),
			
			// -resource <file@name> : add a named resource file
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_RESOURCE_FILE, 
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						currentConfig.addResource(value);
					}
				}),
			
			// -exclude <filename> : don't generate code for classes listed in this file
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_EXCLUDE_FILE, 
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						currentConfig.addExcludeFile(value);
					}
				}),
			
			// -v : turn on verbose mode
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_VERBOSE_MODE_FLAG, 
				new IParamExistense() {
					@Override
					public void save(boolean exist) throws ParseError {
						if (exist) {
							currentConfig.enableVerbose();
						}
					}					
				}),
			
			// -debug : add debug informations to the compiled code
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_DEBUG_MODE_FLAG, 
				new IParamExistense() {
					public void save(boolean exist) {
						if (exist) {
							currentConfig.enableDebug();
						}
					}
				}),			
			
			// -prompt : prompt on error
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_PROMT_ERROR_MODE_FLAG, 
				new IParamExistense() {
					@Override
					public void save(boolean exist) throws ParseError {
						if (exist) {
							currentConfig.enablePromptOnErrorMode();
						}
					}
				}),
			
			// -cmd : run the specified command after successful compilation
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_CMD_COMMAND, 
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						currentConfig.setCmdCommand(value);
					}
				}),
			
			// --flash-strict : more type strict flash API
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_FLASH_STRICT_FLAG, 
				new IParamExistense() {
					@Override
					public void save(boolean exist) throws ParseError {
						if (exist) {
							currentConfig.enableflashStrictMode();
						}						
					}
				}),
			
			// --no-traces : don't compile trace calls in the program
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_NO_TRACES_FLAG, 
				new IParamExistense() {
					@Override
					public void save(boolean exist) throws ParseError {
						if (exist) {
							currentConfig.enableNoTracesMode();
						}
					}
				}),
			
			// --flash-use-stage : place objects found on the 
			// stage of the SWF lib
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_FLASH_USE_STAGE_FLAG, 
				new IParamExistense() {
					@Override
					public void save(boolean exist) throws ParseError {
						if (exist) {
							currentConfig.enableFlashUseStageMode();
						}
					}
				}),
			
			// --neko-source : keep generated neko source
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_NEKO_SOURCE_FLAG, 
				new IParamExistense() {
					@Override
					public void save(boolean exist) throws ParseError {
						if (exist) {
							currentConfig.getNekoConfig().
								enableKeepNekoSource();
						}
					}
				}),
			
			//  --gen-hx-classes <file> : generate hx headers from SWF9 file
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_GENERATE_HAXE_CLASSES_SWF,
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						currentConfig.setSwfFileForHeaders(value);
					}
				}),
			
			//  --next : separate several haXe compilations
			// We shouldn't meet this in parsing. This parameter should be 
			// processed on another level of parsing.
			
			//  --display : display code tips
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_CODE_TIPS_FLAG,
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						
						String[] splitResult = value.split("@");
						
						if (splitResult.length != 2) {
							throw new ParseError("Invalid format of --debug option");
						}
						
						try {
							int position = Integer.parseInt(splitResult[1]);
							currentConfig.enableTips(splitResult[0], position);
						} catch (NumberFormatException e) {
							throw new ParseError(e.getMessage());
						}						
					}
				}
			),
			
			//  --no-output : compiles but does not generate any file
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_NO_OUTPUT_FLAG,
				new IParamExistense () {
					public void save(boolean exist) {
						if (exist) {
							currentConfig.setExplicitNoOutput();
						}
					}
				}),
			
			
			//  --times : mesure compilation times
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_TIME_MESURE_FLAG, 
				new IParamExistense() {
					@Override
					public void save(boolean exist) throws ParseError {
						if (exist) {
							currentConfig.enableTimeMesureMode();
						}
					}
				}),
			
			//  --no-inline : disable inlining
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_NO_INLINE_FLAG, 
				new IParamExistense() {
					@Override
					public void save(boolean exist) throws ParseError {
						if (exist) {
							currentConfig.enableNoInlineMode();
						}						
					}
				}),
			
			//--php-front <filename> : select the name for the php front file
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_PHP_FRONT_FILE,
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						phpFrontParam.check();
						currentConfig.getPHPConfig().setFrontFile(value);						
					}
				}),	
				
			// --remap <package:target> : remap a package to another one
			Builder.createStringParam(
				HaxePreferencesManager.PARAM_PREFIX_REMAP_PACKAGE,
				new IStringValue() {
					@Override
					public void save(String value) throws ParseError {
						currentConfig.addRemapPackage(value);
					}
				}),		
			
			//  -help  Display this list of options
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_HELP1_FLAG,
				new IParamExistense () {
					public void save(boolean exist) {
						if (exist) {
							currentConfig.enableHelp();
						}
					}
				}),
			
			//  --help  Display this list of options
			Builder.createFlagParam(
				HaxePreferencesManager.PARAM_PREFIX_HELP2_FLAG,
				new IParamExistense () {
					public void save(boolean exist) {
						if (exist) {
							currentConfig.enableHelp();
						}
					}
				}),
			
			// hxml file
			Builder.createStringParam("", new HxmlStringParam())
		};
		
		try {
			parser.initialize(params);
		} catch (InitializeParseError e) {
			e.printStackTrace();
		}
	}
	
	public BuildParamParser() {
		parser = new Parser();
		init();
	}
	
	private  HaxeConfiguration parseConfiguration(String strArray[]) throws ParseError {
		
		if (!confintueConfig) {
			
			// Reset counters 
			platformParam.reset();
			mainParam.reset();
			phpFrontParam.reset();
			
			currentConfig = new HaxeConfiguration();
		}
		
		parser.parse(strArray);
		
		return currentConfig;		
	}
	
	private HaxeConfigurationList parse(String str) throws ParseError {
		
		String configStrs[] = str.split("--next");
		
		HaxeConfigurationList configList = new HaxeConfigurationList();
	
		for (String configStr : configStrs) {			
			configList.add(parseConfiguration(configStr.replaceAll("\\s+", " ").split(" ")));			
		}
		
		return configList;
	}
	
	public HaxeConfigurationList parseString(String configStr) throws ParseError {
		this.parsingFile = false;
		return parse(configStr);
	}
	
	public HaxeConfigurationList parseFile(String fileName) throws ParseError
	{
		this.parsingFile = true;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			String buffer, fileContent = new String();
			while((buffer = in.readLine())!= null) {
				String trimString = buffer.trim();
				
				if (!(trimString.length() == 0 || trimString.startsWith("#"))) {
					// We adds only non-comments and not-empty strings 
					fileContent += trimString + " ";
				}
			}
			in.close();
			
			// Remove all not necessary spaces and new line symbols
			return parse(fileContent);
			
		} catch (IOException e) {
			throw new ParseError("Can't open file!");
		}
	}
}

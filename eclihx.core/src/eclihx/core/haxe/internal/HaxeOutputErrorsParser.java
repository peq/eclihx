package eclihx.core.haxe.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;

import eclihx.core.haxe.internal.configuration.CompilationError;
import eclihx.core.util.language.Pair;

/**
 * Parses haXe compile errors.
 * 
 * Supported versions: 2.0
 */
public final class HaxeOutputErrorsParser implements IHaxeOutputErrorsParser {

	private static final String SUPPORTED_VERSIONS[] = { ">=2.0" };
	
	/**
	 * Regular expression for splitting haXe error to groups.
	 * (file) : (line number) : (characters) : message
	 */
	private static final Pattern LINE_ERROR_PATTERN = Pattern.compile(
			"([^:]*)\\:(\\d+):([^:]*):(.*)");
	
	/**
	 * Regular expression for reading errors characters.
	 * characters (first chart) - (last chart)
	 */
	private static final Pattern CHARACTERS_ERROR_PATTERN = Pattern.compile(
			"\\s*characters\\s*(\\d+)\\-(\\d+)\\s*");
	
	/**
	 * String which is given in successful build.
	 */
	private static final String SUCCESS_BUILD_STRING = "Building complete";
	
	/*
	 * (non-Javadoc)
	 * @see eclihx.core.haxe.internal.IHaxeVersionsInfo#getSupportedVersions()
	 */
	@Override
	public final List<String> getSupportedVersions() {
		return Arrays.asList(SUPPORTED_VERSIONS);
	}
	
	/**
	 * Read the file name for the error file part.
	 * 
	 * @param fileNamePart a part with the file name.
	 * @return a file name
	 */
	protected String processFileName(String fileNamePart) {
		return fileNamePart.trim();
	}
	
	/**
	 * Get the line number for the line number part.
	 * 
	 * @param linePart a substring with the line number.
	 * @return a error line number. null if there's some error.
	 */
	protected Integer processLineNumber(String linePart) {
		try {
			return Integer.parseInt(linePart);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	/**
	 * Read the start and end column of the error.
	 * 
	 * @param charactersPart a substring with the line characters.
	 * @return a pair with the start and end character.
	 */
	protected Pair<Integer, Integer> processCharacters(String charactersPart) {
		
		final Matcher matcher = CHARACTERS_ERROR_PATTERN.matcher(charactersPart);
		
		if (matcher.matches()) {	
			
			try {
				int startCharPos = Integer.parseInt(matcher.group(1));
				int endCharPos = Integer.parseInt(matcher.group(2));
				
				return new Pair<Integer, Integer>(startCharPos, endCharPos);
			} catch (NumberFormatException numberFormatException) {
				Assert.isTrue(false);
				return null;
			}
		}
		
		return null;
	}
	
	/**
	 * Gives the message from the message part of the error string 
	 * @param messagePart the part with the message.
	 * @return error message.
	 */
	protected String processMessage(String messagePart) {
		return messagePart.trim();		
	}
	
	/*
	 * (non-Javadoc)
	 * @see eclihx.core.haxe.internal.IHaxeOutputErrorsParser#processErrorLine(java.lang.String)
	 */
	public ICompilerError processErrorLine(String errorLine) {
		
		final Matcher matcher = LINE_ERROR_PATTERN.matcher(errorLine);
		
		if (matcher.matches()) {			
			String filePath = processFileName(matcher.group(1));
			Integer lineNumber = processLineNumber(matcher.group(2));
			Pair<Integer, Integer> characters = processCharacters(matcher.group(3));
			String message = processMessage(matcher.group(4));
			
			if (!(filePath == null || lineNumber == null || characters == null || 
					message == null)) {
				
				return new CompilationError(filePath, lineNumber, characters, message);
			}			
		}
		
		// This is not a haXe compilation error
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see eclihx.core.haxe.internal.IHaxeOutputErrorsParser#parseErrors(java.lang.String)
	 */
	@Override
	public List<ICompilerError> parseErrors(String output, String buildFile) {
		
		ArrayList<ICompilerError> errorsList = new ArrayList<ICompilerError>(); 
		
		// Check build is success
		if (!output.contains(SUCCESS_BUILD_STRING)) {
			
			// It's expected that each line contains a error.
			for (String line : output.split("\n")) {
				ICompilerError error = processErrorLine(line);
				if (error != null) {
					errorsList.add(error);
				} else {
					// Error processing failed and we add to the build file
					errorsList.add(new CompilationError(buildFile, 0, 
							new Pair<Integer, Integer>(0, 0), line));
				}
			}
		}		
		
		return errorsList;
	}
}
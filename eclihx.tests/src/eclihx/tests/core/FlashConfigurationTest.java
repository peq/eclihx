package eclihx.tests.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eclihx.core.haxe.internal.configuration.FlashConfiguration;
import eclihx.core.haxe.internal.configuration.InvalidConfigurationException;

/**
 * Test for flash configuration class.
 * {@link FlashConfiguration}
 * @author GoodwinNK
 *
 */
public class FlashConfigurationTest {
	
	/**
	 * Test flash configuration.
	 */
	private FlashConfiguration flashConfiguration;
	
	/**
	 * Initialize flash configuration.
	 */
	@Before
	public void setUp() {
		flashConfiguration = new FlashConfiguration();
	}
	
	/**
	 * Test for {@link FlashConfiguration#printConfiguration()}
	 */
	@Test
	public void testPrintConfigurationSimple() {
		flashConfiguration.setHeader("testHeader");
		flashConfiguration.setOutputFile("out.swf");
		flashConfiguration.setVersion(7);
		
		try {
			Assert.assertEquals(null, 
				"-swf-version 7 -swf out.swf -swf-header testHeader ", 
				flashConfiguration.printConfiguration()
			);
		} catch (InvalidConfigurationException e) {
			Assert.fail();
		}
	}
	
	/**
	 * Test for {@link FlashConfiguration#printConfiguration()}
	 */
	@Test
	public void testPrintConfigurationQuoted() {
		
		flashConfiguration.setOutputFile("a a\\out.swf");
		flashConfiguration.setVersion(8);
		
		try {
			Assert.assertEquals(null, 
				"-swf-version 8 -swf \"a a\\out.swf\" ",
				flashConfiguration.printConfiguration()
			);
		} catch (InvalidConfigurationException e) {
			Assert.fail();
		}
	}
	
	/**
	 * Test for {@link FlashConfiguration#printConfiguration()}
	 * Test libraries with spaces are quoted.
	 * @throws InvalidConfigurationException If test is trying to test an invalid configuration.
	 */
	@Test
	public void testPrintConfigurationQuotedLib() throws InvalidConfigurationException {
		flashConfiguration.setOutputFile("some.swf");
		flashConfiguration.setVersion(8);
		flashConfiguration.addLibrary("some dir\\swflib.swf");
		
		Assert.assertEquals(null, 
				"-swf-version 8 -swf some.swf -swf-lib \"some dir\\swflib.swf\" ", 
				flashConfiguration.printConfiguration());
	}
	
	/**
	 * Test for {@link FlashConfiguration#printConfiguration()}
	 */
	@Test
	public void testValidateSuccess() {
		
		flashConfiguration.setHeader(null);
		flashConfiguration.setOutputFile("out.swf");
		flashConfiguration.setVersion(7);
		
		Assert.assertTrue(flashConfiguration.isValid());
	}

	/**
	 * Test for {@link FlashConfiguration#printConfiguration()}
	 */
	@Test
	public void testValidateFailVersion() {
		flashConfiguration.setHeader("header");
		flashConfiguration.setOutputFile("out.swf");
		
		// Invalid version.
		flashConfiguration.setVersion(15);
		Assert.assertFalse(flashConfiguration.isValid());
	}
	
	/**
	 * Test for {@link FlashConfiguration#printConfiguration()}
	 */
	@Test
	public void testValidateFailOutputFile() {

		// No output file test.
		flashConfiguration.setVersion(7);
		Assert.assertFalse(flashConfiguration.isValid());
	}
}

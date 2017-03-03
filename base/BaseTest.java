package base;
import static play.test.Helpers.fakeApplication;
import static play.test.Helpers.inMemoryDatabase;
import static play.test.Helpers.running;

import org.joda.time.DateTimeZone;

/**
 * Base class of all tests
 * @author dvraja
 *
 */
public abstract class BaseTest {

	public static final String TEST_DB = "keynectup_test";
	
	public static final Integer TEST_PORT = 3333;
	
	private DateTimeZone origDefault = DateTimeZone.getDefault();

	/**
	 * This runs test in a fake application
	 * @param callback
	 */
	protected void runInFakeApplication(final TestCallback callback) {
		
		running(fakeApplication(inMemoryDatabase(TEST_DB)), new Runnable() {
	        @Override
			public void run() {
	        	callback.execute();
	        }
		});
	}

}

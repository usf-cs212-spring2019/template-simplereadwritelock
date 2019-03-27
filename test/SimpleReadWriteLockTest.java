import org.junit.jupiter.api.Nested;

@SuppressWarnings("javadoc")
public class SimpleReadWriteLockTest {

	@Nested
	public class NestedReadWriteLockTest extends ReadWriteLockTest {

	}

	@Nested
	public class NestedThreadSafeIndexedSetTest extends ThreadSafeIndexedSetTest {

	}
}

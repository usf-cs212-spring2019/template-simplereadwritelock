import java.util.concurrent.locks.ReadWriteLock;

/**
 * A thread-safe version of {@link IndexedSet} using a read/write lock.
 *
 * @param <E>
 *            element type
 * @see IndexedSet
 * @see ReadWriteLock
 */
public class ThreadSafeIndexedSet<E> extends IndexedSet<E> {

	/** The lock used to protect concurrent access to the underlying set. */
	private SimpleReadWriteLock lock;

	/**
	 * Initializes an unsorted thread-safe indexed set.
	 */
	public ThreadSafeIndexedSet() {
		this(false);
	}

	/**
	 * Initializes a thread-safe indexed set.
	 *
	 * @param sorted whether the set should be sorted
	 */
	public ThreadSafeIndexedSet(boolean sorted) {
		super(sorted);
		lock = new SimpleReadWriteLock();
	}

	// TODO Override methods as necessary. Do NOT use the synchronized keyword.
}

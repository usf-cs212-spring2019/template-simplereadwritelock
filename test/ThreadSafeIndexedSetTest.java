import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("javadoc")
public class ThreadSafeIndexedSetTest {

	/**
	 * This creates (in a single threaded way) a TreeSet to be used as the expected
	 * output for these tests.
	 *
	 * @param max number passed to workers
	 * @return expected output
	 */
	private TreeSet<Path> expected(int max) {
		TreeSet<Path> paths = new TreeSet<Path>();

		for (int i = 1; i <= max; i++) {
			paths.add(Paths.get("test" + i + ".txt"));
		}

		return paths;
	}

	@Test
	public void testSynchronized() {
		// This makes sure you are not creating synchronized methods.
		// It will not detect if you use the synchronized keyword INSIDE those methods,
		// but you should still not do that!

		Method[] methods = ThreadSafeIndexedSet.class.getMethods();

		for (Method method : methods) {
			int modifiers = method.getModifiers();
			Assertions.assertFalse(Modifier.isSynchronized(modifiers),
					() -> method.toString() + " method should NOT be synchronized!");
		}
	}

	@Test
	public void testOverridden() {
		// This makes sure you overrode all of the required methods, but
		// not that you implemented them correctly.

		Set<String> expected = Arrays.stream(IndexedSet.class.getDeclaredMethods())
			.map(method -> method.getName())
			.collect(Collectors.toSet());

		Set<String> actual = Arrays.stream(ThreadSafeIndexedSet.class.getDeclaredMethods())
				.map(method -> method.getName())
				.collect(Collectors.toSet());

		// remove any method from actual that was in expected
		// anything leftover in expected was not overridden
		expected.removeAll(actual);

		Assertions.assertTrue(expected.isEmpty(),
				() -> "The following methods were not properly overridden: " + expected);
	}

	@Test
	public void testAddOnly() throws InterruptedException {
		int num = 10;
		int threads = 5;
		int timeout = 30000;

		ThreadSafeIndexedSet<Path> paths = new ThreadSafeIndexedSet<Path>();
		List<Thread> workers = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			workers.add(new AddPathWorker(paths, num));
		}

		assertConcurrent("testAddOnly()", workers, timeout);
		Assertions.assertEquals(expected(num), paths.sortedCopy());
	}

	@Test
	public void testAddAllOnly() throws InterruptedException {
		int num = 10;
		int threads = 5;
		int timeout = 30000;

		ThreadSafeIndexedSet<Path> paths = new ThreadSafeIndexedSet<Path>();
		List<Thread> workers = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			workers.add(new AddAllWorker(paths, num));
		}

		assertConcurrent("testAddAllOnly()", workers, timeout);
		Assertions.assertEquals(expected(num), paths.sortedCopy());
	}

	@Test
	public void testSmallDoubleAddCopy() throws InterruptedException {
		int num = 10;
		int timeout = 30000;

		ThreadSafeIndexedSet<Path> paths = new ThreadSafeIndexedSet<Path>();
		List<Thread> workers = new ArrayList<>();

		workers.add(new AddPathWorker(paths, num));
		workers.add(new CopyPathWorker(paths, num));

		assertConcurrent("testSmallDoubleAddCopy()", workers, timeout);
		Assertions.assertEquals(expected(num), paths.sortedCopy());
	}

	@Test
	public void testLargeDoubleAddCopy() throws InterruptedException {
		int num = 1000;
		int timeout = 30000;

		ThreadSafeIndexedSet<Path> paths = new ThreadSafeIndexedSet<Path>();
		List<Thread> workers = new ArrayList<>();

		workers.add(new AddPathWorker(paths, num));
		workers.add(new CopyPathWorker(paths, num));

		assertConcurrent("testLargeDoubleAddCopy()", workers, timeout);
		Assertions.assertEquals(expected(num), paths.sortedCopy());
	}

	@Test
	public void testSmallMultiAddCopy() throws InterruptedException {
		int num = 10;
		int threads = 5;
		int timeout = 30000;

		ThreadSafeIndexedSet<Path> paths = new ThreadSafeIndexedSet<Path>();
		List<Thread> workers = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			workers.add(new AddPathWorker(paths, num));
			workers.add(new CopyPathWorker(paths, num));
		}

		assertConcurrent("testSmallMultiAddCopy()", workers, timeout);
		Assertions.assertEquals(expected(num), paths.sortedCopy());
	}

	@Test
	public void testLargeMultiAddCopy() throws InterruptedException {
		int num = 1000;
		int threads = 5;
		int timeout = 30000;

		ThreadSafeIndexedSet<Path> paths = new ThreadSafeIndexedSet<Path>();
		List<Thread> workers = new ArrayList<>();

		for (int i = 0; i < threads; i++) {
			workers.add(new AddPathWorker(paths, num));
			workers.add(new CopyPathWorker(paths, num));
		}

		assertConcurrent("testSmallMultiAddCopy()", workers, timeout);
		Assertions.assertEquals(expected(num), paths.sortedCopy());
	}

	/** Forces several write operations */
	private static class AddPathWorker extends Thread {

		private ThreadSafeIndexedSet<Path> paths;
		private int num;

		public AddPathWorker(ThreadSafeIndexedSet<Path> paths, int num) {
			this.paths = paths;
			this.num = num;
		}

		@Override
		public void run() {
			for (int i = 1; i <= num; i++) {
				paths.add(Paths.get("test" + i + ".txt"));
			}
		}
	}

	/** Forces a single write operation */
	private static class AddAllWorker extends Thread {

		private ThreadSafeIndexedSet<Path> paths;
		private int num;

		public AddAllWorker(ThreadSafeIndexedSet<Path> paths, int num) {
			this.paths = paths;
			this.num = num;
		}

		@Override
		public void run() {
			ArrayList<Path> local = new ArrayList<>();

			for (int i = 1; i <= num; i++) {
				local.add(Paths.get("test" + i + ".txt"));
			}

			paths.addAll(local);
		}
	}

	/** Forces several read operations **/
	private static class CopyPathWorker extends Thread {

		private ThreadSafeIndexedSet<Path> paths;
		private int num;

		public CopyPathWorker(ThreadSafeIndexedSet<Path> paths, int num) {
			this.paths = paths;
			this.num = num;
		}

		@Override
		public void run() {
			for (int i = 0; i < num; i++) {
				paths.sortedCopy();
			}
		}
	}

	/**
	 * Handles multithreading in such a way that concurrent modification exceptions
	 * may be detected, causing the test to fail. Code comes from:
	 * https://github.com/junit-team/junit/wiki/Multithreaded-code-and-concurrency
	 *
	 * @param message           test message
	 * @param runnables         runnable objects to execute
	 * @param maxTimeoutSeconds timeout for test
	 * @throws InterruptedException
	 */
	public static void assertConcurrent(final String message,
			final List<? extends Runnable> runnables, final int maxTimeoutSeconds)
			throws InterruptedException {

		final int numThreads = runnables.size();
		final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
		final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);

		try {
			final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
			final CountDownLatch afterInitBlocker = new CountDownLatch(1);
			final CountDownLatch allDone = new CountDownLatch(numThreads);

			for (final Runnable submittedTestRunnable : runnables) {
				threadPool.submit(new Runnable() {

					@Override
					public void run() {
						allExecutorThreadsReady.countDown();
						try {
							afterInitBlocker.await();
							submittedTestRunnable.run();
						}
						catch (final Throwable e) {
							exceptions.add(e);
						}
						finally {
							allDone.countDown();
						}
					}
				});
			}

			// wait until all threads are ready
			Assertions.assertTrue(
					allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS),
					() -> "Timeout initializing threads! Perform long lasting "
							+ "initializations before passing runnables to assertConcurrent");

			// start all test runners
			afterInitBlocker.countDown();
			Assertions.assertTrue(allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS),
					() -> message + " timeout! More than" + maxTimeoutSeconds + "seconds");
		}
		finally {
			threadPool.shutdownNow();
		}

		Assertions.assertTrue(exceptions.isEmpty(),
				() -> message + " failed with exception(s)" + exceptions);
	}

}

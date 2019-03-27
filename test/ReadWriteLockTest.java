
import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Attempts to test the {@link SimpleReadWriteLock} and {@link ThreadSafeIndexedSet}.
 * These tests are not 100% accurate. They attempt to create threads in such a
 * way that problems will occur if the implementation is incorrect, but the
 * tests are inexact.
 */
@SuppressWarnings("javadoc")
public class ReadWriteLockTest {

	/** Specifies how long a worker thread should sleep. */
	public static final long WORKER_SLEEP = 1000;

	/**
	 * Specifies how long to wait before starting a new worker. Must be less
	 * than {@link #WORKER_SLEEP}.
	 */
	public static final long OFFSET_SLEEP = (long) 1000 / 2;

	/**
	 * Short timeout for testing threads.
	 */
	public static final long TIMEOUT_SHORT = Math.round(WORKER_SLEEP * 1.75);

	/**
	 * Long timeout for testing threads.
	 */
	public static final long TIMEOUT_LONG = Math.round(WORKER_SLEEP * 2.75);

	private static final String FORMAT = "Expected:%n%s%nActual:%n%s%n";

	/**
	 * Tests that two threads are able to simultaneously acquire the read
	 * lock without any exceptions. Should also finish in less than 200
	 * milliseconds if both threads are able to execute simultaneously.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testTwoReaders() throws InterruptedException {
		SimpleReadWriteLock lock = new SimpleReadWriteLock();
		StringBuffer buffer = new StringBuffer("\n");

		Thread reader1 = new Thread(new ReadWorker(lock, buffer));
		Thread reader2 = new Thread(new ReadWorker(lock, buffer));

		StringBuffer expected = new StringBuffer("\n");
		expected.append("Read Lock\n");
		expected.append("Read Lock\n");
		expected.append("Read Unlock\n");
		expected.append("Read Unlock\n");

		Assertions.assertTimeoutPreemptively(
				Duration.ofMillis(TIMEOUT_SHORT),
				() -> {
					reader1.start();
					reader2.start();
					reader2.join();
					reader1.join();
				}
		);

		Assertions.assertEquals(expected.toString(), buffer.toString(),
				() -> String.format(FORMAT, expected, buffer));
	}

		/**
		 * Tests that two threads are NOT able to simultaneously acquire the
		 * write lock without any exceptions. Should also finish in over 200
		 * milliseconds if both threads are able to execute simultaneously.
		 *
		 * @throws InterruptedException
		 */
		@Test
		public void testTwoWriters() throws InterruptedException {
			SimpleReadWriteLock lock = new SimpleReadWriteLock();
			StringBuffer buffer = new StringBuffer("\n");

			Thread writer1 = new Thread(new WriteWorker(lock, buffer));
			Thread writer2 = new Thread(new WriteWorker(lock, buffer));

			StringBuffer expected = new StringBuffer("\n");
			expected.append("Write Lock\n");
			expected.append("Write Unlock\n");
			expected.append("Write Lock\n");
			expected.append("Write Unlock\n");

			Assertions.assertTimeoutPreemptively(
					Duration.ofMillis(TIMEOUT_LONG),
					() -> {
						writer1.start();
						writer2.start();
						writer2.join();
						writer1.join();
					}
			);

			Assertions.assertEquals(expected.toString(), buffer.toString(),
					() -> String.format(FORMAT, expected, buffer));
		}

		/**
		 * Tests that two threads are NOT able to simultaneously acquire the
		 * read lock and write lock without any exceptions. Should also finish
		 * in over 200 milliseconds if both threads are able to execute
		 * simultaneously.
		 *
		 * @throws InterruptedException
		 */
		@Test
		public void testReaderWriter() throws InterruptedException {
			SimpleReadWriteLock lock = new SimpleReadWriteLock();
			StringBuffer buffer = new StringBuffer("\n");

			Thread reader = new Thread(new ReadWorker(lock, buffer));
			Thread writer = new Thread(new WriteWorker(lock, buffer));

			StringBuffer expected = new StringBuffer("\n");
			expected.append("Read Lock\n");
			expected.append("Read Unlock\n");
			expected.append("Write Lock\n");
			expected.append("Write Unlock\n");

			Assertions.assertTimeoutPreemptively(
					Duration.ofMillis(TIMEOUT_LONG),
					() -> {
						reader.start();

						// wait a little bit before starting next thread
						Thread.sleep(OFFSET_SLEEP);
						writer.start();

						writer.join();
						reader.join();
					}
			);

			Assertions.assertEquals(expected.toString(), buffer.toString(),
					() -> String.format(FORMAT, expected, buffer));
		}

		/**
		 * Tests that two threads are NOT able to simultaneously acquire the
		 * read lock and write lock without any exceptions. Should also finish
		 * in over 200 milliseconds if both threads are able to execute
		 * simultaneously.
		 *
		 * @throws InterruptedException
		 */
		@Test
		public void testWriterReader() throws InterruptedException {
			SimpleReadWriteLock lock = new SimpleReadWriteLock();
			StringBuffer buffer = new StringBuffer("\n");

			Thread writer = new Thread(new WriteWorker(lock, buffer));
			Thread reader = new Thread(new ReadWorker(lock, buffer));

			StringBuffer expected = new StringBuffer("\n");
			expected.append("Write Lock\n");
			expected.append("Write Unlock\n");
			expected.append("Read Lock\n");
			expected.append("Read Unlock\n");

			Assertions.assertTimeoutPreemptively(
					Duration.ofMillis(TIMEOUT_LONG),
					() -> {
						writer.start();

						// wait a little bit before starting next thread
						Thread.sleep(OFFSET_SLEEP);
						reader.start();

						reader.join();
						writer.join();
					}
			);

			Assertions.assertEquals(expected.toString(), buffer.toString(),
					() -> String.format(FORMAT, expected, buffer));
		}

		/**
		 * Tests that two threads are NOT able to simultaneously acquire the
		 * read lock and write lock without any exceptions, but multiple threads
		 * may acquire read locks (even if a writer is waiting). Should also
		 * finish in over 200 milliseconds if all threads are able to execute
		 * properly.
		 *
		 * @throws InterruptedException
		 */
		@Test
		public void testMultiReadFirst() throws InterruptedException {
			SimpleReadWriteLock lock = new SimpleReadWriteLock();
			StringBuffer buffer = new StringBuffer("\n");

			Thread reader1 = new Thread(new ReadWorker(lock, buffer));
			Thread reader2 = new Thread(new ReadWorker(lock, buffer));

			Thread writer1 = new Thread(new WriteWorker(lock, buffer));

			StringBuffer expected = new StringBuffer("\n");
			expected.append("Read Lock\n");
			expected.append("Read Lock\n");
			expected.append("Read Unlock\n");
			expected.append("Read Unlock\n");
			expected.append("Write Lock\n");
			expected.append("Write Unlock\n");

			Assertions.assertTimeoutPreemptively(
					Duration.ofMillis(TIMEOUT_LONG),
					() -> {
						reader1.start();
						reader2.start();

						// wait a little bit before starting next thread
						Thread.sleep(OFFSET_SLEEP);
						writer1.start();

						reader2.join();
						writer1.join();
						reader1.join();
					}
			);

			Assertions.assertEquals(expected.toString(), buffer.toString(),
					() -> String.format(FORMAT, expected, buffer));
		}

		/**
		 * Tests that two threads are NOT able to simultaneously acquire the
		 * read lock and write lock without any exceptions, but multiple threads
		 * may acquire read locks (even if a writer is waiting). Should also
		 * finish in over 200 milliseconds if all threads are able to execute
		 * properly.
		 *
		 * @throws InterruptedException
		 */
		@Test
		public void testMultiWriteFirst() throws InterruptedException {
			SimpleReadWriteLock lock = new SimpleReadWriteLock();
			StringBuffer buffer = new StringBuffer("\n");

			Thread writer1 = new Thread(new WriteWorker(lock, buffer));

			Thread reader1 = new Thread(new ReadWorker(lock, buffer));
			Thread reader2 = new Thread(new ReadWorker(lock, buffer));

			StringBuffer expected = new StringBuffer("\n");
			expected.append("Write Lock\n");
			expected.append("Write Unlock\n");
			expected.append("Read Lock\n");
			expected.append("Read Lock\n");
			expected.append("Read Unlock\n");
			expected.append("Read Unlock\n");

			Assertions.assertTimeoutPreemptively(
					Duration.ofMillis(TIMEOUT_LONG),
					() -> {
						writer1.start();

						// wait a little bit before starting next thread
						Thread.sleep(OFFSET_SLEEP);
						reader1.start();
						reader2.start();

						reader2.join();
						reader1.join();
						writer1.join();
					}
			);

			Assertions.assertEquals(expected.toString(), buffer.toString(),
					() -> String.format(FORMAT, expected, buffer));
	}

	private static class ReadWorker implements Runnable {

		private final StringBuffer buffer;
		private final SimpleReadWriteLock lock;

		public ReadWorker(SimpleReadWriteLock lock, StringBuffer buffer) {
			this.lock = lock;
			this.buffer = buffer;
		}

		@Override
		public void run() {
			lock.readLock().lock();
			buffer.append("Read Lock\n");

			try {
				Thread.sleep(WORKER_SLEEP);
			}
			catch (Exception ex) {
				buffer.append("Read Error\n");
			}

			buffer.append("Read Unlock\n");
			lock.readLock().unlock();
		}
	}

	private static class WriteWorker implements Runnable {

		private final StringBuffer buffer;
		private final SimpleReadWriteLock lock;

		public WriteWorker(SimpleReadWriteLock lock, StringBuffer buffer) {
			this.lock = lock;
			this.buffer = buffer;
		}

		@Override
		public void run() {
			lock.writeLock().lock();
			buffer.append("Write Lock\n");

			try {
				Thread.sleep(WORKER_SLEEP);
			}
			catch (Exception ex) {
				buffer.append("Write Error\n");
			}

			buffer.append("Write Unlock\n");
			lock.writeLock().unlock();
		}
	}
}

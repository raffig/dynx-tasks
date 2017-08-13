package pl.frati.dynx.tasks;

import java.io.InterruptedIOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.frati.dynx.tasks.Task.State;

public class AbstractThreadTaskTest {

	@Test
	public void checkExecution() throws InterruptedException {

		TestTask task = new TestTask(4);

		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		System.out.println(task);
		Thread.sleep(100);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Thread.sleep(100);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.awaitEnd();
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test
	public void checkPause() throws InterruptedException {

		TestTask task = new TestTask(5);

		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		System.out.println(task);
		Thread.sleep(100);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestPause();
		Thread.sleep(200);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.PAUSED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Thread.sleep(400);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.PAUSED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestResume();

		Thread.sleep(100);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.awaitEnd();
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test
	public void checkStop() throws InterruptedException {

		TestTask task = new TestTask(4);

		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		System.out.println(task);
		Thread.sleep(100);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStop();
		Thread.sleep(100);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.STOPPPED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());

		task.awaitEnd();
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.STOPPPED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test
	public void checkInterruptHandling() {
		
		int interruptAtLoop = 4;
		TestTask task = new TestTask(6, interruptAtLoop);
		
		task.requestStart();
		
		System.out.println(task);
		
		task.awaitEnd();
		
		System.out.println(task);

		Assert.assertEquals(task.getCurrentLoop(), interruptAtLoop);
		Assert.assertEquals(task.getCurrentState(), State.STOPPPED);
		
	}
	
	private static final class TestTask extends AbstractThreadTask {

		private int loops;
		private int currentLoop;
		private int interruptAtLoop = -1;

		public TestTask(int loops) {
			super();
			this.loops = loops;
		}

		public TestTask(int loops, int interruptAtLoop) {
			super();
			this.loops = loops;
			this.interruptAtLoop = interruptAtLoop;
		}

		@Override
		protected boolean hasNextPortion() {
			return currentLoop < loops;
		}
		
		public int getCurrentLoop() {
			return currentLoop;
		}

		@Override
		protected void executeNextPortion() throws InterruptedException, InterruptedIOException {
			Thread.sleep(100);
			
			if (interruptAtLoop == currentLoop) {
				Thread.currentThread().interrupt();
				return;
			}

			currentLoop++;
		}

	}
}

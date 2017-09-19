package pl.frati.dynx.tasks;

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.frati.dynx.tasks.AbstractThreadTask;
import pl.frati.dynx.tasks.ParalleledTask;
import pl.frati.dynx.tasks.Task.State;

public class ParalleledTaskTest {

	@Test
	public void checkEmptyThreadedTask() {
		ParalleledTask task = new ParalleledTask();

		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		Assert.assertEquals(task.getCurrentState(), State.FINISHED);

		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test
	public void checkAwaitEndOnSingleThread() throws InterruptedException {

		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(100, 0, 5));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		task.awaitEnd();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test(dependsOnMethods = "checkAwaitEndOnSingleThread")
	public void checkAwaitEndOnMultipleThreads() throws InterruptedException {

		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(100, 0, 5));
		task.addThreadTask(new ThreadTestTask(50, 0, 15));
		task.addThreadTask(new ThreadTestTask(100, 50, 4));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		task.awaitEnd();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}
	
	@Test(dependsOnMethods = "checkAwaitEndOnSingleThread")
	public void checkThreadedTaskOnSingleThread() throws InterruptedException {

		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(100, 0, 5));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		System.out.println(task.toString());

		Thread.sleep(50);

		System.out.println(task.toString());

		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.awaitEnd();

		System.out.println(task.toString());

		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test(dependsOnMethods = "checkAwaitEndOnSingleThread")
	public void checkThreadedTaskOnSingleThreadWithPause() throws InterruptedException {

		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(100, 0, 3));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		System.out.println(task.toString());

		Thread.sleep(50);

		System.out.println(task.toString());

		task.requestPause();
		Assert.assertEquals(task.getCurrentState(), State.PAUSING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Thread.sleep(150);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.PAUSED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Thread.sleep(700);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.PAUSED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestResume();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Thread.sleep(100);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.awaitEnd();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test(dependsOnMethods = "checkAwaitEndOnSingleThread")
	public void checkThreadedTaskOnSingleThreadWithStop() throws InterruptedException {

		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(100, 0, 3));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		System.out.println(task.toString());

		Thread.sleep(50);

		System.out.println(task.toString());

		task.requestStop();
		Assert.assertEquals(task.getCurrentState(), State.STOPPING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.awaitEnd();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.STOPPPED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test(dependsOnMethods = "checkAwaitEndOnMultipleThreads")
	public void checkThreadedTaskOnMultipleThreadsWithPause() throws InterruptedException {

		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(100, 0, 3));
		task.addThreadTask(new ThreadTestTask(100, 50, 5));
		task.addThreadTask(new ThreadTestTask(100, 0, 4));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestStart();

		System.out.println(task.toString());

		Thread.sleep(100);

		System.out.println(task.toString());

		task.requestPause();
		Assert.assertEquals(task.getCurrentState(), State.PAUSING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Thread.sleep(200);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.PAUSED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Thread.sleep(700);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.PAUSED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.requestResume();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Thread.sleep(100);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		task.awaitEnd();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test(dependsOnMethods = "checkAwaitEndOnSingleThread")
	public void checkTaskFailOnSingleThread() throws InterruptedException {
		
		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(100, 0, 3, 2));
		
		task.requestStart();
		
		task.awaitEnd();
		
		Assert.assertEquals(task.getCurrentState(), State.FAILED);
		Assert.assertTrue(task.getFailCause().isPresent());
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test
	public void checkTaskFailOnMultipleThreads() throws InterruptedException {
		
		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(100, 0, 7, 2));
		task.addThreadTask(new ThreadTestTask(100, 0, 20));
		task.addThreadTask(new ThreadTestTask(100, 0, 18));
		
		task.requestStart();
		
		Thread.sleep(400);
		
		System.out.println(task);
		
		Assert.assertEquals(task.getCurrentState(), State.FAILED);
		Assert.assertTrue(task.getFailCause().isPresent());
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test(dependsOnMethods = "checkAwaitEndOnMultipleThreads")
	public void checkTaskFailAfterPauseOnMultipleThreads() throws InterruptedException {
		
		ParalleledTask task = new ParalleledTask();
		task.addThreadTask(new ThreadTestTask(400, 0, 7, 2));
		task.addThreadTask(new ThreadTestTask(500, 0, 20));
		task.addThreadTask(new ThreadTestTask(100, 0, 18));
		
		task.requestStart();
		
		Thread.sleep(500);
		
		task.requestPause();
		
		System.out.println(task);
		
		task.awaitEnd();
		
		System.out.println(task);
		
		Assert.assertEquals(task.getCurrentState(), State.FAILED);
		Assert.assertTrue(task.getFailCause().isPresent());
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());
	}

	@Test(enabled=false)
	public void checkStateObservers() {
		// TODO test observers
	}

	private static class ThreadTestTask extends AbstractThreadTask {

		private int delay;
		private int loops;
		private int loopTime;
		
		private int currentLoop;
		
		private Integer failAfterLoops;

		public ThreadTestTask(int loopTime, int delay, int loops) {
			super();
			this.loopTime = loopTime;
			this.delay = delay;
			this.loops = loops;
		}

		public ThreadTestTask(int loopTime, int delay, int loops, int failAfterLoops) {
			super();
			this.loopTime = loopTime;
			this.delay = delay;
			this.loops = loops;
			this.failAfterLoops = failAfterLoops;
		}

		@Override
		protected boolean hasNextPortion() {
			return currentLoop < loops;
		}

		@Override
		protected void executeNextPortion() {
			try {
				
				if ((currentLoop == 0) && (delay > 0)) {
					Thread.sleep(delay);
				}
				
				Thread.sleep(loopTime);
				currentLoop++;

				if ((failAfterLoops != null) && (failAfterLoops == currentLoop)) {
					throw new RuntimeException("Task failed");
				}

			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted!", e);
			}
		}

	}

}

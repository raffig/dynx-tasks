package pl.frati.dynx.tasks;

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.frati.dynx.tasks.AbstractThreadTask;
import pl.frati.dynx.tasks.ThreadedTask;
import pl.frati.dynx.tasks.Task.State;

public class ThreadedTaskTest {

	@Test
	public void checkEmptyThreadedTask() {
		ThreadedTask task = new ThreadedTask();

		Assert.assertEquals(State.NOT_STARTED, task.getCurrentState());

		task.requestStart();

		Assert.assertEquals(State.RUNNING, task.getCurrentState());
	}

	@Test
	public void checkThreadedTaskOnSingleThread() throws InterruptedException {

		ThreadedTask task = new ThreadedTask();
		task.addThreadTask(new ThreadTestTask(0, 5));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);

		task.requestStart();

		System.out.println(task.toString());

		Thread.sleep(50);

		System.out.println(task.toString());

		Assert.assertEquals(task.getCurrentState(), State.RUNNING);

		Thread.sleep(700);

		System.out.println(task.toString());

		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
	}

	@Test
	public void checkThreadedTaskOnSingleThreadWithPause() throws InterruptedException {

		ThreadedTask task = new ThreadedTask();
		task.addThreadTask(new ThreadTestTask(0, 3));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);

		task.requestStart();

		System.out.println(task.toString());

		Thread.sleep(50);

		System.out.println(task.toString());

		task.requestPause();
		Assert.assertEquals(task.getCurrentState(), State.PAUSING);

		Thread.sleep(150);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.PAUSED);

		Thread.sleep(700);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.PAUSED);

		task.requestResume();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.RUNNING);

		Thread.sleep(100);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.RUNNING);

		Thread.sleep(600);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
	}

	@Test
	public void checkThreadedTaskOnSingleThreadWithStop() throws InterruptedException {

		ThreadedTask task = new ThreadedTask();
		task.addThreadTask(new ThreadTestTask(0, 3));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);

		task.requestStart();

		System.out.println(task.toString());

		Thread.sleep(50);

		System.out.println(task.toString());

		task.requestStop();
		Assert.assertEquals(task.getCurrentState(), State.STOPPING);

		Thread.sleep(150);

		Assert.assertEquals(task.getCurrentState(), State.STOPPPED);
	}

	@Test
	public void checkThreadedTaskOnMultipleThreadsWithPause() throws InterruptedException {

		ThreadedTask task = new ThreadedTask();
		task.addThreadTask(new ThreadTestTask(0, 3));
		task.addThreadTask(new ThreadTestTask(50, 5));
		task.addThreadTask(new ThreadTestTask(0, 4));

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);

		task.requestStart();

		System.out.println(task.toString());

		Thread.sleep(100);

		System.out.println(task.toString());

		task.requestPause();
		Assert.assertEquals(task.getCurrentState(), State.PAUSING);

		Thread.sleep(200);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.PAUSED);

		Thread.sleep(700);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.PAUSED);

		task.requestResume();

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.RUNNING);

		Thread.sleep(100);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.RUNNING);

		Thread.sleep(600);

		System.out.println(task.toString());
		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
	}

	private static class ThreadTestTask extends AbstractThreadTask {

		private int delay;
		private int loops;
		
		private int currentLoop;

		public ThreadTestTask(int delay, int loops) {
			super();
			this.delay = delay;
			this.loops = loops;
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
				
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted!", e);
			}
			currentLoop++;
		}

	}

}

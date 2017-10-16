package pl.frati.dynx.tasks;

import java.io.InterruptedIOException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import pl.frati.dynx.tasks.Task.State;
import pl.frati.dynx.tasks.Task.StateObserver;

public class AbstractThreadTaskTest {

	@Test
	public void checkExecution() throws InterruptedException {

		TestTask task = new TestTask(4);
		TestStateObserver observer = new TestStateObserver();
		task.addStateObserver(observer);

		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.NOT_STARTED);
		Assert.assertFalse(task.getRequestStartTime().isPresent());
		Assert.assertFalse(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());
		Assert.assertTrue(observer.observedStateChanges.isEmpty());

		task.requestStart();

		System.out.println(task);
		Thread.sleep(100);
		System.out.println(task);
		
		Assert.assertEquals(State.NOT_STARTED, observer.observedStateChanges.get(0).getKey());
		Assert.assertEquals(State.STARTING, observer.observedStateChanges.get(0).getValue());
		Assert.assertEquals(State.STARTING, observer.observedStateChanges.get(1).getKey());
		Assert.assertEquals(State.RUNNING, observer.observedStateChanges.get(1).getValue());

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

		Assert.assertEquals(State.RUNNING, observer.observedStateChanges.get(2).getKey());
		Assert.assertEquals(State.FINISHED, observer.observedStateChanges.get(2).getValue());
	}

	@Test
	public void checkPause() throws InterruptedException {

		TestTask task = new TestTask(5);
		TestStateObserver observer = new TestStateObserver();
		task.addStateObserver(observer);

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

		Assert.assertEquals(observer.observedStateChanges.get(0).getKey(), State.NOT_STARTED);
		Assert.assertEquals(observer.observedStateChanges.get(0).getValue(), State.STARTING);
		Assert.assertEquals(observer.observedStateChanges.get(1).getKey(), State.STARTING);
		Assert.assertEquals(observer.observedStateChanges.get(1).getValue(), State.RUNNING);

		task.requestPause();
		Thread.sleep(200);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.PAUSED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Assert.assertEquals(observer.observedStateChanges.get(2).getKey(), State.RUNNING);
		Assert.assertEquals(observer.observedStateChanges.get(2).getValue(), State.PAUSING);
		Assert.assertEquals(observer.observedStateChanges.get(3).getKey(), State.PAUSING);
		Assert.assertEquals(observer.observedStateChanges.get(3).getValue(), State.PAUSED);
		Assert.assertEquals(observer.observedStateChanges.size(), 4);

		Thread.sleep(400);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.PAUSED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());
		Assert.assertEquals(observer.observedStateChanges.size(), 4);

		task.requestResume();

		Thread.sleep(100);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.RUNNING);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertFalse(task.getEndTime().isPresent());

		Assert.assertEquals(observer.observedStateChanges.get(4).getKey(), State.PAUSED);
		Assert.assertEquals(observer.observedStateChanges.get(4).getValue(), State.RUNNING);
		Assert.assertEquals(observer.observedStateChanges.size(), 5);

		task.awaitEnd();
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.FINISHED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());

		Assert.assertEquals(observer.observedStateChanges.size(), 6);
		Assert.assertEquals(observer.observedStateChanges.get(5).getKey(), State.RUNNING);
		Assert.assertEquals(observer.observedStateChanges.get(5).getValue(), State.FINISHED);
	}

	@Test
	public void checkStop() throws InterruptedException {

		TestTask task = new TestTask(4);
		TestStateObserver observer = new TestStateObserver();
		task.addStateObserver(observer);

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

		Assert.assertEquals(observer.observedStateChanges.get(0).getKey(), State.NOT_STARTED);
		Assert.assertEquals(observer.observedStateChanges.get(0).getValue(), State.STARTING);
		Assert.assertEquals(observer.observedStateChanges.get(1).getKey(), State.STARTING);
		Assert.assertEquals(observer.observedStateChanges.get(1).getValue(), State.RUNNING);

		task.requestStop();
		Thread.sleep(100);
		System.out.println(task);

		Assert.assertEquals(task.getCurrentState(), State.STOPPPED);
		Assert.assertTrue(task.getRequestStartTime().isPresent());
		Assert.assertTrue(task.getActualStartTime().isPresent());
		Assert.assertTrue(task.getEndTime().isPresent());

		Assert.assertEquals(observer.observedStateChanges.get(2).getKey(), State.RUNNING);
		Assert.assertEquals(observer.observedStateChanges.get(2).getValue(), State.STOPPING);
		Assert.assertEquals(observer.observedStateChanges.get(3).getKey(), State.STOPPING);
		Assert.assertEquals(observer.observedStateChanges.get(3).getValue(), State.STOPPPED);

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
		TestStateObserver observer = new TestStateObserver();
		task.addStateObserver(observer);
		
		task.requestStart();
		
		System.out.println(task);
		
		task.awaitEnd();
		
		System.out.println(task);

		Assert.assertEquals(task.getCurrentLoop(), interruptAtLoop);
		Assert.assertEquals(task.getCurrentState(), State.STOPPPED);
		
		Assert.assertEquals(observer.observedStateChanges.get(0).getKey(), State.NOT_STARTED);
		Assert.assertEquals(observer.observedStateChanges.get(0).getValue(), State.STARTING);
		Assert.assertEquals(observer.observedStateChanges.get(1).getKey(), State.STARTING);
		Assert.assertEquals(observer.observedStateChanges.get(1).getValue(), State.RUNNING);
		Assert.assertEquals(observer.observedStateChanges.get(2).getKey(), State.RUNNING);
		Assert.assertEquals(observer.observedStateChanges.get(2).getValue(), State.STOPPING);
		Assert.assertEquals(observer.observedStateChanges.get(3).getKey(), State.STOPPING);
		Assert.assertEquals(observer.observedStateChanges.get(3).getValue(), State.STOPPPED);
	}
	
	@Test
	public void checkIgnoreRequestFeature() {
		
		TestTask task = new TestTask(1);
		task.requestStart();
		task.awaitEnd();
		
		try {
			task.requestPause();
			Assert.fail("This should cause an exception");
		} catch (IllegalStateException e) {}
		
		try {
			task.requestStop();
			Assert.fail("This should cause an exception");
		} catch (IllegalStateException e) {}

		try {
			task.requestStart();
			Assert.fail("This should cause an exception");
		} catch (IllegalStateException e) {}

		try {
			task.requestResume();
			Assert.fail("This should cause an exception");
		} catch (IllegalStateException e) {}

		task.setIgnoreRequestsInFinalState(true);
		
		Assert.assertEquals(Task.State.FINISHED, task.getCurrentState());

		task.requestPause();
		Assert.assertEquals(Task.State.FINISHED, task.getCurrentState());

		task.requestResume();
		Assert.assertEquals(Task.State.FINISHED, task.getCurrentState());

		task.requestStart();
		Assert.assertEquals(Task.State.FINISHED, task.getCurrentState());

		task.requestStop();
		Assert.assertEquals(Task.State.FINISHED, task.getCurrentState());
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
	
	private static class TestStateObserver implements StateObserver {

		private List<Map.Entry<State, State>> observedStateChanges = new LinkedList<>();
		
		@Override
		public void stateChanged(Task task, State oldState, State newState) {
			observedStateChanges.add(new AbstractMap.SimpleImmutableEntry<State, State>(oldState, newState));
		}
		
	}
}

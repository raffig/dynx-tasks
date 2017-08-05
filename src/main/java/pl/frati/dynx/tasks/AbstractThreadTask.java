package pl.frati.dynx.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Base class for implementing tasks that are executed within single thread.
 * </p>
 * 
 * <p>
 * This base implementation allows task to be started, stopped, paused and
 * resumed. To use those features design your task the way it can be splitted in
 * as many <em>portions</em> as it is possible. Then implement the
 * {@link #hasNextPortion()} method so it returns true if there are any more
 * <em>portions</em> to be executed. Next implement the
 * {@link #executeNextPortion()} method that it executes next <em>portion</em>
 * of task.
 * </p>
 * 
 * <p>
 * After executing a <em>portion</em> some checks are performed. If
 * {@link #requestStop() stop} or {@link #requestPause() pause} were requested
 * then task enters appropriate state ({@link State#STOPPPED} or
 * {@link State#PAUSED} respectively).
 * </p>
 * 
 * <p>
 * For monitoring current state of the task use
 * {@link #addStateObserver(pl.frati.dynx.tasks.Task.StateObserver) state
 * observer}.
 * </p>
 * 
 * <p>
 * Single threaded tasks may be used directly or in combination with
 * {@link ThreadedTask} that allows task execution using many threads.
 * </p>
 * 
 * @author Rafal Figas
 *
 */
public abstract class AbstractThreadTask implements ThreadTask {

	private String id;
	private ReentrantReadWriteLock currentStateLock = new ReentrantReadWriteLock();
	private Task.State currentState = Task.State.NOT_STARTED;
	private List<StateObserver> stateObservers = new ArrayList<>(1);

	public AbstractThreadTask() {
		this(UUID.randomUUID().toString());
	}

	public AbstractThreadTask(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	private void notifyObservers(Task.State oldState, Task.State newState) {
		stateObservers.forEach(so -> so.stateChanged(this, oldState, newState));
	}

	protected abstract boolean hasNextPortion();

	protected abstract void executeNextPortion();

	@Override
	public void requestStart() {

		Task.State oldState = null;

		currentStateLock.writeLock().lock();
		try {
			if (!State.NOT_STARTED.equals(currentState)) {
				throw new IllegalStateException("Task must be in state " + State.NOT_STARTED
						+ " for requestStart(). Task is currently in " + currentState.name() + " state");
			}
			oldState = currentState;
			currentState = Task.State.STARTING;
		} finally {
			currentStateLock.writeLock().unlock();
		}

		notifyObservers(oldState, currentState);

		Thread t = new Thread(this, "thread-" + getId());
		t.start();
	}

	@Override
	public void run() {
		boolean stateChanged = false;
		Task.State oldState = null;
		try {
			currentStateLock.writeLock().lock();
			try {

				if (!State.STARTING.equals(currentState)) {
					throw new IllegalStateException("Task must be in state " + State.STARTING.name()
							+ " for run() method. Current task status is: " + currentState.name());
				}
				oldState = currentState;
				currentState = Task.State.RUNNING;
				stateChanged = true;
			} finally {
				currentStateLock.writeLock().unlock();
			}

			notifyObservers(oldState, currentState);

			stateChanged = false;

			while (hasNextPortion()) {

				executeNextPortion();

				oldState = null;
				stateChanged = false;

				currentStateLock.writeLock().lock();
				try {
					if (Task.State.PAUSING.equals(currentState)) {
						oldState = currentState;
						currentState = Task.State.PAUSED;
						stateChanged = true;
					} else if (Task.State.STOPPING.equals(currentState)) {
						oldState = currentState;
						currentState = Task.State.STOPPPED;
						stateChanged = true;
					}
				} finally {
					currentStateLock.writeLock().unlock();
				}

				if (stateChanged) {
					notifyObservers(oldState, currentState);
				}

				if (Task.State.PAUSED.equals(getCurrentState())) {
					synchronized (this) {
						wait();
					}
				}

				if (Task.State.STOPPPED.equals(getCurrentState())) {
					return;
				}
			}

			stateChanged = false;

			currentStateLock.writeLock().lock();
			try {
				oldState = currentState;
				currentState = Task.State.FINISHED;
				stateChanged = true;
			} finally {
				currentStateLock.writeLock().unlock();
			}

			if (stateChanged) {
				notifyObservers(oldState, currentState);
			}

		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted!", e);
		}
	}

	@Override
	public void requestPause() {

		boolean stateChanged = false;
		Task.State oldState = null;

		currentStateLock.writeLock().lock();
		try {
			if (!State.RUNNING.equals(currentState) && !State.STARTING.equals(currentState)) {
				throw new IllegalStateException("Task must be in state " + State.RUNNING + " or " + State.STARTING
						+ " so it can be paused. Current task state is: " + currentState);
			}
			oldState = currentState;
			currentState = Task.State.PAUSING;
			stateChanged = true;
		} finally {
			currentStateLock.writeLock().unlock();
		}

		if (stateChanged) {
			notifyObservers(oldState, currentState);
		}
	}

	@Override
	public void requestStop() {

		boolean stateChanged = false;
		Task.State oldState = null;

		currentStateLock.writeLock().lock();
		try {
			if (!State.STARTING.equals(currentState) && !State.RUNNING.equals(currentState)) {
				throw new IllegalStateException("Task must be in state " + State.STARTING + " or " + State.RUNNING
						+ " for requestStop(). Task is currently in " + currentState.name() + " state");
			}
			oldState = currentState;
			currentState = Task.State.STOPPING;
			stateChanged = true;
		} finally {
			currentStateLock.writeLock().unlock();
		}

		if (stateChanged) {
			notifyObservers(oldState, currentState);
		}
	}

	@Override
	public void requestResume() {

		boolean stateChanged = false;
		Task.State oldState = null;

		currentStateLock.writeLock().lock();
		try {
			if (!State.PAUSING.equals(currentState) && !State.PAUSED.equals(currentState)) {
				throw new IllegalStateException("Task must be in state " + State.PAUSING + " or " + State.PAUSED
						+ " so it can be paused. Current task state is: " + currentState);
			}

			oldState = currentState;
			currentState = Task.State.RUNNING;
			stateChanged = true;
		} finally {
			currentStateLock.writeLock().unlock();
		}

		if (stateChanged) {
			notifyObservers(oldState, currentState);
		}

		synchronized (this) {
			this.notify();
		}
	}

	@Override
	public void addStateObserver(StateObserver observer) {
		stateObservers.add(observer);
	}

	@Override
	public Task.State getCurrentState() {
		currentStateLock.readLock().lock();
		try {
			return currentState;
		} finally {
			currentStateLock.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		return "Task [" + getId() + "]: state=" + currentState;
	}

}

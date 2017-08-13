package pl.frati.dynx.tasks;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pl.frati.dynx.tasks.Task.State;

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
 * {@link ParalleledTask} that allows task execution using many threads.
 * </p>
 * 
 * @author Rafal Figas
 *
 */
public abstract class AbstractThreadTask implements ThreadTask {

	private String id;

	private Date requestStartTime;
	private Date actualStartTime;
	private Date endTime;

	private Thread executionThread;

	private ReentrantReadWriteLock currentStateLock = new ReentrantReadWriteLock();
	private Task.State currentState = Task.State.NOT_STARTED;
	private List<StateObserver> stateObservers = new ArrayList<>(1);
	private Exception failCause;

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

	@Override
	public Optional<Date> getRequestStartTime() {
		return Optional.ofNullable(requestStartTime);
	}

	@Override
	public Optional<Date> getActualStartTime() {
		return Optional.ofNullable(actualStartTime);
	}

	@Override
	public Optional<Date> getEndTime() {
		return Optional.ofNullable(endTime);
	}

	private void notifyObservers(Task.State oldState, Task.State newState) {
		stateObservers.forEach(so -> so.stateChanged(this, oldState, newState));
	}

	@Override
	public boolean isInProgress() {
		return Arrays.asList(State.STARTING, State.RUNNING, State.PAUSING, State.PAUSED, State.STOPPING)
				.contains(currentState);
	}

	@Override
	public boolean isEnded() {
		return Arrays.asList(State.FINISHED, State.FAILED, State.STOPPPED).contains(currentState);
	}

	protected abstract boolean hasNextPortion();

	protected abstract void executeNextPortion() throws InterruptedException, InterruptedIOException;

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
			requestStartTime = new Date();
		} finally {
			currentStateLock.writeLock().unlock();
		}

		notifyObservers(oldState, currentState);

		executionThread = new Thread(this, "thread-" + getId());
		executionThread.start();
	}

	@Override
	public void run() {
		boolean stateChanged = false;
		Task.State oldState = null;

		currentStateLock.writeLock().lock();
		try {

			if (!State.STARTING.equals(currentState)) {
				throw new IllegalStateException("Task must be in state " + State.STARTING.name()
						+ " for run() method. Current task status is: " + currentState.name());
			}
			oldState = currentState;
			currentState = Task.State.RUNNING;
			actualStartTime = new Date();
			stateChanged = true;
		} finally {
			currentStateLock.writeLock().unlock();
		}

		notifyObservers(oldState, currentState);

		oldState = null;
		stateChanged = false;

		while (true) {

			try {
				if (hasNextPortion()) {
					executeNextPortion();
				} else {
					break;
				}
			} catch (InterruptedException | InterruptedIOException e) {

				oldState = currentState;
				currentStateLock.writeLock().lock();
				try {
					currentState = State.STOPPING;
					stateChanged = true;
				} finally {
					currentStateLock.writeLock().unlock();
				}

			} catch (Exception e) {
				failCause = e;

				oldState = currentState;
				currentStateLock.writeLock().lock();
				try {
					currentState = State.FAILED;
					endTime = new Date();
					stateChanged = true;
				} finally {
					currentStateLock.writeLock().unlock();
				}
			}

			currentStateLock.writeLock().lock();
			try {
				if (Task.State.PAUSING.equals(currentState)) {
					oldState = currentState;
					currentState = Task.State.PAUSED;
					stateChanged = true;
				} else if (Task.State.STOPPING.equals(currentState)) {
					oldState = currentState;
					currentState = Task.State.STOPPPED;
					endTime = new Date();
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
					try {
						wait();
					} catch (InterruptedException e) {

						oldState = currentState;
						currentStateLock.writeLock().lock();
						try {
							currentState = State.STOPPPED;
							notifyObservers(oldState, currentState);
						} finally {
							currentStateLock.writeLock().unlock();
						}

					}
				}
			} 
			
			if (Task.State.STOPPPED.equals(getCurrentState())) {
				return;
			} else if (State.FAILED.equals(currentState)) {
				return;
			}
		}

		currentStateLock.writeLock().lock();
		try {
			oldState = currentState;
			currentState = Task.State.FINISHED;
			endTime = new Date();
			stateChanged = true;
		} finally {
			currentStateLock.writeLock().unlock();
		}

		if (stateChanged) {
			notifyObservers(oldState, currentState);
		}

	}

	@Override
	public Optional<Exception> getFailCause() {
		return Optional.ofNullable(failCause);
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
	public void awaitEnd() {
		try {
			executionThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return "Task [" + getId() + "]: state=" + currentState;
	}

}

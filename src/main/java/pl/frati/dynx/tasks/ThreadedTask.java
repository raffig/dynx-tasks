package pl.frati.dynx.tasks;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ThreadedTask implements Task {

	private String name;
	private ReentrantReadWriteLock stateChangeLock = new ReentrantReadWriteLock();
	private State currentState;
	private List<ThreadTask> tasks = new CopyOnWriteArrayList<>();
	private List<StateObserver> stateObservers = new CopyOnWriteArrayList<>();

	public ThreadedTask() {
		this(UUID.randomUUID().toString());
	}

	public ThreadedTask(String name) {
		super();
		this.name = name;
		currentState = State.NOT_STARTED;
	}

	@Override
	public String getName() {
		return name;
	}

	private void updateStateUponTasksStates() {
		stateChangeLock.writeLock().lock();

		try {

			if (State.PAUSING.equals(currentState)) {
				if (tasks.stream().allMatch(t -> State.PAUSED.equals(t.getCurrentState()))) {
					currentState = State.PAUSED;
				}
			}

			if (State.STOPPING.equals(currentState)) {
				if (tasks.stream().allMatch(t -> State.STOPPPED.equals(t.getCurrentState()))) {
					currentState = State.STOPPPED;
				}
			}

			if (tasks.stream().allMatch(t -> State.FINISHED.equals(t.getCurrentState()))) {
				currentState = State.FINISHED;
			}

		} finally {
			stateChangeLock.writeLock().unlock();
		}
	}

	public void addThreadTask(ThreadTask task) {
		tasks.add(task);
		task.addStateObserver(new TaskStateObserver());
	}

	public void addStateObserver(StateObserver observer) {
		stateObservers.add(observer);
	}

	@Override
	public State getCurrentState() {
		try {
			stateChangeLock.readLock().lock();
			return currentState;
		} finally {
			stateChangeLock.readLock().unlock();
		}

	}

	@Override
	public void requestStart() {
		if (!State.NOT_STARTED.equals(currentState)) {
			throw new IllegalStateException("Only manager that is in " + State.NOT_STARTED
					+ " state may be started. Manager is currently in " + currentState.name() + " state.");
		}
		try {

			stateChangeLock.writeLock().lock();

			tasks.forEach(ThreadTask::requestStart);
			currentState = State.RUNNING;
		} finally {
			stateChangeLock.writeLock().unlock();
		}
	}

	@Override
	public void requestPause() {

		if (!State.RUNNING.equals(currentState)) {
			throw new IllegalStateException("Only manager that is in " + State.RUNNING
					+ " state may be paused. Manager is currently in " + currentState.name() + " state.");
		}

		try {

			stateChangeLock.writeLock().lock();

			currentState = State.PAUSING;
			tasks.forEach(ThreadTask::requestPause);
		} finally {
			stateChangeLock.writeLock().unlock();
		}
	}

	@Override
	public void requestStop() {
		if (!State.RUNNING.equals(currentState)) {
			throw new IllegalStateException("Only manager that is in " + State.RUNNING
					+ " state may be stopped. Manager is currently in " + currentState.name() + " state.");
		}

		try {

			stateChangeLock.writeLock().lock();

			currentState = State.STOPPING;
			tasks.forEach(ThreadTask::requestStop);
		} finally {
			stateChangeLock.writeLock().unlock();
		}
	}

	@Override
	public void requestResume() {

		if (!State.PAUSING.equals(currentState) && !State.PAUSED.equals(currentState)) {
			throw new IllegalStateException("Only manager that is in " + State.PAUSING + " or " + State.PAUSED
					+ " state may be resumed. Manager is currently in " + currentState.name() + " state.");
		}

		try {

			stateChangeLock.writeLock().lock();

			tasks.forEach(Task::requestResume);
			currentState = State.RUNNING;
		} finally {
			stateChangeLock.writeLock().unlock();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("ThreadedTask [");
		sb.append(name);
		sb.append("] status=");
		sb.append(currentState);
		sb.append(" {");

		sb.append(String.join(", ", tasks.stream().map(Task::toString).collect(Collectors.toList())));

		sb.append("}");

		return sb.toString();
	}

	private final class TaskStateObserver implements Task.StateObserver {

		@Override
		public void stateChanged(Task task, State oldState, State newState) {
			updateStateUponTasksStates();
		}

	}
}

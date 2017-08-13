package pl.frati.dynx.tasks;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ParalleledTask implements Task {

	private String id;
	
	private Date requestStartTime;
	private Date actualStartTime;
	private Date endTime;
	
	private ReentrantReadWriteLock stateChangeLock = new ReentrantReadWriteLock();
	private State currentState;
	private List<Task> tasks = new CopyOnWriteArrayList<>();
	private List<StateObserver> stateObservers = new CopyOnWriteArrayList<>();
	private boolean finishingWithFail;
	private Exception failCause;

	public ParalleledTask() {
		this(UUID.randomUUID().toString());
	}

	public ParalleledTask(String name) {
		super();
		this.id = name;
		currentState = State.NOT_STARTED;
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
	
	@Override
	public void awaitEnd() {
		tasks.forEach(Task::awaitEnd);
	}
	
	@Override
	public Optional<Exception> getFailCause() {
		return Optional.ofNullable(failCause);
	}

	@Override
	public boolean isInProgress() {
		return Arrays.asList(State.STARTING, State.RUNNING, State.PAUSING, State.PAUSED, State.STOPPING).contains(currentState);
	}
	
	@Override
	public boolean isEnded() {
		return Arrays.asList(State.FINISHED, State.FAILED, State.STOPPPED).contains(currentState);
	}
	
	public List<Task> getSubTasks() {
		return tasks;
	}

	private void endTask(State finalState) {
		currentState = finalState;
		endTime = new Date();
	}
	
	private void updateStateUponTasksStates() {
		stateChangeLock.writeLock().lock();

		try {

			if (finishingWithFail) {

				if (tasks.stream().allMatch(t -> {
					return State.FAILED.equals(t.getCurrentState()) || State.FINISHED.equals(t.getCurrentState()) || State.NOT_STARTED.equals(t.getCurrentState()) || State.STOPPPED.equals(t.getCurrentState());
				})) {
					endTask(State.FAILED);
				}
			} else {
				if (State.PAUSING.equals(currentState)) {
					if (tasks.stream().allMatch(t -> State.PAUSED.equals(t.getCurrentState()))) {
						currentState = State.PAUSED;
					}
				}

				if (State.STOPPING.equals(currentState)) {
					if (tasks.stream().allMatch(t -> State.STOPPPED.equals(t.getCurrentState()))) {
						endTask(State.STOPPPED);
					}
				}

				if (tasks.stream().allMatch(t -> State.FINISHED.equals(t.getCurrentState()))) {
					endTask(State.FINISHED);
				}
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
			throw new IllegalStateException("Only task that is in " + State.NOT_STARTED
					+ " state may be started. Task is currently in " + currentState.name() + " state.");
		}
		try {

			stateChangeLock.writeLock().lock();
			requestStartTime = new Date();

			tasks.forEach(Task::requestStart);
			currentState = State.RUNNING;
			actualStartTime = new Date();
		} finally {
			stateChangeLock.writeLock().unlock();
		}
	}

	@Override
	public void requestPause() {

		if (!State.RUNNING.equals(currentState)) {
			throw new IllegalStateException("Only task that is in " + State.RUNNING
					+ " state may be paused. Manager is currently in " + currentState.name() + " state.");
		}

		try {

			stateChangeLock.writeLock().lock();

			currentState = State.PAUSING;
			tasks.forEach(Task::requestPause);
		} finally {
			stateChangeLock.writeLock().unlock();
		}
	}

	@Override
	public void requestStop() {
		if (!State.RUNNING.equals(currentState) && (!State.STARTING.equals(currentState))) {
			throw new IllegalStateException("Only task that is in " + State.RUNNING + " or " + State.STARTING
					+ " state may be stopped. Task is currently in " + currentState.name() + " state.");
		}

		try {

			stateChangeLock.writeLock().lock();

			currentState = State.STOPPING;
			tasks.forEach(Task::requestStop);
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
		sb.append(id);
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

			if (State.FAILED.equals(newState)) {
				task.getFailCause().ifPresent(e -> failCause = e);
				finishingWithFail = true;
				tasks.forEach(t -> {
					if (State.PAUSED.equals(t.getCurrentState()) || State.PAUSING.equals(t.getCurrentState())) {
						t.requestResume();
						t.requestStop();
					} else if (State.RUNNING.equals(t.getCurrentState())
							|| State.STARTING.equals(t.getCurrentState())) {
						t.requestStop();
					}
				});
			}
			updateStateUponTasksStates();
		}

	}
}

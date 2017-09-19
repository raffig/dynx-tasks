package pl.frati.dynx.tasks;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import pl.frati.dynx.tasks.Task.State;

public class ParalleledTask implements Task {

	public static final List<State> FINAL_STATES = Arrays.asList(State.FINISHED, State.FAILED, State.STOPPPED);

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
	
	protected void setFailCause(Exception failCause) {
		this.failCause = failCause;
	}

	/**
	 * <p>
	 * Method performs transition to given final state.
	 * </p>
	 * 
	 * <p>
	 * This method is designed only for a special case - when task implementor
	 * knows that task has nothing to do and wants to go directly from
	 * {@link State#NOT_STARTED} state to some {@link #FINAL_STATES final state}
	 * this method should be used. Note that this method will ommit all states
	 * related to task in progress and will go directly from
	 * {@link State#NOT_STARTED} state to given final state.
	 * </p>
	 * 
	 * @param finalState
	 *            one of the {@link #FINAL_STATES final states}
	 * 
	 * @throws IllegalArgumentException
	 *             thrown if given final state is not one of
	 *             {@link #FINAL_STATES final states}
	 * @throws IllegalStateException
	 *             thrown when task is in state other than
	 *             {@link State#NOT_STARTED}
	 */
	protected void endTask(State finalState) {
		if (!FINAL_STATES.contains(finalState)) {
			throw new IllegalArgumentException("Given state must be final (one of: "
					+ String.join(", ", FINAL_STATES.stream().map(Enum::toString).collect(Collectors.toList())) + ")");
		}

		stateChangeLock.writeLock().lock();

		try {
			if (!State.NOT_STARTED.equals(currentState)) {
				throw new IllegalStateException("This method may be executed only when task is in "
						+ State.NOT_STARTED.name() + " state. Current task state is: " + currentState.name());
			}

			requestStartTime = new Date();
			actualStartTime = new Date();
			currentState = finalState;
			endTime = new Date();
		} finally {
			stateChangeLock.writeLock().unlock();
		}

		notifyObservers(State.NOT_STARTED, finalState);
	}

	@Override
	public boolean isInProgress() {
		return Arrays.asList(State.STARTING, State.RUNNING, State.PAUSING, State.PAUSED, State.STOPPING)
				.contains(currentState);
	}

	@Override
	public boolean isEnded() {
		return FINAL_STATES.contains(currentState);
	}

	public List<Task> getSubTasks() {
		return tasks;
	}

	private void endTaskInternal(State finalState) {
		currentState = finalState;
		endTime = new Date();
	}

	private void updateStateUponTasksStates() {
		stateChangeLock.writeLock().lock();

		State oldState = currentState;
		State newState = null;

		try {

			if (finishingWithFail) {

				if (tasks.stream().allMatch(t -> {
					return State.FAILED.equals(t.getCurrentState()) || State.FINISHED.equals(t.getCurrentState())
							|| State.NOT_STARTED.equals(t.getCurrentState())
							|| State.STOPPPED.equals(t.getCurrentState());
				})) {
					endTaskInternal(State.FAILED);
				}

			} else {
				if (State.PAUSING.equals(currentState)) {
					if (tasks.stream().allMatch(t -> State.PAUSED.equals(t.getCurrentState()))) {
						currentState = State.PAUSED;
					}
				}

				if (State.STOPPING.equals(currentState)) {
					if (tasks.stream().allMatch(t -> State.STOPPPED.equals(t.getCurrentState()))) {
						endTaskInternal(State.STOPPPED);
					}
				}

				if (tasks.stream().allMatch(t -> State.FINISHED.equals(t.getCurrentState()))) {
					endTaskInternal(State.FINISHED);
				}
			}

			newState = currentState;

		} finally {
			stateChangeLock.writeLock().unlock();
		}

		if (!oldState.equals(newState)) {
			notifyObservers(oldState, newState);
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

	/**
	 * <p>
	 * Requests task start.
	 * </p>
	 * 
	 * <p>
	 * Method requests start of all {@link #getSubTasks() subtasks} and
	 * transists the task to {@link State#RUNNING} state. If there are no
	 * subtasks then the task goes immadiately to {@link State#FINISHED} state.
	 * </p>
	 */
	@Override
	public void requestStart() {
		try {
			stateChangeLock.writeLock().lock();

			if (!State.NOT_STARTED.equals(currentState)) {
				throw new IllegalStateException("Only task that is in " + State.NOT_STARTED
						+ " state may be started. Task is currently in " + currentState.name() + " state.");
			}

			requestStartTime = new Date();

			if (tasks.isEmpty()) {
				actualStartTime = new Date();
				endTask(State.FINISHED);
			} else {
				tasks.forEach(Task::requestStart);
				currentState = State.RUNNING;
				actualStartTime = new Date();
			}
		} finally {
			stateChangeLock.writeLock().unlock();
		}

		notifyObservers(State.NOT_STARTED, State.RUNNING);
	}

	@Override
	public void requestPause() {

		try {

			stateChangeLock.writeLock().lock();

			if (!State.RUNNING.equals(currentState)) {
				throw new IllegalStateException("Only task that is in " + State.RUNNING
						+ " state may be paused. Manager is currently in " + currentState.name() + " state.");
			}

			currentState = State.PAUSING;
			tasks.forEach(Task::requestPause);
		} finally {
			stateChangeLock.writeLock().unlock();
		}

		notifyObservers(State.RUNNING, State.PAUSING);
	}

	@Override
	public void requestStop() {

		State oldState = null;
		try {

			stateChangeLock.writeLock().lock();

			if (!State.RUNNING.equals(currentState) && (!State.STARTING.equals(currentState))) {
				throw new IllegalStateException("Only task that is in " + State.RUNNING + " or " + State.STARTING
						+ " state may be stopped. Task is currently in " + currentState.name() + " state.");
			}

			oldState = currentState;
			currentState = State.STOPPING;
			tasks.forEach(Task::requestStop);
		} finally {
			stateChangeLock.writeLock().unlock();
		}

		notifyObservers(oldState, State.STOPPING);
	}

	@Override
	public void requestResume() {

		State oldState = null;
		try {

			stateChangeLock.writeLock().lock();

			if (!State.PAUSING.equals(currentState) && !State.PAUSED.equals(currentState)) {
				throw new IllegalStateException("Only manager that is in " + State.PAUSING + " or " + State.PAUSED
						+ " state may be resumed. Manager is currently in " + currentState.name() + " state.");
			}

			tasks.forEach(Task::requestResume);
			oldState = currentState;
			currentState = State.RUNNING;
		} finally {
			stateChangeLock.writeLock().unlock();
		}

		notifyObservers(oldState, State.RUNNING);
	}

	private void notifyObservers(Task.State oldState, Task.State newState) {
		stateObservers.forEach(so -> so.stateChanged(this, oldState, newState));
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

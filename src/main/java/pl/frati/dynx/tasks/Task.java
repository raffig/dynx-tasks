package pl.frati.dynx.tasks;

import java.util.Optional;

/**
 * <p>
 * General interface of managable task.
 * </p>
 * 
 * @author Rafal Figas
 *
 */
public interface Task {

	/**
	 * <p>
	 * Gets current state of a task.
	 * </p>
	 * 
	 * @return Current state (never null)
	 */
	State getCurrentState();

	/**
	 * <p>
	 * Gets task identifier.
	 * </p>
	 * 
	 * @return Task identifier that is constant through whole task execution.
	 */
	String getId();

	/**
	 * <p>
	 * Requests task execution start.
	 * </p>
	 * 
	 * <p>
	 * After creation task is in {@link State#NOT_STARTED} state. Execution of
	 * task should not be started until this method is called. This method
	 * transists task to {@link State#STARTING} state.
	 * </p>
	 * 
	 * <p>
	 * Note that calling this method is not equivalent to beginning of
	 * execution. This is just a signal that execution is now allowed. It is up
	 * to implementation when the execution begins.
	 * </p>
	 * 
	 * <p>
	 * Once the task start has been requested task may be {@link #requestPause()
	 * paused} or {@link #requestStop() stopped}.
	 * </p>
	 * 
	 * @throws IllegalStateException
	 *             thrown if task is other state than {@link State#NOT_STARTED}
	 */
	void requestStart();

	/**
	 * <p>
	 * Requests task execution pause.
	 * </p>
	 * 
	 * <p>
	 * Task that is in {@link State#STARTING} or {@link State#RUNNING} state may
	 * be temporarily paused. This method will request task to pause. Task will
	 * be put in {@link State#PAUSING} which is a signal for task to pause.
	 * After actually pausing task will enter {@link State#PAUSED} state. To
	 * continue execution use {@link #requestResume()} method.
	 * </p>
	 */
	void requestPause();

	/**
	 * <p>
	 * Resume task that has been paused.
	 * </p>
	 * 
	 * <p>
	 * If the task is in {@link State#PAUSING} or {@link State#PAUSED} state its
	 * execution may be resumed with this method. After calling this method task
	 * will enter {@link State#RUNNING} state.
	 * </p>
	 */
	void requestResume();

	/**
	 * <p>
	 * Requests task to stop its execution.
	 * </p>
	 * 
	 * <p>
	 * This method puts task in {@link State#STOPPING} state, which is a signal
	 * for task to stop at first possible moment. Task that has been requested
	 * to stop will immediately stop and cannot be continued. When task actually
	 * stops it will enter {@link State#STOPPPED} state which is terminal state.
	 * </p>
	 */
	void requestStop();

	/**
	 * <p>
	 * Adds task state observer.
	 * </p>
	 * 
	 * @param observer
	 *            observer to be added (never null)
	 */
	void addStateObserver(StateObserver observer);

	/**
	 * <p>
	 * Returns exception that caused the task to fail.
	 * </p>
	 * 
	 * <p>
	 * Note that exception may not be present even when task fails (is in
	 * {@link State#FAILED}, as other circumstances may cause a task to fail.
	 * </p>
	 * 
	 * @return Optional Exception causing task fail
	 */
	Optional<Exception> getFailCause();

	/**
	 * <p>
	 * Interface of {@link Task#getCurrentState() task's current state}
	 * observer.
	 * </p>
	 * 
	 * @author Rafal Figas
	 *
	 */
	public interface StateObserver {

		/**
		 * <p>
		 * Method called when given task state changes.
		 * </p>
		 * 
		 * @param task
		 *            task whose state changed
		 * @param oldState
		 *            old state of task
		 * @param newState
		 *            new state of tasks
		 */
		public void stateChanged(Task task, State oldState, State newState);
	}

	/**
	 * <p>
	 * Enumeration of possible {@link Task#getCurrentState() states} of
	 * {@link Task}.
	 * </p>
	 * 
	 * @author Rafal Figas
	 *
	 */
	public enum State {

		/**
		 * <p>
		 * First state of newly created task.
		 * </p>
		 */
		NOT_STARTED,

		/**
		 * <p>
		 * State of task that has been {@link Task#requestStart() requested to
		 * start}, but actual execution has not begun yet.
		 * </p>
		 */
		STARTING,
		/**
		 * <p>
		 * State of task that execution is in progress.
		 * </p>
		 */
		RUNNING,

		/**
		 * <p>
		 * State of task that has been {@link Task#requestPause() requested to
		 * pause}, but has not been actually paused yet.
		 * </p>
		 */
		PAUSING,

		/**
		 * <p>
		 * State of task that execution is currently paused.
		 * </p>
		 */
		PAUSED,

		/**
		 * <p>
		 * State when task {@link Task#requestStop() has been requested to
		 * stop}, but has not been actually stopped yet.
		 * </p>
		 */
		STOPPING,

		/**
		 * <p>
		 * State of task which execution has been definitely stopped.
		 * </p>
		 */
		STOPPPED,

		/**
		 * <p>
		 * State of successfully finished task.
		 * </p>
		 */
		FINISHED,

		/**
		 * <p>
		 * State of task that execution has been failed.
		 * </p>
		 */
		FAILED,
	}

}

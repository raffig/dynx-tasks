package pl.frati.dynx.tasks;

/**
 * <p></p>
 * 
 * @author Rafal Figas
 *
 */
public interface Task {

	String getName();
	
	void requestStart();

	void requestPause();

	void requestStop();

	void requestResume();
	
	void addStateObserver(StateObserver observer);
	
	State getCurrentState();

	public interface StateObserver {
		public void stateChanged(Task task, State oldState, State newState);
	}
	
	public enum State {

		NOT_STARTED,
		RUNNING,
		PAUSING,
		PAUSED,
		STOPPING,
		STOPPPED,
		FINISHED,
	}

}

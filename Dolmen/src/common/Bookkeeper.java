package common;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Stack;

/**
 * This is a utility class which helps keep track
 * of a task, possibly split into several subtasks
 * recursively. It keeps timings for the different
 * subtasks and can display progress in a nice
 * textual way.
 * <p>
 * For example, the following program:
 * <pre>
 *  Bookkeeper bk = Bookkeeper.start(System.out, "Cooking meal");
 *  bk.done("Picked recipe");
 *  bk.infos("(Guacamole)");
 *  bk.enter("Gathering ingredients");
 *  bk.done("a ripe avocado");
 *  bk.done("a lime");
 *  bk.done("salt and pepper");
 *  bk.leave();
 *  bk.done("Found mortar and pestle");
 *  bk.infos("(in the cupboard)");
 *  bk.enter("Making the guacamole");
 *  bk.done("Mashed avocado");
 *  bk.done("Added lime juice");
 *  bk.leaveWith("Added seasonings");
 *  bk.done("Served with nachos!");
 *  bk.leave();
 * </pre>
 * will produce something like this on the
 * program's standard output:
 * <pre>
 * Cooking meal
 * ├─ Picked recipe			[1ms]
 * │  (Guacamole)
 * ├─ Gathering ingredients
 * │  ├─ a ripe avocado			[0ms]
 * │  ├─ a lime			[0ms]
 * │  ├─ salt and pepper			[0ms]
 * │  └─ Done in 3ms
 * ├─ Found mortar and pestle			[0ms]
 * │  (in the cupboard)
 * ├─ Making the guacamole
 * │  ├─ Mashed avocado			[0ms]
 * │  ├─ Added lime juice			[0ms]
 * │  ├─ Added seasonings			[0ms]
 * │  └─ Done in 1ms
 * ├─ Served with nachos!			[0ms]
 * └─ Done in 5ms
 * </pre>
 * 
 * A task can also be <i>aborted</i> at any time
 * which results in something like this:
 * <pre>
 * Cooking meal
 * ├─ Picked recipe			[1ms]
 * │  (Guacamole)
 * ├─ Gathering ingredients
 * │  ├─ a ripe avocado			[0ms]
 *    ╧  could not found lime juice
 * <pre>
 * 
 * @author Stéphane Lescuyer
 */
public final class Bookkeeper {
	@SuppressWarnings("unused")
	private final String mainTask;
	private final PrintStream out;
	
	private int depth;
	private String prefix;
	private final Stack<Instant> tasks;
	private boolean aborted;
	
	private Bookkeeper(PrintStream out, String mainTask) {
		this.out = out;
		this.mainTask = mainTask;
		this.depth = 0;
		this.tasks = new Stack<>();
		this.tasks.push(Instant.now());
		this.prefix = "";
		this.aborted = false;
	}
	
	/**
	 * @param out
	 * @param mainTask
	 * @return a new bookkeeper for the task {@code mainTask}
	 * 	which will display progress on {@code out}
	 */
	public static Bookkeeper start(PrintStream out, String mainTask) {
		Bookkeeper bk = new Bookkeeper(out, mainTask);
		bk.enter(mainTask);
		return bk;
	}
	
	/**
	 * Starts a new phase in the current task, called {@code task},
	 * which will be split into several subtasks
	 * 
	 * @param task
	 */
	public void enter(String task) {
		if (aborted) throw new IllegalArgumentException();
		if (depth > 0) out.println("");
		out.print(prefix);
		out.print(task);
		Instant now = Instant.now();

		++this.depth;
		if (!prefix.isEmpty()) {
			prefix = prefix.substring(0, prefix.length() - 3) + "│  "; 
		}
		prefix = prefix + "├─ ";
		tasks.push(now);
	}
	
	/**
	 * Reports that the subtask {@code task} of the current
	 * task has been completed
	 * 
	 * @param task
	 */
	public void done(String task) {
		if (aborted) throw new IllegalArgumentException();
		Instant start = tasks.pop();
		out.println("");
		out.print(prefix);
		out.print(task);
		Instant now = Instant.now();
		Duration elapsed = Duration.between(start, now);
		out.print(String.format("\t\t\t[%dms]", elapsed.toMillis()));
		tasks.push(now);
	}
	
	/**
	 * Adds some infos about the last completed subtask
	 * 
	 * @param infos
	 */
	public void infos(String infos) {
		if (aborted) throw new IllegalArgumentException();
		out.println("");
		out.print(prefix.substring(0, prefix.length() - 3) + "│  ");
		out.print(infos);
	}
	
	/**
	 * Convenient wrapper around {@link #infos(String)} to report
	 * on a certain number of problems found during the last completed subtask
	 * 
	 * @param n
	 */
	public void problems(int n) {
		if (n <= 0) return;
		infos("(" + n + " potential problem" + (n > 1 ? "s" : "") + " found)");
	}
	
	/**
	 * Aborts the current task, and thus this bookkeeper's main
	 * task, for the given {@code reason}.
	 * <b>No more methods can be called on the {@link Bookkeeper}
	 * instance after this.</b>
	 * 
	 * @param reason
	 */
	public void aborted(String reason) {
		if (aborted) throw new IllegalArgumentException();
		out.println("");
		int nblanks = prefix.length() - 3;
		for (int i = 0; i < nblanks; ++i)
			out.print(' ');
		out.print("╧  ");
		out.print(reason);
		out.println();
		this.aborted = true;
	}
	
	/**
	 * Reports that the current task has been completed.
	 * 
	 * Unless this was the main task, this means that the 
	 * current task's parent will now become the current task.
	 */
	public void leave() {
		if (aborted) throw new IllegalArgumentException();
		if (depth == 0) throw new IllegalArgumentException();
		tasks.pop(); 
		Instant start = tasks.pop();
		Instant now = Instant.now();
		Duration elapsed = Duration.between(start, now);

		out.println("");
		prefix = prefix.substring(0, prefix.length() - 3);
		out.print(prefix); out.print("└─ ");
		out.print(String.format("Done in %dms", elapsed.toMillis()));
		if (depth == 1)
			out.println("");
		
		--this.depth;
		if (!prefix.isEmpty()) {
			prefix = prefix.substring(0, prefix.length() - 3) + "├─ ";
		}
		tasks.push(now);
	}
	
	/**
	 * This is equivalent to:
	 * <pre>
	 *  done(task); leave();
	 * </pre>
	 * In other words, this reports that the subtask {@code task}
	 * has been completed and that it was the last subtask of
	 * the current task.
	 * 
	 * @param task
	 */
	public void leaveWith(String task) {
		done(task);
		leave();
	}

	/**
	 * A method to test {@link Bookkeeper}
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
//		Bookkeeper bk = Bookkeeper.start(System.out, "Cooking meal");
//		bk.done("Picked recipe");
//		bk.infos("(Guacamole)");
//		bk.enter("Gathering ingredients");
//		bk.done("a ripe avocado");
//		bk.aborted("could not found lime juice");
//		bk.done("a lime");
//		bk.done("salt and pepper");
//		bk.leave();
//		bk.done("Found mortar and pestle");
//		bk.infos("(in the cupboard)");
//		bk.enter("Making the guacamole");
//		bk.done("Mashed avocado");
//		bk.done("Added lime juice");
//		bk.leaveWith("Added seasonings");
//		bk.done("Served with nachos!");
//		bk.leave();
		
		Bookkeeper bk = Bookkeeper.start(System.out, "Super task A");
		Thread.sleep(100);
		bk.done("First step A.1");
		bk.infos("(infos are a cool feature)");
		Thread.sleep(50);
		bk.done("Second step A.2");
		bk.enter("Starting big task A.3");
		bk.done("Well it was easy");
		bk.leave();
		Thread.sleep(75);
		bk.done("Fourth step A.4");
		bk.enter("Starting big task A.5");
		bk.enter("Starting medium task A.5.a)");
		bk.done("OK I see");
		bk.infos("(turns out)");
		bk.done("Yes, it was easy");
		bk.infos("(it could have been easier)");
		bk.infos("(but i'm not complaining)");
		bk.leave();
		Thread.sleep(50);
		bk.done("Not so easy this time A.5.b)");
		Thread.sleep(50);
		bk.done("I told you I was freaky A.5.c");
		bk.done("You didn't believe me A.5.d)");
		bk.leave();
		bk.done("Sixth step A.6");
		bk.leave();
	}
}
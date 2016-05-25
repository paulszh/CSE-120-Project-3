package nachos.threads;

import nachos.machine.*;
import java.util.*;
import java.util.Comparator;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */

	PriorityQueue<wrapThread> pq;
	public Alarm() {
		Comparator<wrapThread> cmp = new wakeTimeComparator();
		pq = new PriorityQueue<wrapThread>(11, cmp);
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		long threshold_time = Machine.timer().getTime();
        
       /* if(pq.size() == 0){
            return;
        }
        if ( pq.peek().currTime > threshold_time){
	        return;
        }*/

		while(pq.size() != 0){

			if(threshold_time >= pq.peek().currTime){
				pq.poll().currThread.ready();
			}
            else{
                break;
            }
		}

		KThread.currentThread().yield();


	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeUpTime = Machine.timer().getTime() + x;
		// guarantees atomicity
		boolean initStatus = Machine.interrupt().disable();
		// use data structure to store both time and waittime
        //System.out.println("Wait thread " + KThread.currentThread().getName() +" until " + wakeUpTime);

		pq.add(new wrapThread(KThread.currentThread(), wakeUpTime));
    	//System.out.println(pq.peek().currThread.getName());
        KThread.currentThread().sleep();
		Machine.interrupt().restore(initStatus);
	}
	public class wrapThread{
		KThread currThread;
		long currTime;
		private wrapThread(KThread k, long t){
			this.currThread = k;
			this.currTime = t;
		}
	}
	public class wakeTimeComparator implements Comparator<wrapThread>
	{
		// Comparing the wake up time during initialization
		@Override
		public int compare(wrapThread x, wrapThread y){
			if(x.currTime < y.currTime){
                return -1;
            }
            if(x.currTime > y.currTime){
                return 1;
            }
            return 0;
			
		}
	}
		// Place this function inside Alarm. And make sure Alarm.selfTest() is called inside ThreadedKernel.selfTest() method.
	public static void selftest() {
    	KThread t1 = new KThread(new Runnable() {
        	public void run() {
            	long time1 = Machine.timer().getTime();
            	int waitTime = 10000;
            	System.out.println("Thread calling wait at time:" + time1);
            	ThreadedKernel.alarm.waitUntil(waitTime);
            	System.out.println("Thread woken up after:" + (Machine.timer().getTime() - time1));
            	Lib.assertTrue((Machine.timer().getTime() - time1) >= waitTime, " thread woke up too early.");
        	}
    	});
    	t1.setName("T1");
    	t1.fork();
    	t1.join();
    	System.out.println("Alarm test succesfully exited....");
	}
}

package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
		memoryLock = new Lock();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	public int pageFaultHandler(){
		//freePageList is not empty
		if(freeList.size() != 0){
			int ppn = ((Integer)UserKernel.freeList.removeFirst()).intValue();
		}
		else{

		}

	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	//global swap file accross all the processes
	public static OpenFile swapFile;

	public static Lock memoryLock;
	//globale freelist 
	public static LinkedList freeList = new LinkedList();

	private static final char dbgVM = 'v';
}

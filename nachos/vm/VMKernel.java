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

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	/*
	 * Implements the clock algorithm for the page eviction strategy.
	 * LOTS OF TODOs...
	 */
	private frameMemoryBlock evictPage(){
		memoryLock.acquire();
		// Initialize Conditional variable here and call sleep() when
		// all the pages are pinned
		Int numPagesPinned = 0;
		Int numPhysPages = Machine.processor().getNumPhysPages();

		for (int i = 0; i < numPhysPages; ++i){
			if(physMap[i].pinned == true){
				numPagesPinned++;
			}
		}
		if(numPagesPinned == numPhysPages) { pinnedPages.sleep(); }

		syncTLBEntry(false);

		// while the page has not found, keeps running the clock alg. in loop
		int clockPin = 0;
		boolean pageFound = false;
		while(!pageFound){

		}

		// check for several conditions:

		// 1. evict the page that does not have a process. break out.

		// 2. page has been used, set .used flag to true

		// 3. evicts the current page

		memoryLock.release();
	}

	/**
	 * Sync TLB entries with page table entries since users can only
	 * manipulate the TLB entried.
	 *
	 * @ switched indicate if a context switch happened
	 */
	public void syncTLBEntry(boolean contextSwitch){
		TranslationEntry entry = null;
		for(int i = 0; i < Machine.processor().getTLBSize(); i++){
			entry = Machine.processor().readTLBEntry(i);
			if(entry.valid){
				TranslationEntry physEntry = physMap[entry.ppn].translationEntry;
				physEntry.dirty = entry.dirty;
				physEntry.used = entry.used;
				if(contextSwitch){
					entry.valid = false;
				}
			}
			// sync back to page table
			Machine.processor().writeTLBEntry(i, entry);
		}
	}


	/*
	 * Wrapper class to account for each block in the global data structure.
	 * Check for pinned status of pages and which process owns the page.
	 * Read write up design section: Global Memory Accounting
	 */
	private static class frameMemoryBlock{
		frameMemoryBlock(int ppn){
			translationEntry = new TranslationEntry(-1, ppn, false, false, false, false);
			pinned = false;
		}
		TranslationEntry translationEntry;
		VMProcess process;
		boolean pinned;
	}

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;
	//global swap file accross all the processes
	public static OpenFile swapFile;

	public static Lock memoryLock;
	//globale freelist 
	public static LinkedList freeList = new LinkedList();
	// condition variable to track if all pages are pinned
	public static Condition pinnedPages = new Condtion();

	// global array to track pinned pages and associated processes
	private frameMemoryBlock[] physMap = new frameMemoryBlock[Machine.processor().getNumPhysPages()];

	private static final char dbgVM = 'v';
}

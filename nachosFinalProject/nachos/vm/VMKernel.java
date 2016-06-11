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

	}
	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		File = ThreadedKernel.fileSystem.open("swapFile",true);
		//File.read()/File.write()
		//System.out.println("initialize break point 1 ");
		pageFaultLock = new Lock();
		allPinnedLock = new Lock();
		pinLock = new Lock();
		evictLock = new Lock();
		countLock = new Lock();
		swapLock = new Lock();
		swapOutLock = new Lock();
		swapInLock = new Lock();
		IPTLock = new Lock();
		allPinned = new Condition(allPinnedLock);

		//initialize the clock position to 0
		victim = 0;
		pinnedCount = 0;
		swapFileSize = 0;
		//FIXME have to increase the count correctly.
		//System.out.println("initialize break point 2 ");
		for(int i = 0; i < Machine.processor().getNumPhysPages(); i++){
		//System.out.println("initialize break point 3 ");
			IPTable[i] = new IPTEntry();
		}
		//System.out.println("initialize break point 4 ");
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
		File.close();
		ThreadedKernel.fileSystem.remove(File.getName());
	}


	/**swap the entry(of inverted page table) to the swapfile on the disk
	 * return the position inside the swap file
	 */
	public static int swapOut(int ppn){
		
		swapOutLock.acquire();
		int spn;
		//first, create a empty buffer with the size equals to the page size
		byte[] buffer = new byte[pageSize];
		int vpn = IPTable[ppn].vpn;
		int vaddr = Processor.makeAddress(vpn, 0);
		VMProcess process = IPTable[ppn].currProcess;
		
		int byteRead = process.readVirtualMemory(vaddr, buffer, 0, pageSize);
		//handle the error
		Lib.assertTrue(byteRead == pageSize);
		
		spn = getFreeSwapPages();
		//set the value of the current file pointer
		//File.seek(spn*pageSize);
		//write the data in the buffer to the swap file
		//FIXME
		int byteWritten = File.write(spn*pageSize, buffer, 0, pageSize);
		
		//record the spn in the process
		process.recordSPN(vpn, spn);
		//handle the error
		Lib.assertTrue(byteWritten == pageSize);
		swapOutLock.release();
		
		return spn;
	}

	/**read the data from the swap file on the disk*/
	public static void swapIn(int ppn, int vpn, VMProcess process){
		swapInLock.acquire();
		//First, create a empty buffer with the size equals to the page size
		byte[] buffer = new byte[pageSize];

		//then read the data from swap file
		int spn = process.readSwapPos(vpn);
		//File.seek( spn * pageSize);
		int bytesRead = File.read(spn*pageSize, buffer, 0, pageSize);

		Lib.assertTrue(bytesRead == pageSize);
		//write to virutal memory
		int vaddr = Processor.makeAddress(vpn, 0);
		int byteWritten = process.writeVirtualMemory(vaddr, buffer, 0, pageSize);

		Lib.assertTrue(byteWritten == pageSize);
		
		//free the space in freeswapPages
		freeSwapPage(spn);
		//TODO set the dirty bit and update the inverted table

		//update the Inverted page table. This time, link the ppn with the vpn
		//FIXME: NEED A LOCK HERE
		process.swapPos[vpn] = -1;
		process.dirtyBit(vpn,true);
		IPTLock.acquire();
		IPTable[ppn].vpn = vpn;
		IPTable[ppn].currProcess = process;
		IPTLock.release();
		swapInLock.release();

	}

	/**get a free position in the swap file(freeswapPages),
	 * allocate one if there is no space in swap file
	 * @return
	 */
	public static int getFreeSwapPages(){
		//FIXME: need a lock here?
		swapLock.acquire();
		int spn;
		//if the freeswapPages is not empty;
		if(freeswapPages.size() > 0){
			 spn = freeswapPages.removeFirst().intValue();
		}
		else{
			//need to allocate new space in swapfile
			spn = swapFileSize++;
		}
		swapLock.release();
		return spn;
	}

	public static void freeSwapPage(int spn){
		//need a lock here
		swapLock.acquire();
		freeswapPages.add(spn);
		swapLock.release();
	}




	/**find the next unpinned page. If all the pages are pinned, then sleep the conditional variable.
	 * otherwise, the method will ruturn a unpinned page index inside the inverted page table*/
	public static int sleepIfAllPinned(){
//		//FIXME: WHEN TO PIN THE PHYSICAL MEMORY
		allPinnedLock.acquire();
		int numPhysPages = Machine.processor().getNumPhysPages();

		int idx = (victim+1)% numPhysPages;

		//if pinnedCount equals to the number of phyiscal pages, then all the physical pages are pinned
		if(pinnedCount == numPhysPages){
			allPinned.sleep();
		}
		//there must be a unpinned page, loop through the IPTable and find it
		while(true){

			//if we find the unpinned page, we can break the loop
			if(!IPTable[idx].pin)
				break;
			idx = (idx+1)% numPhysPages;
		}
		allPinnedLock.release();
		return idx;
	}



	/**clock algorithm*/
	public static int selectReplacementEntry(VMProcess process){
		//already sync the TLB before the method call
		
		evictLock.acquire();
		//sync TLB entries before evicting
		
		while(true){
			
			victim = sleepIfAllPinned();
			VMProcess ownerProcess = IPTable[victim].currProcess;
			int vpn = IPTable[victim].vpn;

			Lib.assertTrue(ownerProcess != null);
			Lib.assertTrue(vpn >= 0);
			TranslationEntry entry = ownerProcess.getPTE(vpn);
			//if recently used, then continue
			if(entry.used){
				entry.used = false;
			}
			//else,need to check if the entry is dirty; If it is, swapped it out
			else{
				if(entry.dirty){
					//FIXME, NEED TO CHECK if swapped out with no error
					int spn = swapOut(victim);
					//this entry has been swapped out, so we have to set it to be invalid
					ownerProcess.recordSPN(vpn,spn);
					//the entry has been swapped out, need to set to invalid
					entry.valid  = false;
					break;
				}
				else{
					//is readonly, can be read from coff section
					entry.valid  = false;
					break;
				}
			}
			
			//updateTLB for current process
		}
		evictLock.release();
		return victim;
	}

	//pin the physical page, put a pinLock here;
	public static void pinPage(int ppn){
		//FIXME NEED A LOCK HERE
		pinLock.acquire();
		IPTable[ppn].pin = true;
		increasePinCount(true);
		pinLock.release();

	}

	//unpin the physical page, put a pinLock here;
	public static void unpinPage(int ppn){
		pinLock.acquire();

		IPTable[ppn].pin = false;
		
		increasePinCount(false);

		pinLock.release();
	}

	public static void increasePinCount(boolean increase){
		//FIXME: need a unique lock here
		countLock.acquire();
		if(increase){
			pinnedCount++;
		}
		else{
			pinnedCount--;
		}
		countLock.release();
	}



	/**The Entry of IPTable, each entry includes the info about process, vpn and pinCount*/
	public class IPTEntry{
		public VMProcess currProcess = null;
		public int vpn = -1;
		public boolean pin = false;
		//public TranslationEntry entry = null
		public IPTEntry(){
			vpn = -1;
			pin = false;
			currProcess = null;
		}

		public IPTEntry(VMProcess process, int vpn, boolean pinned){
			this.vpn = vpn;
			this.currProcess = process;
			this.pin = pinned;
		}
		
		public void reset(){
			currProcess = null;
			vpn = -1;
		}
	}

	public static Lock pageFaultLock;
	public static Lock pinLock;

	//inverted page table
	public static IPTEntry [] IPTable = new IPTEntry[Machine.processor().getNumPhysPages()];
	public static Lock IPTLock;
	public static OpenFile File;
	public static int victim;
	//a lock to keep track of the pinnedCount;
	public static Lock swapOutLock;
	public static Lock swapInLock;
	public static Lock countLock;
	public static Lock evictLock;
	public static int pinnedCount = 0;
	//allPinnedLock is associated with the conditional varibale allPinned;
	public static Lock allPinnedLock;
	public static Condition allPinned;
	//record which places are available in the swap file

	//swapFileSize and freeswapPages share a lock
	public static Lock swapLock;
	public static int swapFileSize = 0;
	public static LinkedList<Integer> freeswapPages = new LinkedList();

	private static final int pageSize = Processor.pageSize;


	private static final char dbgVM = 'v';
}

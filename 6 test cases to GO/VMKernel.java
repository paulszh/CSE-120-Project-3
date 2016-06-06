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
		IPTLock = new Lock();
		allPinned = new Condition(allPinnedLock);
		pinCountLock = new Lock();
		swapLock = new Lock();
		pageFaultLock = new Lock();
		
		//initialize the clockHand to 0
		clockHand = 0;
		pinnedCount = 0;
		swapFileSize = 0;
		//FIXME have to increase the count correctly.
		for(int i = 0; i < Machine.processor().getNumPhysPages(); i++){
			IPTable[i] = new IPTEntry();
		}
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
		//File.close();
		//ThreadedKernel.filesystem.remove();
	}
	
	
	
	/**swap the entry(of inverted page table) to the swapfile on the disk
	 * return the position inside the swap file
	 */
	public static int swapOut(int vpn, VMProcess process){
		//swap file page number
		int spn;
		
		//first, create a empty buffer with the size equals to the page size
		byte[] buffer = new byte[pageSize];
		int vaddr = Processor.makeAddress(vpn, 0);
		int byteRead = process.readVirtualMemory(vaddr, buffer, 0, pageSize);
		//handle the error
		if(byteRead != pageSize){
			Lib.debug(dbgVM, "readVirtualMemory failed");
		}
		//FIXME get a free swap Pages   
		spn = getFreeSwapPages();
		//set the value of the current file pointer
		File.seek(spn*pageSize);
		//write the data in the buffer to the swap file
		int byteWritten = File.write(buffer, 0, pageSize);
		
		//handle the error
		if(byteWritten != pageSize){
			Lib.debug(dbgVM, "write to swapfile failed");
		}
		
		return spn;
	}
	
	/**read the data from the swap file on the disk*/
	public static void swapIn(int ppn, int vpn, VMProcess process){
		
		//FIXME: NEED A LOCK HERE
		//First, create a empty buffer with the size equals to the page size
		byte[] buffer = new byte[pageSize];
		
		//then read the data from swap file
		int spn = process.readSwapPos(vpn);
		//FIXME 
		File.seek( spn * pageSize);
		int bytesRead = File.read(buffer, 0, pageSize);
		
		
		if(bytesRead != pageSize){
			Lib.debug(dbgVM, "Read from swapFile failed");
		}
		
		//write to virutal memory
		int vaddr = Processor.makeAddress(vpn, 0);
		int byteWritten = process.writeVirtualMemory(vaddr, buffer, 0, pageSize);
		
		if(byteWritten != pageSize){
			Lib.debug(dbgVM, "write to virtual memory fail");
		}
		
		//free the space in freeswapPages
		freeSwapPage(spn);
		//TODO set the dirty bit and update the inverted table
	
		//update the Inverted page table. This time, link the ppn with the vpn
		//FIXME: NEED A LOCK HERE
		IPTable[ppn].vpn = vpn;
		IPTable[ppn].currProcess = process;
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
			//allocate space from free swap pools
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
	public static int sleepIfAllPinned(int clockHandIdx){
		//FIXME: WHEN TO PIN THE PHYSICAL MEMORY
//		int numPhysPages = Machine.processor().getNumPhysPages();
//		
//		int idx = (clockHandIdx+1)% numPhysPages; 
//		
//		allPinnedLock.acquire();
//		//if pinnedCount equals to the number of phyiscal pages, then all the physical pages are pinned
//		if(pinnedCount == numPhysPages){
//			System.out.println("Slept");
//			allPinned.sleep();
//			allPinnedLock.release();
//		}
//		//there must be a unpinned page, loop through the IPTable and find it
//		while(true){
//			//if we find the unpinned page, we can break the loop
//			if(!IPTable[idx].pin)
//				break;
//			idx = (idx+1)% numPhysPages; 
//		}
//		return idx;
		clockHand=(clockHand+1)%Machine.processor().getNumPhysPages();
    	int start=clockHand;
    	allPinnedLock.acquire();
    	while (IPTable[clockHand].pin)
    	{
        	clockHand=(clockHand+1)%Machine.processor().getNumPhysPages();
        	if(clockHand==start){
        		System.out.println("slept");
        		allPinned.sleep();
        	}
    	}
    	allPinnedLock.release();
    	return clockHand;

		
		
		
	}


	public static void pinPage(int ppn){
		pinLock.acquire();
		IPTable[ppn].pin = true;	
		increasePinCount(true);
		pinLock.release();
		
	}
	public static void unpinPage(int ppn){
		pinLock.acquire();
		IPTable[ppn].pin = false;	
		increasePinCount(false);
		pinLock.release();
	}

	public static void increasePinCount(boolean increase){
		pinCountLock.acquire();
		if(increase){
			pinnedCount++;	
		}
		else{
			pinnedCount--;
		}
		pinCountLock.release();
	}
		
	
	/**clock algorithm*/
	public static int selectReplacementEntry(){
		//already sync the TLB before the method call
		IPTLock.acquire();
		while(true){
			clockHand = sleepIfAllPinned(clockHand);
			VMProcess ownerProcess = IPTable[clockHand].currProcess;
			int vpn = IPTable[clockHand].vpn;
			TranslationEntry entry = ownerProcess.getPTE(vpn);
			//if recently used, then continue
			if(entry.used){
				entry.used = false;
			}
			//else,need to check if the entry is dirty; If it is, swapped it out
			else{
				if(entry.dirty){
					//FIXME, NEED TO CHECK if swapped out with no error
					int spn = swapOut(vpn, ownerProcess);
					//this entry has been swapped out, so we have to set it to be invalid
					Lib.assertTrue(spn != -1);
					ownerProcess.recordSPN(vpn,spn);
					//the entry has been swapped out, need to set to invalid
					entry.valid  = false;
					break;	
				}
				//FIXME: Do we need to explicitly handle the case when readonly is true?	
			}
			
		}
		//need to update the TLBEntry of the current process;
		//return the ppn 
		IPTLock.release();
		return clockHand;
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
	}
	
	public static Lock pageFaultLock;
	
	
	//inverted page table
	public static IPTEntry [] IPTable = new IPTEntry[Machine.processor().getNumPhysPages()];
	public static Lock IPTLock;
	public static Lock pinLock;
	
	public static OpenFile File;
	public static int clockHand;

	
	//This lock is for the pin count
	public static Lock pinCountLock;
	public static int pinnedCount;
	

	public static int swapFileSize;

	//this lock is associated with the conditional variable
	public static Lock allPinnedLock;
	public static Condition allPinned;
	//record which places are available in the swap file
	//this lock is for the swapPagePool
	public static Lock swapLock;
	public static LinkedList<Integer> freeswapPages = new LinkedList();
	
	private static final int pageSize = Processor.pageSize;
	
	private static final char dbgVM = 'v';
}

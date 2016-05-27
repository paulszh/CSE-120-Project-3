package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		//TLB miss
		//save TLB to pageTable 
		//invalid each entry in TLB
		int TLBSize = Machine.processor().getTLBSize();
		TranslationEntry entry = null;
		//invalid all the entry in TLB
		for(int i = 0; i < TLBSize; i++){

			entry = Machine.processor().readTLBEntry(i);
			//sycn the dirty bit in page table

			updatePageTable(entry.vpn, entry);
			//pageTable[entry.vpn].dirty = entry.dirty;
			entry.valid = false;
		}
		//super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		//super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		//CREATE A PAGE TABLE ALL THE ENTRY IS FALSE
	//VMkernel.memoryLock.acquire();

	if (UserKernel.freePages.size() < numPages) {
	    UserKernel.memoryLock.release();
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	pageTable = new TranslationEntry[numPages];

	for (int vpn=0; vpn<numPages; vpn++) {
	    pageTable[vpn] = new TranslationEntry(vpn, -1,
						  false, false, false, false);
	}
	
	//VMkernel.memoryLock.release();
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
	}

	private void handleTLBMisss(){

		//VMkernel.memoryLock.acquire();
		int vaddress = Machine.processor().readRegister(Processor.regBadVAddr);
		int vpn = Machine.processor().pageFromAddress(vaddress);
		//need to check if vpn is out of bound
		if(vpn > pageTable.length || vpn < 0){
			Lib.debug(dbgProcess, "illegal memory access");
		}
		//get the page table entry from VPN
		TranslationEntry ptEntry = pageTable[vpn];
	
		//need to check if ptEntry is valid,assume it is valid here

		//Get the size of TLB
		int TLBSize = Machine.processor().getTLBSize();
		//Allocate a new TLB entry
		TranslationEntry entry = null;

		int index = -1;

		for(int i = 0; i < TLBSize; i++){

			entry = Machine.processor().readTLBEntry(i);

			if(!entry.valid){
				//the entry is invalid, can be replaced directly
				entry.valid = true;
				index = i;
				break;
			}

		}
		//if all the entry is valid, then ramdomly replace on entry;
		if(index == -1){
			index = Lib.random(TLBSize);
			entry = Machine.processor().readTLBEntry(index);
		}

		if(ptEntry.valid){
			updatePageTable(ptEntry.vpn, entry);
			Machine.processor().writeTLBEntry(index,ptEntry);
		}
		else{
			VMkernel.pageFaultHandler(ptEntry.ppn);

		}
		//need to handle the case: ptEntry is invalid;

		//VMkernel.memoryLock.release();

	}

	private void updatePageTable(int vpn, TranslationEntry entry){
		TranslationEntry toUpdate = pageTable[vpn];
		toUpdate.dirty = entry.dirty;
		toUpdate.readOnly = entry.readOnly; 
		toUpdate.used = entry.used;
		toUpdate.valid = entry.valid;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionTLBMiss:
			handleTLBMisss();//handle the TLB miss
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	private static final int pageSize = Processor.pageSize;

    //protected TranslationEntry[] pageTable;

	//protected Lock memoryLock;
	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}

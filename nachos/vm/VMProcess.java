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

		VMKernel.syncTLBEntry(true);
		// Need to:
		// 1. Invalidate all entries in the page table
		// TODO 2. Completely reload the TLB 

	//	TranslationEntry entry = null;

		//invalid all the entry in TLB
	//	for(int i = 0; i < Machine.processor().getTLBSize(); i++){
	//		entry = Machine.processor().readTLBEntry(i);


		/*	TODO update the TranslationEntry in the physical page allocation.

			p.s. need to know the 'coreMap' --> which basically maintains
			information about physical page alocation (for replacement)

			if (entry.valid){
				TranslationEntry pageEntry = pageTable[entry.ppn].TranslationEntry;

			}
		*/
			// Simple implementation for now.
			// Set all TLB entries to be invalid
	//		entry.valid = false;
	//	}
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
		VMkernel.memoryLock.acquire();
		int vaddress = Machine.processor().readRegister(Processor.regBadVAddr);
		int vpn = Machine.processor().pageFromAddress(vaddress);
		//need to check if vpn is out of bound
		if(vpn > pageTable.length || vpn < 0){
			Lib.debug(dbgProcess, "illegal memory access");
		}
		// TODO get the page table entry from VPN
		// If the ppn of this entry equals the other entry,
		// would need to invalidate it.
		TranslationEntry ptEntry = pageTable[vpn];

		//Get the size of TLB
		int TLBSize = Machine.processor().getTLBSize();
		//Allocate a new TLB entry
		TranslationEntry entry = null;

		int index = -1;
		Process faultProcess = Machine.processor();
		for(int i = 0; i < TLBSize; i++){
			entry = faultProcess.readTLBEntry(i);

			// For now, simple implentation when valid bit is found
			// ... need to set the dirty bits and used bits later
			if(!entry.valid){
				entry.valid = true;
				index = i;
				faultProcess.writeTLBEntry(index, ptEntry);
				break;
			}

		}
		//if all the entry is valid, then ramdomly replace on entry;
		if(index == -1){
			index = Lib.random(TLBSize);
			entry = Machine.processor().readTLBEntry(index);

			// TODO
			// propage the info down onto memory (page swap)
			// ...

			index.writeTLBEntry(index, ptEntry);
		}
		// TODO
		// unpin the physical page

		VMkernel.memoryLock.release();
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

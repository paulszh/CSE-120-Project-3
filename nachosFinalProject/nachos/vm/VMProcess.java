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
		syncTLBEntry();
	}
	
	
	public void syncTLBEntry(){
		for(int i = 0; i < Machine.processor().getTLBSize(); i++){
			
			TranslationEntry entry = Machine.processor().readTLBEntry(i);
			
			if(entry.valid){
				// Sync TLB Entries with Page Table Entries
				pageTable[entry.vpn].dirty = entry.dirty;
				pageTable[entry.vpn].used = entry.used;
			}
			
			
			entry.valid = false;
			Machine.processor().writeTLBEntry(i, entry);	
		}
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		//super.restoreState();
	}
	
	/**
	 * 
	 * @return
	 */
	public int readVirtualMemory(){
		return 0;
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * This method should record the mapping of VPN .. section and offset
	 * Only called once per process
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
	//VMkernel.memoryLock.acquire();
	// 
	if (UserKernel.freePages.size() < numPages) {
	    UserKernel.memoryLock.release();
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	pageTable = new TranslationEntry[numPages];
	swapPos = new int[numPages];
	// lazy loading after a TLB miss occurs
	for (int i=0; i<numPages; i++) {
	    pageTable[i] = new TranslationEntry(i, -1,
						  false, false, false, false);
	    //initialize all the entry inside swapPos to -1 which indicates no entry has been written to the swap file 
	    swapPos[i] = -1;
	}
	
	// *** Same as the UserProcess.java??
	// Lazy loading means always returning true
	// Loads the Coff sections into memory
	for (int j = 0; j < coff.getNumSections(); ++j){
		CoffSection section = coff.getSection(j);
		for(int k = 0; k < section.getLength(); ++k){
			int vpn = section.getFirstVPN() + k;
			pageTable[vpn].readOnly = section.isReadOnly();
			section.loadPage(k, pageTable[vpn].ppn);
		}
	}
	
	// maps out stack pages???? Not understanding
	/* make sure you understands stackPages well enough before uncomment this
	for(int s = numPages - (stackPages + 1); s < numPages; ++s){
		pageTable[s].valid = false;
		pageTable[s].readOnly = false;
		pageTable[s].dirty = false;
		pageTable[s].vpn = s;
	} */
	
	//VMkernel.memoryLock.release();
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		// acquire a lock
		for(int i = 0; i < pageTable.length; ++i){
			if(pageTable[i].readOnly == false && pageTable[i].valid == false){
				// TODO 
				// free the swap file space here... use pageTable[i].vpn
			}
		}
		
		super.unloadSections();
		// *** close the Coff here??
	//	coff.close();
		// release the lock
	}
	
	
	private void updateTLB(){
		TranslationEntry entry;
		for (int i=0;i<Machine.processor().getTLBSize();i++)
    	{
    		entry = Machine.processor().readTLBEntry(i);
    		if(entry.valid)
    		{
    			Machine.processor().writeTLBEntry(i, pageTable[entry.vpn]);
    		}
    	}
	}
	
	
	
	/**the current index(vpn) entry is invalid, so we need to allocate a new ppn to it if there is still physical memory available
	*or we need to replace one entry in the inverted page table(indexed by ppn). The method will return a ppn. 
	*/
	private int pageFaultHandler(int vpn){
		//must ensure that PTE is invalid
		Lib.assertTrue(!pageTable[vpn].valid);
	
		int ppn;
		//FIXME need a lock here
		//VMKernel.pageFaultLock.acquire();
		
		//FIXME: Assume the page we need is in the swap file, need to fix it later(part 3)
		//Case: freepageList is not empty
		if(!VMKernel.freePages.isEmpty()){
			//allocate one from list
			ppn = ((Integer)VMKernel.freePages.removeFirst()).intValue();
			
		}
		else{
			//sync TLB entries
			// ***** TO CHECK
			// Is this the right way to sync TLB entries??
			updatePageTable();
			//select a victim for replacement
			ppn = VMKernel.selectReplacementEntry();	
			//need to update TLB immediately. since some entry in pageTable might have changed
			updateTLB();
		}
		
		//map the old invalid entry to the new ppn
		pageTable[vpn].ppn = ppn;
		pageTable[vpn].valid = true;
		//then swap in from the disk , need to map ppn with vpn in the inverted page table
		VMKernel.swapIn(ppn, vpn, this);
		return ppn;
		
	}
	
	public void recordSPN(int vpn, int spn){
		swapPos[vpn] = spn;
	}
	
	public int readSwapPos(int vpn){
		return swapPos[vpn];
	}
	
	
	
	/**DOES this method needs to return something?*/
	private void handleTLBMiss(int vaddress){
		VMKernel.memoryLock.acquire();
		
		int vpn = Processor.pageFromAddress(vaddress);
		
		//need to check if vpn is out of bound
		if(vpn > pageTable.length || vpn < 0){
			Lib.debug(dbgProcess, "illegal memory access");
		}
		
		//get the page table entry from VPN
		TranslationEntry ptEntry = pageTable[vpn];

		//Get the size of TLB
		int TLBSize = Machine.processor().getTLBSize();
		
		//Allocate a new TLB entry
		TranslationEntry entry = null;

		int index = -1;

		for(int i = 0; i < TLBSize; i++){

			entry = Machine.processor().readTLBEntry(i);

			if(!entry.valid){
				//the entry is invalid, can be replaced directly
				//FIXME: does it need to be set to valid?
				//FIXME: if the entry is invalid, do we need to sync it with the page table? 
				index = i;
				break;
			}

		}
		
		//if all the entry is valid, then randomly replace the entry;
		if(index == -1){
			// ALSO need to sync victim's page table entry
			index = Lib.random(TLBSize);
			entry = Machine.processor().readTLBEntry(index);
		}
		
		// need to sync the TLB entry with the PTE
		if(ptEntry.valid){
			//update the index entry.vpn in pageTable according to the used and dirty bits of the entry
			updatePageTableEntry(entry.vpn, entry);
			
			Machine.processor().writeTLBEntry(index,ptEntry);
		}
		
		//need to handle the case: ptEntry is invalid;
		else{
			//need to handle it in the VMKernel
			//the pageFaultHandler need to read the data from the swap file and then update 
			//the TLB
			int ppn = pageFaultHandler(ptEntry.vpn);
			if(ppn == -1){
				Lib.debug(dbgVM, "error occurs");
			}
		
		}
		VMKernel.memoryLock.release();
	}


	private void updatePageTableEntry(int vpn, TranslationEntry entry) {
		TranslationEntry toUpdate = pageTable[vpn];
		toUpdate.dirty = entry.dirty;
		toUpdate.used = entry.used;
		
	}
	
	
	private void updatePageTable(){
		TranslationEntry toUpdate;
		for(int i = 0; i < Machine.processor().getTLBSize(); i++){
			toUpdate = Machine.processor().readTLBEntry(i);
			if(toUpdate.valid){
				updatePageTableEntry(toUpdate.vpn,toUpdate);
			}
		}
	}
	
	//get the PTE by VPN
	public TranslationEntry getPTE(int vpn){
		return pageTable[vpn];
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
			int vaddress = Machine.processor().readRegister(Processor.regBadVAddr);
			handleTLBMiss(vaddress);//handle the TLB miss
			break;
		default:
			super.handleException(cause);
			break;
		}
	}

	private static final int pageSize = Processor.pageSize;

	//protected Lock memoryLock;
	public static int swapPos[];
	
	private int numCoffPages = 0;
	
	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}

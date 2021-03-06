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
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		System.out.println(numPages);
		//CREATE A PAGE TABLE ALL THE ENTRY IS FALSE
		//VMkernel.memoryLock.acquire();

		//UserKernel.memoryLock.release();

		pageTable = new TranslationEntry[numPages];
		swapPos = new int[numPages];

		for (int i=0; i<numPages; i++) {
			pageTable[i] = new TranslationEntry(i, -1,
					false, false, false, false);
			//initialize all the entry inside swapPos to -1 which indicates no entry has been written to the swap file 
			swapPos[i] = -1;
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

		//System.out.println("page Fault Handler");
		//must ensure that PTE is invalid
		Lib.assertTrue(!pageTable[vpn].valid);

		int ppn;
		//FIXME need a lock here
		//Case: freepageList is not empty
		if(!VMKernel.freePages.isEmpty()){
			//allocate one from list
			ppn = ((Integer)VMKernel.freePages.removeFirst()).intValue();

		}
		else{
			System.out.println("Updating the page table when freepages is not empty");
			//sync TLB entries before evicting
			updatePageTable();
			//select a victim for replacement and swap out a page
			//FIXME NEED A LOCK HERE 
			ppn = VMKernel.selectReplacementEntry();
			System.out.println("[pageFaultHandler] successfully selected replacement");	
			//need to update TLB immediately. since some entry in pageTable might have changed
			updateTLB();
			System.out.println("[pageFaultHandler] successfully update TLB");	
		}
		System.out.println("After freePages has been checked");

		//map the old invalid entry to the new ppn
		pageTable[vpn].ppn = ppn;
		//set the current entry to valid
		pageTable[vpn].valid = true;
		//mark as used
		pageTable[vpn].used = true;
		//set the dirty bit to true
		pageTable[vpn].dirty = true;

		//pick the source to read the data in
		if( swapPos[vpn] != -1){	
			System.out.println("read the data from the swap file");
			//then swap in from the disk , need to map ppn with vpn in the inverted page table
			VMKernel.swapIn(ppn, vpn, this);
		}
		//The data we need is in the coff file, load on demand(load one page everytime)
		else{
				System.out.println("loading From Coff file");
			loadFromCoffFile(vpn,ppn);
		}


		System.out.println("before returning PPN");
		return ppn;


	}

	//only one page at a time
	//FIXME need to modify
	private void loadFromCoffFile(int vpn,int ppn){
		//	System.out.println("read from the coff area");
		int coffLength= numPages- stackPages - 1;

		if(vpn<coffLength)
		{
			for (int s=0; s<coff.getNumSections(); s++) {
				CoffSection section = coff.getSection(s);

				if (vpn-section.getFirstVPN()<section.getLength())
				{

					pageTable[vpn].readOnly=section.isReadOnly();
					if(pageTable[vpn].readOnly)
						Lib.debug(dbgVM,"\tReadOnly");
					section.loadPage(vpn-section.getFirstVPN(), ppn);
					break;
				}
			}
		}
		//update the inverted table
		//need multiple lock here
		VMKernel.IPTable[ppn].currProcess = this;
		VMKernel.IPTable[ppn].vpn = vpn;

	}


	public void recordSPN(int vpn, int spn){
		swapPos[vpn] = spn;
	}

	public int readSwapPos(int vpn){
		return swapPos[vpn];
	}



	/**DOES this method needs to return something?*/
	private void handleTLBMisss(int vaddress){

		//	System.out.println("TLB miss");
		int vpn = Processor.pageFromAddress(vaddress);
		//check if vpn is out of bound
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
			index = Lib.random(TLBSize);
			entry = Machine.processor().readTLBEntry(index);
		}

		//need to sync the TLB entry with the PTE
		if(ptEntry.valid){

			//update the index entry.vpn in pageTable according to the used and dirty bits of the entry
			updatePageTableEntry(entry.vpn, entry);

			Machine.processor().writeTLBEntry(index,ptEntry);
		}
		//need to handle the case: ptEntry is invalid;
		else{
			//need to handle it in the VMKernel
			//the pageFaultHandler need to read the data from the swap file and also need to update the 
			//ptEntry.vpn and set the entry to valid
			int ppn = pageFaultHandler(ptEntry.vpn);

			if(ppn == -1){
				Lib.debug(dbgVM, "error occurs");
			}
			//the entry at the index vpn in page table has been updated. We can then write it to the TLB
			Machine.processor().writeTLBEntry(index, pageTable[vpn]);
		}

	}


	private void updatePageTableEntry(int vpn, TranslationEntry entry) {

		TranslationEntry toUpdate = pageTable[vpn];
		//	System.out.println("[In updatePageTable with params] vpn at table " + vpn);
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
		//	System.out.println("updatePageTable() returns");
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
				System.out.println(vaddress);
				handleTLBMisss(vaddress);//handle the TLB miss
			//	System.out.println("Successfully handle TLB misses");
				break;
			default:
				System.out.println("something is wrong");
				System.out.println("the cause of the problem" + cause);
				super.handleException(cause);
				break;
		}
	}

	private static final int pageSize = Processor.pageSize;

	//protected Lock memoryLock;
	public static int swapPos[];

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}

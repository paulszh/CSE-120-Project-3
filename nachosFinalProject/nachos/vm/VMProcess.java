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
		//System.out.println(numPages);
		//CREATE A PAGE TABLE ALL THE ENTRY IS FALSE

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
		//super.unloadSections();
		
	
		
		for(int i = 0;i < numPages;i++){
			
			if(swapPos[i] != -1){
				VMKernel.freeSwapPage(swapPos[i]);
			}
			
			if(pageTable[i].valid){
				VMKernel.IPTable[pageTable[i].ppn].reset();
				
				if(VMKernel.IPTable[pageTable[i].ppn].pin){
					VMKernel.unpinPage(pageTable[i].ppn);
				}
				
				VMKernel.memoryLock.acquire();
				
				VMKernel.freePages.add(new Integer(pageTable[i].ppn));
				
				VMKernel.memoryLock.acquire();
				
				pageTable[i].valid = false;
			}
		}
		syncTLBEntry();

	}


	public void updateTLB(){

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

	@Override
	protected int pinVirtualPage(int vpn, boolean isUserWrite) {
		//System.out.println("pinVirtualPage is called");
		int ppn;
		//need to handlePageFault
		if(vpn >= numPages){
			System.out.println("number virutal pages" + numPages);
			System.out.println("vpn is " + vpn);
		}
		if(!pageTable[vpn].valid){
			ppn = pageFaultHandler(vpn);
			Lib.assertTrue(ppn != -1);
		}
		else{
			if (vpn < 0 || vpn >= pageTable.length){
				return -1;
			}

			TranslationEntry entry = pageTable[vpn];

			if (!entry.valid || entry.vpn != vpn){
				return -1;
			}

			if (isUserWrite) {
				if (entry.readOnly){
					return -1;
				}
				entry.dirty = true;

			}
			entry.used = true;
			ppn = entry.ppn;
		}
		Lib.assertTrue(ppn < VMKernel.IPTable.length);
		//next need to actually pin the page pysical page
		VMKernel.pinPage(ppn);
		return ppn;
	}

	@Override
	protected void unpinVirtualPage(int ppn){
		//System.out.println("unpinVirtualPage is called");
		VMKernel.unpinPage(ppn);
		VMKernel.allPinnedLock.acquire();
        	VMKernel.allPinned.wakeAll();
		VMKernel.allPinnedLock.release();
	}

	/**the current index(vpn) entry is invalid, so we need to allocate a new ppn to it if there is still physical memory available
	*or we need to replace one entry in the inverted page table(indexed by ppn). The method will return a ppn.
	*/
	private int pageFaultHandler(int vpn){

		//must ensure that PTE is invalid
		VMKernel.pageFaultLock.acquire();
		Lib.assertTrue(!pageTable[vpn].valid);

		int ppn;
		//FIXME need a lock here
		//Case: freepageList is not empty
		if(!VMKernel.freePages.isEmpty()){
			//allocate one from list
			UserKernel.memoryLock.acquire();
			ppn = ((Integer)VMKernel.freePages.removeFirst()).intValue();
			UserKernel.memoryLock.release();

		}
		else{
			updatePageTable();
			//select a victim for replacement and swap out a page
			//FIXME NEED A LOCK HERE
			ppn = VMKernel.selectReplacementEntry(this);
			
			Lib.assertTrue(ppn != -1);
			//need to update TLB immediately. since some entry in pageTable might have changed
			updateTLB();
		}
		
		if( swapPos[vpn] == -1){
			//System.out.println("loading From Coff file");
			loadFromCoffFile(vpn,ppn);
			
		}
		else{
			//System.out.println("read the data from the swap file");
			//then swap in from the disk , need to map ppn with vpn in the inverted page table
			pageTable[vpn].ppn = ppn;
			
			//set the dirty bit to true
			pageTable[vpn].valid = true;
			VMKernel.swapIn(ppn, vpn, this);
			pageTable[vpn].dirty = true;
		}

		//map the old invalid entry to the new ppn
	
		//set the current entry to valid
		
		//mark as used
	
		//pick the source to read the data in
		
		//The data we need is in the coff file, load on demand(load one page everytime)
		pageTable[vpn].valid = true;
		pageTable[vpn].used = true;

		VMKernel.pageFaultLock.release();
		//System.out.println("before returning PPN");
		return ppn;


	}

	//only one page at a time
	private void loadFromCoffFile(int vpn,int ppn){
		//	System.out.println("read from the coff area");
		int length= numPages- stackPages - 1;
		//VMKernel.pinPage(ppn);
		if(vpn<length)
		{
			for (int s=0; s<coff.getNumSections(); s++) {
				CoffSection section = coff.getSection(s);

				int section_Num = vpn-section.getFirstVPN();
				if (section_Num < section.getLength())
				{

					pageTable[vpn].readOnly=section.isReadOnly();
					section.loadPage(section_Num, ppn);
					break;
				}
			}
		}
		else{
			byte[] buff = new byte[pageSize];
			byte[] memory = Machine.processor().getMemory();
			System.arraycopy(buff, 0, memory, ppn * pageSize, pageSize);

		}
		//update the inverted table
		//need multiple lock here
		pageTable[vpn].ppn = ppn;
		VMKernel.IPTLock.acquire();
		VMKernel.IPTable[ppn].currProcess = this;
		VMKernel.IPTable[ppn].vpn = vpn;
		VMKernel.IPTLock.release();
		//VMKernel.unpinPage(ppn);

	}


	public void recordSPN(int vpn, int spn){
		swapPos[vpn] = spn;
	}

	public int readSwapPos(int vpn){
		return swapPos[vpn];
	}



	/**DOES this method needs to return something?*/
	private void handleTLBMisss(int vaddress){

		int vpn = Processor.pageFromAddress(vaddress);
		//check if vpn is out of bound
		if(vpn > pageTable.length || vpn < 0){
			Lib.debug(dbgProcess, "illegal memory access");
		}
		//get the page table entry from VPN
		TranslationEntry ptEntry = pageTable[vpn];

		//Page Fault
		if(!ptEntry.valid){
			int ppn = pageFaultHandler(ptEntry.vpn);
			Lib.assertTrue(ppn != -1);
		}
		
		//Get the size of TLB
		int TLBSize = Machine.processor().getTLBSize();
		TranslationEntry entry = null;
		int index = -1;

		for(int i = 0; i < TLBSize; i++){

			entry = Machine.processor().readTLBEntry(i);
			if(!entry.valid){
				//the entry is invalid, can be replaced directly
				index = i;
				break;
			}

		}
		//if all the entry is valid, then randomly replace the entry;
		if(index == -1){
			index = Lib.random(TLBSize);
			entry = Machine.processor().readTLBEntry(index);
			Lib.assertTrue(entry.valid);
			//need to sync the TLB entry with the PTE
            updatePageTable();
			//updatePageTableEntry(entry.vpn, entry);
		}
		
		Lib.assertTrue(index!=-1);
		Machine.processor().writeTLBEntry(index,ptEntry);

	}


	private void updatePageTableEntry(int vpn, TranslationEntry entry) {

		TranslationEntry toUpdate = pageTable[vpn];
		//	System.out.println("[In updatePageTable with params] vpn at table " + vpn);
		toUpdate.dirty = entry.dirty;
		toUpdate.used = entry.used;
		//toUpdate.readOnly = entry.readOnly;

	}


	public void updatePageTable(){
		TranslationEntry toUpdate;
		for(int i = 0; i < Machine.processor().getTLBSize(); i++){
			toUpdate = Machine.processor().readTLBEntry(i);
			if(toUpdate.valid){
				updatePageTableEntry(toUpdate.vpn,toUpdate);
			}
		}
	}

    public void checkTLB(){
			
			TranslationEntry entry;
			TranslationEntry toCompare;
			VMProcess process;
			for (int i=0;i<Machine.processor().getTLBSize();i++)
			{
				entry = Machine.processor().readTLBEntry(i);
				
				if(entry.valid)
				{	
					int vpn = entry.vpn;
					int ppn = entry.ppn;
					Lib.assertTrue(vpn == VMKernel.IPTable[ppn].vpn);

					process = VMKernel.IPTable[ppn].currProcess;
					
					toCompare = process.pageTable[vpn];
					Lib.assertTrue(toCompare.ppn == ppn);
					if(entry.used != toCompare.used){
						System.out.println("entry vpn ppn" + vpn + " " + ppn);
						System.out.println("toCompare vpn ppn" + toCompare.vpn + " " + toCompare.ppn);
						
					}
					Lib.assertTrue(entry.used == toCompare.used);
					Lib.assertTrue(entry.readOnly == toCompare.readOnly);
					Lib.assertTrue(entry.readOnly == toCompare.readOnly);
						
				}
			}

	}

	//get the PTE by VPN
	public TranslationEntry getPTE(int vpn){
		return pageTable[vpn];
	}

	public void dirtyBit(int vpn, boolean setbit){
		pageTable[vpn].dirty = setbit;
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
			handleTLBMisss(vaddress);//handle the TLB miss
			break;
			default:
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

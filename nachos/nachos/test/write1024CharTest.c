#include "syscall.h"
int main() {
   int rdFd = open("1024Char.txt");
   int wrFd = creat("1024Char.copy");
   int readBytes, writtenBytes;
   int totalReadBytes = 0, totalWrittenBytes = 0;
   char buffer[1024];
   do {
      readBytes = read(rdFd, buffer, 1024);

      if (readBytes == -1) {
         printf("Fatal Error in read; Not expected;\n");
         break;
      }

      totalReadBytes += readBytes;
      printf("Bytes read: %d; Total bytes read: %d;\n", 
             readBytes, totalReadBytes);

      writtenBytes = write(wrFd, buffer, readBytes);
      if (writtenBytes == -1) {
         printf("Fatal Error in write; Not expected;\n");
         break;
      }
      totalWrittenBytes += writtenBytes;
      printf("Bytes written: %d; Total bytes written %d;\n",
             writtenBytes, totalWrittenBytes);
            
   } while (readBytes == 1024);
   
   printf("Total bytes read: %d;\n", totalReadBytes);
   printf("Total bytes written: %d;\n", totalWrittenBytes);

   halt();
   return 0;
}

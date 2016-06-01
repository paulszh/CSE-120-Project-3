#include "syscall.h"
int main() {
   int fd = open("USConst.txt");
   int readBytes;
   int totalReadBytes = 0;
   char buffer[512];
   do {
      readBytes = read(fd, buffer, 512);

      if (readBytes == -1) {
         printf("Fatal Error; Not expected;\n");
         break;
      }

      totalReadBytes += readBytes;
      printf("Bytes read: %d; Total bytes read: %d;\n", 
             readBytes, totalReadBytes);

      //write(1, buffer, readBytes);
   } while (readBytes == 512);
   
   printf("Total bytes read: %d;\n", totalReadBytes);

   halt();
   return 0;
}

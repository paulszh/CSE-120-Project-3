#include "syscall.h"
int main() {
   int fd = 0;
   int readBytes;
   int totalReadBytes = 0;
   char * buffer[1024];
   do {
      readBytes = read(fd, buffer, 1024);

      if (readBytes == -1) {
         printf("Fatal Error; Not expected;\n");
         break;
      }

      totalReadBytes += readBytes;
      printf("Bytes read: %d; Total bytes read: %d;\n", 
             readBytes, totalReadBytes);

      //printf("%s\n", buffer);
   } while (readBytes == 1024);
   
   printf("Total bytes read: %d;\n", totalReadBytes);

   halt();
   return 0;
}

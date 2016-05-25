#include "syscall.h"
#include "stdio.h"
#include "strcat.c"

int main () {
   int i;
   char fileName[16] = "nonExistent";
   for (i = 2; i <= 20; i++) {
      write(1, fileName, 14);
      fileName[11] = (char)(63 + i);
      fileName[12] = 0;

      int fdNE = open(fileName);
      printf("fdNE: %d; Supposed to be -1\n", fdNE);
      fdNE = creat(fileName);
      if (i < 16) {
         printf("fdNE: %d; Supposed to be %d\n", fdNE, i);
      } else {
         printf("fdNE: %d; Upper Limit Hit, thus supposed to be -1\n", fdNE);
      }
   }

   halt();
   return 0;
}

